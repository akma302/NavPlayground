# Архитектура навигации NavPlayground

Навигация одинакова в `main` (manual DI), `di/hilt`, `di/koin` и `di/metro`.
DI определяет способ создания объектов; контракты, очередь, middleware и операции со стеком общие.
Схемы отражают исходники; UI-поведение на устройстве пока отдельно не проверялось.

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
    Dispatch["app: NavigatorImpl.dispatch<br/>Forward / Replace / NewRoot / Back / BackTo"]
    Stack["Бэкстек NavigatorImpl<br/>Compose SnapshotStateList"]
    Entries["NavEntryContributor от фич<br/>register в entryProvider"]
    Display["app: AppNavigation / NavDisplay<br/>NavKey → NavEntry → Screen"]
    Startup["MainActivity.onCreate<br/>restore / ensureRoot"]
    Screen --> VM
    API -->|целевой ключ| VM
    VM -->|execute с командой| Contract
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
`feature:api` хранит NavKey; `feature:impl` — экран, ViewModel и навигационные вклады.
Фичи используют интерфейс Navigator из core и API ключей других фич, без зависимости от чужих impl.
`app` объединяет фичи, владеет реализацией навигатора и отображает стек через Navigation 3.

`execute()` сразу ставит Request в неограниченный Channel и возвращается. Worker обрабатывает
команды по одной: экран не вызывает NavDisplay или изменение списка напрямую.
Pipeline находится в core, но terminal-функция `dispatch` передаётся из NavigatorImpl в app.
Обработанные команды меняют стек через dispatch; `restore` и `ensureRoot` — отдельные startup hooks.

NavEntryContributor регистрирует сопоставление типа ключа с экраном.
Неизвестный ключ даёт UnknownDestinationScreen через fallback, а не ошибку поиска entry.
Способ собрать contributors различается между DI-вариантами: [атлас DI](di/README.md).
Для manual/Koin одного подключения Gradle-модуля недостаточно — регистрации добавляются явно.

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
FeatureFlagMiddleware подменяет целевой ключ на StubNavKey, AuthMiddleware — на AuthNavKey с returnTo;
тип Forward/Replace/NewRoot сохраняется. Аналитика видит уже изменённую цель.
Обычная ошибка обработки логируется на уровне Request; CancellationException распространяется дальше.

| Команда после middleware | Что делает dispatch |
|---|---|
| Forward | Добавляет ключ наверх |
| Replace | Удаляет верхний ключ, затем добавляет новый |
| NewRoot | Удаляет все записи и добавляет новый корень |
| Back | Удаляет верхнюю запись, если она не последняя |
| BackTo | Находит ближайший с конца равный ключ; inclusive удаляет и его |
| DeepLink | Только логирует неразрешённую ссылку: штатно Resolution уже должен превратить её в Forward |

BackTo игнорируется, если ключ не найден или операция оставила бы стек пустым.
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

## 5. Восстановление стека и время жизни состояния

```mermaid
flowchart TB
    Create["MainActivity.onCreate"] --> Need{"Есть savedInstanceState<br/>и стек навигатора пуст?"}
    Need -->|да| Decode["Bundle: STATE_BACKSTACK<br/>BackStackCodec.decodeOrNull"]
    Decode --> Valid{"Ключи успешно прочитаны?"}
    Valid -->|да| Restore["navigator.restore<br/>добавить ключи в пустой стек"]
    Valid -->|нет| Root["ensureRoot с FeedNavKey<br/>добавить только если стек пуст"]
    Restore --> Root
    Need -->|нет| Root
    Root --> Cold{"savedInstanceState отсутствует?"}
    Cold -->|да| Intent["handleDeepLink текущего Intent<br/>если URI есть"]
    Cold -->|нет| UI["setContent / NavDisplay"]
    Intent --> UI
    UI --> Save["onSaveInstanceState<br/>codec.encode копии бэкстека → Bundle"]
    Save -.->|при следующем пересоздании| Create
```

При повороте process-level навигатор уже хранит стек: восстановление не перезаписывает его.
После смерти процесса создаётся новый пустой навигатор; Activity пытается восстановить список ключей.
Повреждённый JSON или неизвестный сериализатор дают null; ensureRoot обеспечивает стартовый Feed.
Кодирование ключей выполняется через объединённые NavKeySerializers всех фич и app-заглушки.

| Состояние | Владелец и механизм |
|---|---|
| Список ключей экранов | NavigatorImpl; сохраняется BackStackCodec в Bundle |
| Compose saveable state записи | rememberSaveableStateHolderNavEntryDecorator |
| ViewModel записи | rememberViewModelStoreNavEntryDecorator и фабрика выбранного DI |
| Сессия, flags, лог | Общие зависимости приложения; в playground хранятся в памяти |
| Ожидание forResult | pending внутри NavigatorImpl; только память текущего процесса |

Восстановление ключей не восстанавливает объекты ViewModel, авторизованную сессию или незавершённые
ожидания результатов. Restore не прогоняет восстановленные ключи через middleware.
Внешний Intent при пересоздании с savedInstanceState повторно не обрабатывается;
`onNewIntent` передаёт новую ссылку в ту же очередь команд.

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
