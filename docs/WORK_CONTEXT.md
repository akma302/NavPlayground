# Контекст работы — 2026-10-20

Это сохранённый снимок для продолжения работы в другой сессии. Перед действиями сверить с Git.

## Цель и принятые решения

Сравнить три DI-подхода в одном Android-приложении с Navigation 3, сохранив одинаковое поведение.
Пользователь выбрал отдельные Git-ветки вместо копий проекта и попросил начать с Metro.
Общую архитектурную абстракцию для переключения DI в одной сборке сейчас не создаём.
Koin ещё не внедрён; следующий шаг требует учитывать актуальный запрос пользователя.

## Репозиторий и ветки

- Origin: https://github.com/akma302/NavPlayground.git
- `main`, `di/hilt`: исходная Hilt-реализация, коммит `a028ebe`.
- `di/koin`: пока та же исходная Hilt-реализация, коммит `a028ebe`.
- `di/metro`: завершённая миграция на Metro 1.4.5, коммит `3e140eb`; отправлен в origin.
- Работа выполнялась в `di/metro`. Имя ветки не гарантирует, что одноимённый DI уже внедрён:
  для `di/koin` миграция ещё предстоит.

## Как устроена Metro-версия

- Convention plugins подключают Metro вместо Hilt/KSP; Dagger/Hilt и `javax.inject` в исходниках удалены.
- `core:di` содержит общий маркер `AppScope`; не зависит от фич.
- `app/di/AppGraph.kt`: `@DependencyGraph(AppScope::class)`, наследует `ViewModelGraph`.
- `PlaygroundApp` лениво создаёт один `AppGraph` через `createGraph<AppGraph>()` на процесс.
- `MainActivity` получает зависимости из графа Application и передаёт
  `LocalMetroViewModelFactory` через `CompositionLocalProvider`.
- `*NavigationModule.kt`: `@BindingContainer`, `@ContributesTo(AppScope::class)`,
  `@Binds` в виде extension properties и наборы `@IntoSet`.
- Граф собирает 5 contributors экранов, 7 middleware, 3 deep link handlers и сериализаторы NavKey.
  Порядок middleware определяется `order`, а не порядком DI-набора.
- Общие зависимости помечены `@SingleIn(AppScope::class)`. `NavigationScope` — qualifier
  для process-level CoroutineScope (`SupervisorJob + Dispatchers.Main.immediate`), а не DI scope.
- ViewModel: `@Inject`, `@ViewModelKey`, `@ContributesIntoMap(AppScope::class)`.
  `AppViewModelFactory` наследует `MetroViewModelFactory`; экраны используют `metroViewModel()`.
- Важная деталь API 1.4.5: Compose-функции импортируются из `dev.zacsweers.metrox.viewmodel`,
  без дополнительного пакета `.compose`. Scope ставится на реализацию/provider, не на `@Binds`.
- `rememberViewModelStoreNavEntryDecorator` сохраняет время жизни ViewModel по записи бэкстека.
  ViewModel не кэшируются как singleton приложения.

## Выполненные проверки

До миграции исходная Hilt-версия успешно прошла assembleDebug и JVM-тесты.
После миграции выполнено:

```powershell
.\gradlew.bat clean :app:assembleDebug testDebugUnitTest --console=plain
```

Результат: BUILD SUCCESSFUL, 91 тест, 0 failures/errors; создан
`app/build/outputs/apk/debug/app-debug.apk`.

Новый `app/.../di/MetroGraphTest.kt` создаёт отдельный сгенерированный проверочный граф
с production-вкладами. Проверяет identity общих зависимостей, состав наборов,
регистрации экранов, сериализацию ключей, создание всех четырёх ViewModel,
разделение экземпляров между ViewModelStore и общую сессию/навигатор.
`NavigationGraphTest` оставлен как ручной тест контрактов.

## Ограничения и дальнейшая работа

- UI не запускался на устройстве/эмуляторе; release-сборка не проверялась.
- Следующая полезная проверка Metro: запуск, auth redirect, выбор медиа с результатом,
  deep links, поворот экрана и восстановление после смерти процесса.
- Следующая DI-миграция — Koin в `di/koin`, начиная с исходного Hilt, а не с Metro.
- Общие исправления логики при необходимости переносить между ветками отдельно от DI-изменений.
- Ожидания результатов навигации находятся в памяти и не переживают смерть процесса —
  это существующее ограничение, не добавленное миграцией Metro.

## Среда

Windows / PowerShell. Gradle wrapper 9.6.0; AGP 9.4.0; Kotlin 2.4.21;
daemon JVM 25 задан в `gradle/gradle-daemon-jvm.properties`.
Локальный SDK задаётся через игнорируемый `local.properties`.
Ограниченная среда может потребовать разрешение для Gradle-кеша/сети и Git push.
GitHub CLI (`gh`) не установлен; обычные Git fetch/push работают через настроенный credential helper.

Официальная документация Metro: https://zacsweers.github.io/metro/latest/
Подробности устройства приложения: `README.md`.
