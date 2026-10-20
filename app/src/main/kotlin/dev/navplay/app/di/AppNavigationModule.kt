package dev.navplay.app.di

import dev.navplay.app.navigation.LogNavigationAnalytics
import dev.navplay.core.navigation.BackStackCodec
import dev.navplay.core.navigation.DeepLinkHandler
import dev.navplay.core.navigation.DeepLinkResolver
import dev.navplay.core.navigation.FeatureFlags
import dev.navplay.core.navigation.InMemoryFeatureFlags
import dev.navplay.core.navigation.MutableFeatureFlags
import dev.navplay.core.navigation.NavKeySerializers
import dev.navplay.core.navigation.NavigationAnalytics
import dev.navplay.core.navigation.NavigationLog
import dev.navplay.core.navigation.NavigationMiddleware
import dev.navplay.core.navigation.StubNavKey
import dev.navplay.core.navigation.middleware.AnalyticsMiddleware
import dev.navplay.core.navigation.middleware.DeepLinkResolutionMiddleware
import dev.navplay.core.navigation.middleware.DeepLinkValidationMiddleware
import dev.navplay.core.navigation.middleware.FeatureFlagMiddleware
import dev.navplay.core.navigation.middleware.LoggingMiddleware
import dev.navplay.core.navigation.middleware.ThrottlingMiddleware
import dev.navplay.core.navigation.navKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Инфраструктура навигации и «общие» middleware. Auth-middleware сюда не попадает: его явно добавляет
 * AppContainer при сборке цепочки. Порядок исполнения задаёт `order`.
 */
object AppNavigationModule {

    fun navigationLog(): NavigationLog = NavigationLog()

    fun navigationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun mutableFeatureFlags(): MutableFeatureFlags = InMemoryFeatureFlags()

    fun featureFlags(flags: MutableFeatureFlags): FeatureFlags = flags

    fun analytics(log: NavigationLog): NavigationAnalytics = LogNavigationAnalytics(log)

    fun deepLinkResolver(handlers: Set<DeepLinkHandler>): DeepLinkResolver =
        DeepLinkResolver(handlers)

    fun backStackCodec(contributions: Set<NavKeySerializers>): BackStackCodec =
        BackStackCodec(contributions)

    // --- сериализатор ключа заглушки (сам ключ живёт в core, экран — в app) ---

    fun stubNavKeySerializers(): NavKeySerializers =
        navKeys { subclass(StubNavKey::class, StubNavKey.serializer()) }

    // --- middleware: порядок — по NavigationMiddleware.Order, а не по порядку объявления ---

    fun loggingMiddleware(log: NavigationLog): NavigationMiddleware = LoggingMiddleware(log)

    fun throttlingMiddleware(log: NavigationLog): NavigationMiddleware = ThrottlingMiddleware(log)

    fun deepLinkValidationMiddleware(resolver: DeepLinkResolver, log: NavigationLog): NavigationMiddleware =
        DeepLinkValidationMiddleware(resolver, log, allowedSchemes = setOf("navplay"))

    fun deepLinkResolutionMiddleware(resolver: DeepLinkResolver, log: NavigationLog): NavigationMiddleware =
        DeepLinkResolutionMiddleware(resolver, log)

    fun featureFlagMiddleware(flags: FeatureFlags, log: NavigationLog): NavigationMiddleware =
        FeatureFlagMiddleware(flags, log)

    fun analyticsMiddleware(analytics: NavigationAnalytics): NavigationMiddleware =
        AnalyticsMiddleware(analytics)
}
