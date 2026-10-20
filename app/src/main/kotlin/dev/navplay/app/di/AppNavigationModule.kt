package dev.navplay.app.di

import dev.navplay.app.navigation.LogNavigationAnalytics
import dev.navplay.app.navigation.NavigationScope
import dev.navplay.app.navigation.NavigatorImpl
import dev.navplay.app.navigation.StubEntryContributor
import dev.navplay.core.di.AppScope
import dev.navplay.core.navigation.BackStackCodec
import dev.navplay.core.navigation.DeepLinkHandler
import dev.navplay.core.navigation.DeepLinkResolver
import dev.navplay.core.navigation.FeatureFlags
import dev.navplay.core.navigation.InMemoryFeatureFlags
import dev.navplay.core.navigation.MutableFeatureFlags
import dev.navplay.core.navigation.NavEntryContributor
import dev.navplay.core.navigation.NavKeySerializers
import dev.navplay.core.navigation.NavigationAnalytics
import dev.navplay.core.navigation.NavigationLog
import dev.navplay.core.navigation.NavigationMiddleware
import dev.navplay.core.navigation.Navigator
import dev.navplay.core.navigation.StubNavKey
import dev.navplay.core.navigation.middleware.AnalyticsMiddleware
import dev.navplay.core.navigation.middleware.DeepLinkResolutionMiddleware
import dev.navplay.core.navigation.middleware.DeepLinkValidationMiddleware
import dev.navplay.core.navigation.middleware.FeatureFlagMiddleware
import dev.navplay.core.navigation.middleware.LoggingMiddleware
import dev.navplay.core.navigation.middleware.ThrottlingMiddleware
import dev.navplay.core.navigation.navKeys
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@BindingContainer
@ContributesTo(AppScope::class)
abstract class NavigatorBindingModule {
    @Binds
    abstract val NavigatorImpl.navigator: Navigator

    @Binds
    @IntoSet
    abstract val StubEntryContributor.stubEntries: NavEntryContributor
}

/**
 * Инфраструктура навигации и «общие» middleware. Auth-middleware сюда не попадает: его вносит сама
 * фича `:feature:auth:impl` (`@IntoSet`), `:app` о нём не знает. Порядок исполнения задаёт `order`.
 */
@BindingContainer
@ContributesTo(AppScope::class)
object AppNavigationModule {

    @Provides
    @SingleIn(AppScope::class)
    fun navigationLog(): NavigationLog = NavigationLog()

    @Provides
    @SingleIn(AppScope::class)
    @NavigationScope
    fun navigationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Provides
    @SingleIn(AppScope::class)
    fun mutableFeatureFlags(): MutableFeatureFlags = InMemoryFeatureFlags()

    @Provides
    fun featureFlags(flags: MutableFeatureFlags): FeatureFlags = flags

    @Provides
    @SingleIn(AppScope::class)
    fun analytics(log: NavigationLog): NavigationAnalytics = LogNavigationAnalytics(log)

    @Provides
    @SingleIn(AppScope::class)
    fun deepLinkResolver(handlers: Set<@JvmSuppressWildcards DeepLinkHandler>): DeepLinkResolver =
        DeepLinkResolver(handlers)

    @Provides
    @SingleIn(AppScope::class)
    fun backStackCodec(contributions: Set<@JvmSuppressWildcards NavKeySerializers>): BackStackCodec =
        BackStackCodec(contributions)

    // --- сериализатор ключа заглушки (сам ключ живёт в core, экран — в app) ---
    @Provides
    @IntoSet
    fun stubNavKeySerializers(): NavKeySerializers =
        navKeys { subclass(StubNavKey::class, StubNavKey.serializer()) }

    // --- middleware: порядок — по NavigationMiddleware.Order, а не по порядку объявления ---
    @Provides
    @IntoSet
    fun loggingMiddleware(log: NavigationLog): NavigationMiddleware = LoggingMiddleware(log)

    @Provides
    @IntoSet
    @SingleIn(AppScope::class)
    fun throttlingMiddleware(log: NavigationLog): NavigationMiddleware = ThrottlingMiddleware(log)

    @Provides
    @IntoSet
    fun deepLinkValidationMiddleware(resolver: DeepLinkResolver, log: NavigationLog): NavigationMiddleware =
        DeepLinkValidationMiddleware(resolver, log, allowedSchemes = setOf("navplay"))

    @Provides
    @IntoSet
    fun deepLinkResolutionMiddleware(resolver: DeepLinkResolver, log: NavigationLog): NavigationMiddleware =
        DeepLinkResolutionMiddleware(resolver, log)

    @Provides
    @IntoSet
    fun featureFlagMiddleware(flags: FeatureFlags, log: NavigationLog): NavigationMiddleware =
        FeatureFlagMiddleware(flags, log)

    @Provides
    @IntoSet
    fun analyticsMiddleware(analytics: NavigationAnalytics): NavigationMiddleware =
        AnalyticsMiddleware(analytics)
}
