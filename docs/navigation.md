# Архитектура навигации NavPlayground

Навигация одинакова в `main` (manual DI), `di/hilt`, `di/koin`, `di/metro` и `di/dagger`.
DI определяет способ создания объектов. Контракты, очередь, middleware и операции со стеками общие.

## 1. От действия пользователя до экрана

```mermaid
flowchart TB
    Screen["Экран фичи<br/>действие пользователя"]
    VM["ViewModel фичи"]
    API["feature:api<br/>типизированный NavKey и аргументы"]
    Contract["core:navigation<br/>Navigator + NavigationCommand"]
    Queue["app: NavigatorImpl.execute<br/>Channel с Request"]
    Worker["Один worker<br/>Dispatchers.Main.immediate"]
    Pipeline["core: NavigationPipeline<br/>цепочка middleware"]
    Dispatch["app: NavigatorImpl.dispatch<br/>команды экранов, выбор раздела, logout"]
    Stack["NavigatorImpl<br/>выбранный раздел и два SnapshotStateList"]
    Entries["NavEntryContributor от фич<br/>register в entryProvider"]
    Display["app: AppNavigation / NavDisplay<br/>NavKey → NavEntry → Screen"]
    Startup["MainActivity.onCreate<br/>initialize"]
    Screen --> VM
    API -->|целевой ключ| VM
    VM -->|execute с командой| Contract
    Screen -->|выбор раздела в AppNavigation| Contract
    Contract -->|реализация| Queue
    Queue -->|последовательно| Worker
    Worker --> Pipeline
    Pipeline -->|если команда пропущена| Dispatch
    Dispatch -->|изменяет| Stack
    Stack -->|наблюдаемое состояние| Display
    Entries -->|соответствие ключа и экрана| Display
    Display -->|создаёт UI записи| Screen
    Startup -.->|инициализация вне очереди| Stack
```

Стрелки показывают поток выполнения и данных, а не Gradle-зависимости.
`feature:api` хранит NavKey. В `feature:impl` находятся экраны, навигационные вклады и ViewModel, если она нужна фиче.
Фичи используют интерфейс Navigator из core и API ключей других фич, без зависимости от чужих impl.
`app` объединяет фичи, владеет реализацией навигатора и отображает стек через Navigation 3.

`execute()` сразу ставит Request в неограниченный Channel и возвращается. Worker обрабатывает
команды по одной: экран не вызывает NavDisplay или изменение списка напрямую.
Pipeline находится в core, но terminal-функция `dispatch` передаётся из NavigatorImpl в app.
Обработанные команды меняют стеки через dispatch. `initialize` загружает состояние до первого показа UI.

NavEntryContributor регистрирует сопоставление типа ключа с экраном.
Неизвестный ключ даёт UnknownDestinationScreen через fallback, а не ошибку поиска entry.
Способ собрать contributors различается между DI-вариантами: [атлас DI](di/README.md).
В manual DI регистрируются сами вклады, в Koin подключается модуль DSL, а в Dagger 2 модуль
включается в `@Component`. Это отдельный шаг после подключения Gradle-модуля.

## 2. Middleware: порядок, остановка и подмена команды

```mermaid
flowchart TB
    Command["NavigationCommand"] --> Logging["Logging · -100<br/>журнал команд"]
    Logging --> Throttle["Throttling · 0<br/>защита от повторной команды"]
    Throttle --> Validation["DeepLinkValidation · 10<br/>внешняя схема + handler + exported"]
    Validation --> Resolution["DeepLinkResolution · 15<br/>DeepLink → Forward с NavKey"]
    Resolution --> Flags["FeatureFlags · 20<br/>выключенная фича → StubNavKey"]
    Flags --> Auth["Auth · 30<br/>RequiresAuth без сессии → AuthNavKey"]
    Auth --> Analytics["Analytics · 40<br/>событие по итоговой цели"]
    Analytics --> Dispatch["dispatch<br/>изменение бэкстека"]
    Throttle -.->|повтор: без proceed| Drop["Команда остановлена<br/>стек не меняется этой командой"]
    Validation -.->|недопустимая ссылка| Drop
    Resolution -.->|ключ не найден| Drop
```

