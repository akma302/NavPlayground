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
- каждая фича: тесты ViewModel на `FakeNavigator` и контрактные тесты своих deep link.

## Честные слабые места схемы (спросят на собесе)

- Boilerplate: на 3 фичах 3 модуля на фичу — overkill; окупается с ростом числа команд.
- Неявность мультибайндингов: ловится `NavigationGraphTest` и fallback-entry.
- Отладка цепочки сложнее прямого вызова: поэтому есть `LoggingMiddleware` и журнал на экране.
- Ожидающие результаты (`forResult`) живут в памяти и не переживают смерть процесса — после неё экран-результат
  восстановится, но вызывающий уже не ждёт.
- Navigation 3 молодая; на Nav2 та же схема ложится на `NavController`, меняется только `dispatch`.

## Статус проверки

Проверено запуском (Kotlin 2.0.21, coroutines 1.6.4, JUnit 4.13.2 на заглушках Android-классов):
логика core, `NavigatorImpl`, все middleware, ViewModel, обработчики deep link — 82 теста зелёные,
намеренно внесённые в навигатор ошибки тесты ловят.

**Не проверялось сборкой**: Gradle sync, Hilt/KSP, Compose-экраны, `NavDisplay`, сериализация (`BackStackCodecTest`,
`NavigationGraphTest`) — в окружении, где писался проект, не было доступа к Google/Maven. Версии в
`gradle/libs.versions.toml` взяты из официальных страниц релизов, но их совместимость между собой (KSP ↔ Kotlin,
Hilt ↔ AGP 9, имена API Navigation 3) не проверена. Если sync ругается — начните с этого файла.
AGP 9.4.0 требует Gradle 9.6.0 (прописан в wrapper) и JDK 17+.
