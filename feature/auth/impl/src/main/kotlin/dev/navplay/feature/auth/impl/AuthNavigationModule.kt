package dev.navplay.feature.auth.impl

import androidx.navigation3.runtime.EntryProviderScope
import dev.navplay.core.di.AppScope
import dev.navplay.core.navigation.DeepLinkHandler
import dev.navplay.core.navigation.NavEntryContributor
import dev.navplay.core.navigation.NavKey
import dev.navplay.core.navigation.NavKeySerializers
import dev.navplay.core.navigation.NavigationMiddleware
import dev.navplay.core.navigation.navKeys
import dev.navplay.feature.auth.api.AuthNavKey
import dev.navplay.feature.auth.api.SessionManager
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides

/** Фича сама вносит себя в граф: экран, middleware, deep link и сериализатор ключа. */
@Inject
class AuthEntryContributor() : NavEntryContributor {
    override fun register(scope: EntryProviderScope<NavKey>) {
        scope.entry<AuthNavKey> { key -> AuthScreen(returnTo = key.returnTo) }
    }
}

@BindingContainer
@ContributesTo(AppScope::class)
abstract class AuthNavigationModule {
    @Binds
    abstract val InMemorySessionManager.sessionManager: SessionManager

    @Binds
    @IntoSet
    abstract val AuthMiddleware.middleware: NavigationMiddleware

    @Binds
    @IntoSet
    abstract val AuthEntryContributor.entries: NavEntryContributor

    @Binds
    @IntoSet
    abstract val AuthDeepLinkHandler.deepLink: DeepLinkHandler

    companion object {
        @Provides
        @IntoSet
        fun navKeySerializers(): NavKeySerializers =
            navKeys { subclass(AuthNavKey::class, AuthNavKey.serializer()) }
    }
}