NavigationPipeline сортирует middleware по `order`, затем строит вложенную цепочку через foldRight.
На схеме показан путь команды внутрь цепочки; код после `proceed()` выполняется в обратном порядке.
Порядок регистрации или обхода Set не используется. У элементов с одинаковым order нет гарантированного
порядка между собой.

Middleware может передать исходную команду, передать изменённую либо не вызвать `proceed()`.
FeatureFlagMiddleware подменяет цель на StubNavKey, AuthMiddleware — на AuthNavKey с returnTo.
Forward/Replace/NewRoot сохраняют тип команды. При подмене цели SelectSection получается Forward:
вход или заглушка остаются в текущем разделе, пока переключение недоступно. Аналитика видит итоговую цель.
Обычная ошибка обработки логируется на уровне Request; CancellationException распространяется дальше.

| Команда после middleware | Что делает dispatch |
|---|---|
| Forward | Добавляет ключ в активную историю. Для ключа корня выбирает раздел |
| Replace | Заменяет верхнюю запись поверх корня. Для ключа корня выбирает раздел |
| NewRoot | Сбрасывает целевую историю до указанного корня |
| SelectSection | Переключает историю. Повторный выбор удаляет записи над корнем |
| Logout | Завершает сессию, очищает историю Requests и возвращает к Feed |
| Back | Убирает верхнюю запись. От корня Requests ведёт к Home, от корня Home — в систему |
| BackTo | Находит ближайший с конца равный ключ. Для Feed операция применяется к истории Home |
| DeepLink | Только логирует неразрешённую ссылку: штатно Resolution уже должен превратить её в Forward |

BackTo с inclusive удаляет и целевой ключ. Команда игнорируется, если ключ отсутствует
или операция оставила бы стек пустым.
При удалении записей `popTo` завершает связанные ожидания результатов ошибкой Cancelled.

## 3. Пример: внешний deep link и авторизация

```mermaid
flowchart TB
    Intent["Intent: navplay://profile/42"] --> Activity["MainActivity.handleDeepLink"]
    Activity --> Enqueue["execute DeepLink с source External<br/>очередь и начало pipeline"]
    Enqueue --> Validate{"Validation<br/>scheme navplay, handler найден,<br/>handler.exported?"}
    Validate -->|нет| Reject["Отклонить и записать в лог"]
    Validate -->|да| Resolve["Resolution<br/>Forward с ProfileNavKey 42"]
    Resolve --> Flags["FeatureFlags middleware"]
    Flags --> Auth{"Auth middleware<br/>SessionManager.isAuthorized?"}
    Auth -->|да| Profile["Analytics → dispatch<br/>профиль в бэкстеке"]
    Auth -->|нет| SignIn["Forward с AuthNavKey<br/>returnTo = ProfileNavKey 42"]
    SignIn --> AuthScreen["Analytics → dispatch<br/>экран входа"]
    AuthScreen --> Login["AuthViewModel.signIn<br/>SessionManager.login"]
    Login --> Replace["execute Replace с returnTo<br/>снова очередь и весь pipeline"]
    Replace --> Profile
```

DeepLinkResolver сопоставляет URI с шаблонами DeepLinkHandler и строит NavKey из аргументов.
После Resolution обработка продолжается дальше по той же цепочке — без повторного старта с Logging.
Новый Replace после входа, напротив, является новой командой и проходит pipeline с начала.
Replace убирает экран входа, поэтому он не остаётся под целевым профилем.

Для External разрешена схема `navplay`; handler должен существовать и быть exported.
Internal пропускает внешнюю валидацию, но всё равно проходит Resolution, флаги и auth.
При совпадающих шаблонах resolver выбирает первый подошедший handler; это не механизм приоритета
между фичами — конфликтующие шаблоны лучше не регистрировать.

## 4. Типизированный результат между экранами

