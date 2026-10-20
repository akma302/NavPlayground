package dev.navplay.app.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dev.navplay.app.navigation.LogNavigationAnalytics
import dev.navplay.app.navigation.NavigationScope
import dev.navplay.app.navigation.NavigatorImpl
import dev.navplay.app.navigation.StubEntryContributor
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
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
abstract class NavigatorBindingModule {
    @Binds
    abstract fun navigator(impl: NavigatorImpl): Navigator

    @Binds
    @IntoSet
    abstract fun stubEntries(impl: StubEntryContributor): NavEntryContributor
}

/**
 * Инфраструктура навигации и «общие» middleware. Auth-middleware сюда не попадает: его вносит сама
 * фича `:feature:auth:impl` (`@IntoSet`), `:app` о нём не знает. Порядок исполнения задаёт `order`.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppNavigationModule {

    @Provides
    @Singleton
    fun navigationLog(): NavigationLog = NavigationLog()

    @Provides
    @Singleton
    @NavigationScope
    fun navigationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Provides
    @Singleton
    fun mutableFeatureFlags(): MutableFeatureFlags = InMemoryFeatureFlags()

    @Provides
    fun featureFlags(flags: MutableFeatureFlags): FeatureFlags = flags

    @Provides
    @Singleton
    fun analytics(log: NavigationLog): NavigationAnalytics = LogNavigationAnalytics(log)

    @Provides
    @Singleton
    fun deepLinkResolver(handlers: Set<@JvmSuppressWildcards DeepLinkHandler>): DeepLinkResolver =
        DeepLinkResolver(handlers)

    @Provides
    @Singleton
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
    @Singleton
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
