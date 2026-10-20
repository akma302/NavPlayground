# NavPlayground — «флагманская» навигация на Navigation 3

Playground для подготовки к собеседованию: многомодульное Android-приложение, где работает каждый механизм
из шпаргалки. Откройте в Android Studio (нужна версия, поддерживающая AGP 9.4), запустите `:app` — на главном
экране кнопки для каждого механизма, а внизу **журнал навигации**, где видно, что сделала цепочка middleware.

## Граф модулей

```
                      :app   (composition root: владеет бэкстеком, собирает entry через DI)
        ┌──────────┬───┴──────┬───────────┐
   feed:impl  profile:impl  auth:impl  media:impl      ← impl видят только свой api, чужие api и core
        │          │           │           │
   feed:api   profile:api   auth:api   media:api       ← @Serializable NavKey + маленькие контракты
        └──────────┴─────┬─────┴───────────┘
                  :core:navigation                      ← абстракции, middleware, deep link, кодек бэкстека
                         ▲
              :core:navigation-testing                  ← FakeNavigator, MainDispatcherRule (только в тестах)
```

Правила: `impl` → чужие `api` и `core`, **никогда** чужой `impl`; `core` не знает ни одной фичи;
`:app` — единственный, кто видит все `impl`.
В Metro-варианте `app` и feature `impl` также используют `core:di` с общим маркером AppScope.

## Реализации DI и схемы архитектуры

| Реализация | Ветка | Статус | Визуализация |
|---|---|---|---|
| Hilt | `di/hilt`, исходная `main` | Реализован | [Граф Hilt](docs/di/hilt.md) |
| Metro 1.4.5 | `di/metro` | Реализован; текущий вариант | [Граф Metro](docs/di/metro.md) |
| Koin | `di/koin` | Миграция не начата, в ветке ещё Hilt | [План графа Koin](docs/di/koin.md) |

В [атласе DI](docs/di/README.md) собраны общая схема навигационных вкладов, сравнение времени жизни
объектов и подробные схемы каждой реализации со ссылками на код. Диаграммы Mermaid отображаются на GitHub.

В Metro-версии Application хранит один AppGraph; фичи вносят зависимости через AppScope,
а ViewModel создаются Metro-фабрикой и живут в ViewModelStore записи навигации.

Назначение документов: этот README — обзор, запуск и тесты; [AGENTS.md](AGENTS.md) — правила работы;
[WORK_CONTEXT.md](docs/WORK_CONTEXT.md) — актуальный статус, проверки и дальнейшие шаги.

## Где что лежит (карта «вопрос интервьюера → код»)

| Тема | Код |
|---|---|
| NavKey, команды, `Navigator` | `core/navigation/.../NavKey.kt`, `NavigationCommand.kt`, `Navigator.kt` |
| Middleware-цепочка («луковица», явный `order`) | `NavigationMiddleware.kt`, `NavigationPipeline.kt` |
| Auth guard + `returnTo` | `feature/auth/impl/.../AuthMiddleware.kt`, `AuthViewModel.kt` |
| Feature flags → заглушка | `core/.../middleware/FeatureFlagMiddleware.kt`, `StubNavKey.kt` |
| Аналитика / throttling / логирование | `core/.../middleware/{Analytics,Throttling,Logging}Middleware.kt` |
| Бэкстек как `SnapshotStateList`, очередь команд | `app/.../navigation/NavigatorImpl.kt` |
| Регистрация экранов через DI (`@IntoSet`) | `*NavigationModule.kt` в каждой `impl`, `NavEntryContributor.kt` |
| `NavDisplay`, ViewModel на запись стека, fallback-entry | `app/.../navigation/AppNavigation.kt` |
| Типизированные результаты (`ResultKey<T>`, `forResult`) | `Navigator.kt`, `NavigatorImpl.kt`, `feature/media/*`, `FeedViewModel.pickMedia` |
| Deep links: handler → resolver → тот же пайплайн | `DeepLink.kt`, `DeepLinkValidationMiddleware.kt`, `DeepLinkResolutionMiddleware.kt` |
| Process death | `NavKeySerializers.kt` (`BackStackCodec`), `MainActivity.kt` |
| Convention plugins (serialization в каждом `api`) | `build-logic/convention` |