```mermaid
sequenceDiagram
    participant F as FeedViewModel
    participant N as NavigatorImpl
    participant P as Pipeline и dispatch
    participant M as MediaPickerViewModel
    F->>N: forResult с MediaPickerNavKey
    N->>N: pending[key] = CompletableDeferred
    N->>N: поставить Forward в очередь
    Note over F,N: Приостановлена вызывающая coroutine, не worker
    N->>P: обработать Forward
    P-->>N: результат обработки команды
    alt исходный ключ не стал вершиной стека
        N-->>F: Result.failure с NotNavigated
    else экран выбора открыт
        Note over M: UI создаёт NavDisplay по записи стека
        alt пользователь подтвердил выбор
            M->>N: finishWithResult с тем же ключом
            N->>N: complete(value) и удалить pending
            N-->>F: Result.success со списком MediaId
            N->>P: BackTo с inclusive = true через очередь
            Note over P: Удалить экран выбора
        else экран закрыт без результата
            M->>N: execute Back
            N->>P: обработать Back
            P->>N: popTo завершает pending как Cancelled
            N-->>F: Result.failure с Cancelled
        end
    end
```

MediaPickerNavKey реализует ResultKey для списка MediaId. Тот же экземпляр/равный ключ передаётся
в экран и в finishWithResult. Результат завершается **до** постановки BackTo в очередь,
иначе удаление записи считалось бы отменой ожидания.

AlreadyPending возвращается при повторном ожидании точно равного ключа. Если middleware остановил
Forward или заменил его цель, после обработки Request навигатор выдаёт NotNavigated.
Отмена вызывающей coroutine удаляет ожидание из pending; это не закрывает открытый экран автоматически.
Pending и CompletableDeferred живут в памяти и не восстанавливаются после смерти процесса.

## 5. Восстановление историй и время жизни состояния

```mermaid
flowchart TB
    Create["MainActivity.onCreate"] --> Decode["Bundle → SectionHistory<br/>Home, Requests, выбранный раздел"]
    Decode --> Need{"NavigatorImpl уже<br/>инициализирован?"}
    Need -->|да| Existing["Сохранить текущее состояние<br/>поворот Activity"]
    Need -->|нет| Session{"Есть действующая сессия?"}
    Session -->|да| Restore["Восстановить истории<br/>с валидными корнями и выбор"]
    Session -->|нет| Public["Открыть Home<br/>только публичный префикс"]
    Existing --> Cold{"Первый запуск Activity?"}
    Restore --> Cold
    Public --> Cold
    Cold -->|да| Intent["handleDeepLink текущего Intent"]
    Cold -->|нет| UI["setContent / NavDisplay"]
    Intent --> UI
    UI --> Save["onSaveInstanceState<br/>обе истории и выбор → Bundle"]
    Save -.->|при следующем пересоздании| Create
```

При повороте навигатор уже хранит стеки. При смерти процесса новый навигатор проверяет корни
и сессию перед тем, как передать записи в UI. Повреждённая или устаревшая история отбрасывается,
а стартовый Feed доступен всегда. Старый Bundle с одним стеком читается как история Home.

| Состояние | Владелец и механизм |
|---|---|
| Ключи обоих стеков и выбранный раздел | NavigatorImpl. BackStackCodec сериализует ключи, Activity сохраняет снимок в Bundle |
| Compose saveable state записи | rememberSaveableStateHolderNavEntryDecorator |
| ViewModel записи | rememberViewModelStoreNavEntryDecorator и фабрика выбранного DI |
| Сессия, flags, лог | Общие зависимости приложения; в playground хранятся в памяти |
| Ожидание forResult | pending внутри NavigatorImpl; только память текущего процесса |

Навигационный снимок содержит маршруты, Compose сохраняет состояние экранов отдельно.
При смерти процесса ViewModel создаются заново, сессия в памяти пропадает, ожидания результатов прекращаются.
При гостевом восстановлении Home обрезается на первой защищённой записи. Её потомки могут зависеть от её контекста.
Requests отбрасывается целиком. Эту проверку выполняет `initialize`, потому что загрузка снимка проходит мимо middleware.
`onNewIntent` ставит новую ссылку в очередь команд. При восстановлении Activity прежний Intent не проигрывается.

## 6. Независимые разделы и защищённый вход

