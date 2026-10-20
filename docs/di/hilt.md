# Hilt — исходная реализация

**Реализовано в `di/hilt`, исходный коммит `a028ebe`.**
Схема привязана к исходному Hilt-коду. В main теперь отдельная реализация manual DI.

```mermaid
flowchart TB
    subgraph Build["Сборка: Dagger / Hilt + KSP"]
        Infra["app: AppNavigationModule<br/>NavigatorBindingModule"]
        Features["auth / feed / profile / media<br/>NavigationModule"]
        Declarations["@Inject constructor<br/>@HiltViewModel"]
        Compiler["Hilt compiler<br/>проверка и генерация графа"]
        Infra -->|"@Module + @InstallIn"| Compiler
        Features -->|"@Binds / @Provides / @IntoSet"| Compiler
        Declarations --> Compiler
    end
    subgraph Runtime["Во время работы приложения"]
        App["PlaygroundApp<br/>@HiltAndroidApp"]
        Graph["SingletonComponent"]
        Shared["@Singleton<br/>NavigatorImpl, SessionManager<br/>flags, log, resolver, codec, analytics, scope"]
        Sets["Наборы вкладов<br/>entries / middleware<br/>handlers / serializers"]
        Activity["MainActivity<br/>@AndroidEntryPoint"]
        Nav["AppNavigation / NavDisplay"]
        Screen["Screen<br/>hiltViewModel()"]
        Factory["Hilt ViewModel factory<br/>ViewModelComponent"]
        VM["Feed / Auth / Profile / Media<br/>ViewModel"]
        Graph -->|предоставляет| Shared
        Graph -->|собирает| Sets
        Graph -->|field injection| Activity
        Shared -->|navigator, codec, log| Activity
        Activity -->|navigator, contributors| Nav
        Sets -->|entries| Nav
        Nav -->|отображает запись| Screen
        Screen -->|получает через фабрику| Factory
        Graph -->|родительские зависимости| Factory
        Factory -->|создаёт для ViewModelStore| VM
        Shared -->|constructor injection| VM
        App -->|владеет корневым компонентом| Graph
    end
    Compiler -.->|генерирует реализацию| Graph
```

Hilt управляет Android-точками входа: Application запускает корневой компонент, Activity получает
поля через injection. ViewModel запрашивают интерфейс Navigator; Activity получает NavigatorImpl
для работы с бэкстеком. `@Binds` связывает их с одним экземпляром.

Фичи сами вносят экран, сериализатор и, где предусмотрено, deep link handler и auth middleware.
Граф собирает четыре набора; их потребители показаны на [общей схеме](README.md).
Полная иерархия Hilt-компонентов, включая ActivityRetainedComponent, здесь опущена.

Время жизни ViewModel определяется ViewModelStore записи NavDisplay:
`rememberViewModelStoreNavEntryDecorator`, а не SingletonComponent.

## Привязка к исходникам

- [Application и Hilt entry point](https://github.com/akma302/NavPlayground/blob/a028ebe/app/src/main/kotlin/dev/navplay/app/PlaygroundApp.kt)
- [MainActivity: field injection](https://github.com/akma302/NavPlayground/blob/a028ebe/app/src/main/kotlin/dev/navplay/app/MainActivity.kt)
- [Инфраструктура и общие зависимости](https://github.com/akma302/NavPlayground/blob/a028ebe/app/src/main/kotlin/dev/navplay/app/di/AppNavigationModule.kt)
- [Auth: session binding и вклады фичи](https://github.com/akma302/NavPlayground/blob/a028ebe/feature/auth/impl/src/main/kotlin/dev/navplay/feature/auth/impl/AuthNavigationModule.kt)
- [ViewModelStore на запись навигации](https://github.com/akma302/NavPlayground/blob/a028ebe/app/src/main/kotlin/dev/navplay/app/navigation/AppNavigation.kt)

[Сравнение реализаций](README.md)

[Архитектура навигации: от команды до экрана](../navigation.md)
