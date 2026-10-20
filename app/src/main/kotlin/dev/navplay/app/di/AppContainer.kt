package dev.navplay.app.di

import dev.navplay.app.navigation.NavigatorImpl
import dev.navplay.app.navigation.StubEntryContributor
import dev.navplay.core.di.ManualViewModelFactory
import dev.navplay.core.navigation.DeepLinkHandler
import dev.navplay.core.navigation.NavEntryContributor
import dev.navplay.core.navigation.NavKeySerializers
import dev.navplay.core.navigation.NavigationMiddleware
import dev.navplay.feature.auth.api.SessionManager
import dev.navplay.feature.auth.impl.AuthDeepLinkHandler
import dev.navplay.feature.auth.impl.AuthEntryContributor
import dev.navplay.feature.auth.impl.AuthMiddleware
import dev.navplay.feature.auth.impl.AuthNavigationModule
import dev.navplay.feature.auth.impl.AuthViewModel
import dev.navplay.feature.auth.impl.InMemorySessionManager
import dev.navplay.feature.feed.impl.FeedDeepLinkHandler
import dev.navplay.feature.feed.impl.FeedEntryContributor
import dev.navplay.feature.feed.impl.FeedNavigationModule
import dev.navplay.feature.feed.impl.FeedViewModel
import dev.navplay.feature.media.impl.MediaEntryContributor
import dev.navplay.feature.media.impl.MediaNavigationModule
import dev.navplay.feature.media.impl.MediaPickerViewModel
import dev.navplay.feature.profile.impl.ProfileDeepLinkHandler
import dev.navplay.feature.profile.impl.ProfileEntryContributor
import dev.navplay.feature.profile.impl.ProfileNavigationModule
import dev.navplay.feature.profile.impl.ProfileViewModel
import kotlinx.coroutines.cancel

/** Composition root: every instance and connection is explicit. One container per Application. */
class AppContainer : AutoCloseable {
    val log = AppNavigationModule.navigationLog()
    val navigationScope = AppNavigationModule.navigationScope()
    val flags = AppNavigationModule.mutableFeatureFlags()
    val sessions: SessionManager = InMemorySessionManager()
    val analytics = AppNavigationModule.analytics(log)

    val deepLinkHandlers: Set<DeepLinkHandler> = setOf(
        AuthDeepLinkHandler(), FeedDeepLinkHandler(), ProfileDeepLinkHandler(),
    )
    val resolver = AppNavigationModule.deepLinkResolver(deepLinkHandlers)

    val serializers: Set<NavKeySerializers> = setOf(
        AuthNavigationModule.navKeySerializers(),
        FeedNavigationModule.navKeySerializers(),
        ProfileNavigationModule.navKeySerializers(),
        MediaNavigationModule.navKeySerializers(),
        AppNavigationModule.stubNavKeySerializers(),
    )
    val codec = AppNavigationModule.backStackCodec(serializers)

    val middleware: Set<NavigationMiddleware> = setOf(
        AppNavigationModule.loggingMiddleware(log),
        AppNavigationModule.throttlingMiddleware(log),
        AppNavigationModule.deepLinkValidationMiddleware(resolver, log),
        AppNavigationModule.deepLinkResolutionMiddleware(resolver, log),
        AppNavigationModule.featureFlagMiddleware(flags, log),
        AuthMiddleware(sessions, log),
        AppNavigationModule.analyticsMiddleware(analytics),
    )
    val navigator = NavigatorImpl(middleware, log, navigationScope)

    val contributors: Set<NavEntryContributor> = setOf(
        AuthEntryContributor(), FeedEntryContributor(), ProfileEntryContributor(),
        MediaEntryContributor(), StubEntryContributor(navigator),
    )

    val viewModelFactory = ManualViewModelFactory(mapOf(
        FeedViewModel::class to { FeedViewModel(navigator, sessions, flags, log) },
        AuthViewModel::class to { AuthViewModel(navigator, sessions) },
        ProfileViewModel::class to { ProfileViewModel(navigator, sessions) },
        MediaPickerViewModel::class to { MediaPickerViewModel(navigator) },
    ))

    /** Explicit cleanup for tests or another explicit owner; Android keeps this for the process. */
    override fun close() {
        navigationScope.cancel()
    }
}
