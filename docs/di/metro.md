# Metro — текущая реализация

**Реализовано в `di/metro`, Metro 1.4.5, миграция `3e140eb`.**

```mermaid
flowchart TB
    subgraph Build["Сборка: Metro compiler plugin"]
        Scope["core:di<br/>AppScope"]
        Modules["app + четыре фичи<br/>@BindingContainer<br/>@ContributesTo(AppScope)"]
        VmDefs["Четыре ViewModel<br/>@Inject + @ViewModelKey<br/>@ContributesIntoMap(AppScope)"]
        Compiler["Проверка и генерация<br/>AppGraph : ViewModelGraph"]
        Scope -->|общий маркер агрегации| Modules
        Scope --> VmDefs
        Modules -->|"@Binds / @Provides / @IntoSet"| Compiler
        VmDefs -->|map providers| Compiler
    end
    subgraph Runtime["Во время работы приложения"]
        App["PlaygroundApp<br/>lazy createGraph()"]
        Graph["AppGraph<br/>один экземпляр на Application"]
        Shared["@SingleIn(AppScope)<br/>NavigatorImpl, SessionManager<br/>flags, log, resolver, codec, analytics, scope"]
        Sets["Наборы вкладов<br/>entries / middleware<br/>handlers / serializers"]
        Activity["MainActivity<br/>application.appGraph"]
        Local["CompositionLocalProvider<br/>LocalMetroViewModelFactory"]
        Nav["AppNavigation / NavDisplay<br/>Screen: metroViewModel()"]
        Factory["AppViewModelFactory<br/>Map: KClass → provider"]
        VM["Feed / Auth / Profile / Media<br/>ViewModel"]
        App -->|создаёт и хранит| Graph
        Graph -->|предоставляет| Shared
        Graph -->|собирает| Sets
        Graph -->|предоставляет фабрику| Factory
        Graph -->|явный доступ| Activity
        Activity -->|передаёт фабрику| Local
        Local -->|доступна в Composition| Nav
        Sets -->|entries| Nav
        Shared -->|navigator| Nav
        Nav -->|запрашивает через ViewModelStore| Factory
        Factory -->|вызывает provider| VM
        Shared -->|constructor injection| VM
    end
    Compiler -.->|генерирует реализацию| Graph
```

Android создаёт обычные Application и Activity. Граф создаётся явно в Application;
MainActivity получает его через `application`, без Hilt-аннотаций и field injection.
Вклады фич автоматически обнаруживаются на classpath по общему AppScope.

AppViewModelFactory — singleton фабрика; создаваемые ею ViewModel не singleton.
`metroViewModel()` использует ViewModelStore текущей записи NavDisplay, поэтому две записи
могут иметь разные ProfileViewModel с общей SessionManager.

Набор ViewModel providers хранит функции создания, а не заранее созданные ViewModel.
MetroX также предоставляет карты assisted factories; сейчас приложение их не использует.

## Привязка к исходникам

- [AppScope](../../core/di/src/main/kotlin/dev/navplay/core/di/AppScope.kt)
- [AppGraph](../../app/src/main/kotlin/dev/navplay/app/di/AppGraph.kt)
- [Application: создание графа](../../app/src/main/kotlin/dev/navplay/app/PlaygroundApp.kt)
- [MainActivity: передача фабрики в Compose](../../app/src/main/kotlin/dev/navplay/app/MainActivity.kt)
- [Навигационные привязки и providers](../../app/src/main/kotlin/dev/navplay/app/di/AppNavigationModule.kt)
- [AppViewModelFactory](../../app/src/main/kotlin/dev/navplay/app/di/AppViewModelFactory.kt)
- [Пример ViewModel](../../feature/feed/impl/src/main/kotlin/dev/navplay/feature/feed/impl/FeedViewModel.kt)
- [MetroGraphTest](../../app/src/test/kotlin/dev/navplay/app/di/MetroGraphTest.kt)

Для версии 1.4.5 Compose API находятся в `dev.zacsweers.metrox.viewmodel` без `.compose`.
Scope ставится на injected class/provider, а не на `@Binds`.

[Документация Metro](https://zacsweers.github.io/metro/latest/) · [Сравнение реализаций](README.md)