### Как проходит команда

```
execute(cmd) ─► очередь ─► Logging(-100) ─► Throttle(0) ─► DeepLinkValidation(10) ─► DeepLinkResolution(15)
                                                                      │ (DeepLink → Forward(key))
             ─► FeatureFlags(20) ─► Auth(30) ─► Analytics(40) ─► dispatch: меняет бэкстек
```

Deep link становится обычным `Forward` на шаге Resolution, поэтому дальше он проходит те же флаги, auth-guard
и аналитику, что и внутренний переход.

## Что попробовать на экране Feed

1. **Open profile #1** без входа → auth-guard открывает Sign in; после входа вы попадаете в профиль
   (`Replace(returnTo)`, экран входа не остаётся в стеке).
2. **Double tap** → в журнале `throttle: dropped duplicate`.
3. **Pick media** → типизированный результат. Выключите флаг `media_picker` и повторите: откроется заглушка,
   а вызывающий получит `NotNavigated`. «Cancel» на экране выбора даёт `Cancelled`.
4. **Deep links**: внешний `navplay://profile/42` (пройдёт через guard), `navplay://auth` (отклонён: не `exported`),
   `https://evil.example/...` (отклонён: схема не в allowlist).
5. **Process death**: «Don't keep activities» в настройках разработчика → сверните и вернитесь, стек восстановится.

Внешняя ссылка с компьютера:

```
adb shell am start -a android.intent.action.VIEW -d "navplay://profile/42" dev.navplay
```

## Тесты

`./gradlew test` — JVM-тесты без Android-устройства:

- `core/navigation`: порядок цепочки, отмена и подмена команд, throttling (с поддельными часами), флаги,
  валидация и резолв deep link, `UriPattern`, кодек бэкстека (включая повреждённое состояние);
- `app`: `NavigatorImplTest` — навигатор на **настоящих** `AuthMiddleware` и обработчиках deep link
  (стек, auth-redirect, внешние ссылки, результаты, отмена, падение middleware); `NavigationGraphTest` —
  «у каждого NavKey есть entry» и «каждый NavKey переживает process death» (лечит неявность DI-мультибайндингов);
- `app`: `MetroGraphTest` — сгенерированный граф с production-вкладами: identity общих зависимостей,
  состав наборов, создание всех ViewModel, ViewModelStore и общая сессия/навигация;
- каждая фича: тесты ViewModel на `FakeNavigator` и контрактные тесты своих deep link.

## Честные слабые места схемы (спросят на собесе)

- Boilerplate: на 3 фичах 3 модуля на фичу — overkill; окупается с ростом числа команд.
- Неявность мультибайндингов: ловится `NavigationGraphTest` и fallback-entry.
- Отладка цепочки сложнее прямого вызова: поэтому есть `LoggingMiddleware` и журнал на экране.
- Ожидающие результаты (`forResult`) живут в памяти и не переживают смерть процесса — после неё экран-результат
  восстановится, но вызывающий уже не ждёт.
- Navigation 3 молодая; на Nav2 та же схема ложится на `NavController`, меняется только `dispatch`.

## Статус проверки

2026-10-20: `./gradlew :app:assembleDebug testDebugUnitTest --console=plain` — успешно,
91 JVM-тест, без ошибок. Скомпилированы Metro-граф, Compose-экраны и Navigation 3; создан debug APK.
Проверка UI на устройстве/эмуляторе и release-сборка не выполнялись.

Gradle 9.6.0 задан в wrapper; `gradle/gradle-daemon-jvm.properties` выбирает JDK 25 для daemon.
