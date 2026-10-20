package dev.navplay.app.di

import dev.navplay.app.navigation.LogNavigationAnalytics
import dev.navplay.app.navigation.NavigationScope
import dev.navplay.app.navigation.NavigatorImpl
import dev.navplay.app.navigation.StubEntryContributor
import dev.navplay.core.navigation.BackStackCodec
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.dsl.onClose

/** Common infrastructure. Auth guard is supplied by the auth feature. */
object AppNavigationModule {
    val module: Module = module {
        single { NavigationLog() }
        single<CoroutineScope>(NavigationScope) {
            CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        } onClose { it?.cancel() }
        single<MutableFeatureFlags> { InMemoryFeatureFlags() } bind FeatureFlags::class
        single<NavigationAnalytics> { LogNavigationAnalytics(get()) }
        single { DeepLinkResolver(getAll()) }
        single { BackStackCodec(getAll()) }
        single { NavigatorImpl(getAll<NavigationMiddleware>().toSet(), get(), get(NavigationScope)) } bind Navigator::class

        factory<NavEntryContributor>(named("app.stub.entries")) { StubEntryContributor(get()) }
        factory<NavKeySerializers>(named("app.stub.serializers")) { stubNavKeySerializers() }
        factory<NavigationMiddleware>(named("app.logging")) { LoggingMiddleware(get()) }
        single<NavigationMiddleware>(named("app.throttling")) { ThrottlingMiddleware(get()) }
        factory<NavigationMiddleware>(named("app.deepLink.validation")) {
            DeepLinkValidationMiddleware(get(), get(), allowedSchemes = setOf("navplay"))
        }
        factory<NavigationMiddleware>(named("app.deepLink.resolution")) { DeepLinkResolutionMiddleware(get(), get()) }
        factory<NavigationMiddleware>(named("app.featureFlags")) { FeatureFlagMiddleware(get(), get()) }
        factory<NavigationMiddleware>(named("app.analytics")) { AnalyticsMiddleware(get()) }
    }

    fun stubNavKeySerializers(): NavKeySerializers =
        navKeys { subclass(StubNavKey::class, StubNavKey.serializer()) }
}