```mermaid
flowchart TB
    Tap["Вкладка Requests"] --> Select["SelectSection(RequestsNavKey)"]
    Select --> Guard{"AuthMiddleware<br/>есть сессия?"}
    Guard -->|нет| Login["Forward(AuthNavKey)<br/>Home остаётся выбранным"]
    Login -->|Cancel или Back| Home["Home<br/>предыдущая запись"]
    Login -->|успешный вход| Resume["SelectSection(RequestsNavKey)<br/>fromSignIn = true"]
    Resume --> Dispatch["dispatch<br/>удалить Login и выбрать Requests"]
    Guard -->|да| Dispatch
    Dispatch --> Requests["Requests: список → карточка"]
    Requests -->|выбрать Home| Home
    Home -->|снова выбрать Requests с сессией| Requests
    Requests -->|Back от корня| Home
```

В гостевом режиме доступны Feed и MediaPicker, если его флаг включён. Оба ключа Requests защищены.
Вход хранит исходную цель и тип операции. Успешный вход убирает Login с исходного стека перед выбором
Requests, поэтому Back из списка возвращает на предыдущий экран Home.
Logout очищает Requests даже при выбранном Home. Это предотвращает показ карточки прошлой сессии после нового входа.

`AppNavigation` держит отдельный `rememberDecoratedNavEntries` на каждый стек.
Декораторы отслеживают удаление записей, поэтому смена вкладки сохраняет ViewModel и Compose state
скрытого раздела. При выборе Requests `NavDisplay` получает Home + Requests, чтобы за корнем
Requests была сцена Home. С Home передаётся только его история, поэтому на его корне Back обрабатывает Android.
Префикс `requests:` в contentKey отделяет состояние одинаковых ключей, если, например, Profile открыт в обеих историях.

Экраны списка и карточки читают один локальный набор данных. В ключе карточки передаётся только ID.
Сценарий проверяет навигацию без отдельной ViewModel для статических данных.
Официальные примеры описывают [несколько стеков](https://developer.android.com/guide/navigation/navigation-3/recipes/multiple-backstacks)
и [декораторы записей](https://developer.android.com/guide/navigation/navigation-3/naventrydecorators).

## Исходники и проверки

- [Контракт Navigator и ошибки результатов](../core/navigation/src/main/kotlin/dev/navplay/core/navigation/Navigator.kt)
- [Команды и работа с целевым ключом](../core/navigation/src/main/kotlin/dev/navplay/core/navigation/NavigationCommand.kt)
- [NavigatorImpl: очередь, pending, dispatch и startup hooks](../app/src/main/kotlin/dev/navplay/app/navigation/NavigatorImpl.kt)
- [NavigationPipeline](../core/navigation/src/main/kotlin/dev/navplay/core/navigation/NavigationPipeline.kt)
- [DeepLinkResolver и UriPattern](../core/navigation/src/main/kotlin/dev/navplay/core/navigation/DeepLink.kt)
- [AuthMiddleware](../feature/auth/impl/src/main/kotlin/dev/navplay/feature/auth/impl/AuthMiddleware.kt)
- [BackStackCodec](../core/navigation/src/main/kotlin/dev/navplay/core/navigation/NavKeySerializers.kt)
- [AppNavigation и декораторы записей](../app/src/main/kotlin/dev/navplay/app/navigation/AppNavigation.kt)
- [MainActivity: Intent и сохранение состояния](../app/src/main/kotlin/dev/navplay/app/MainActivity.kt)
- [NavigatorImplTest](../app/src/test/kotlin/dev/navplay/app/navigation/NavigatorImplTest.kt)
- [NavigationPipelineTest](../core/navigation/src/test/kotlin/dev/navplay/core/navigation/NavigationPipelineTest.kt)
- [BackStackCodecTest](../core/navigation/src/test/kotlin/dev/navplay/core/navigation/BackStackCodecTest.kt)

Ссылки открывают код текущей ветки. Способ создания объектов сравнивается в [атласе DI](di/README.md),
статус проверок проекта — в [WORK_CONTEXT.md](WORK_CONTEXT.md).
