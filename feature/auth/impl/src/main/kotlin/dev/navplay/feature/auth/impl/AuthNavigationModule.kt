package dev.navplay.feature.auth.impl

import androidx.navigation3.runtime.EntryProviderScope
import dev.navplay.core.navigation.DeepLinkHandler
import dev.navplay.core.navigation.NavEntryContributor
import dev.navplay.core.navigation.NavKey
import dev.navplay.core.navigation.NavKeySerializers
import dev.navplay.core.navigation.NavigationMiddleware
import dev.navplay.core.navigation.navKeys
import dev.navplay.feature.auth.api.AuthNavKey
import dev.navplay.feature.auth.api.SessionManager
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

class AuthEntryContributor() : NavEntryContributor {
    override fun register(scope: EntryProviderScope<NavKey>) {
        scope.entry<AuthNavKey> { key -> AuthScreen(returnTo = key.returnTo) }
    }
}

/** Feature-owned registrations. Qualifiers keep collection contributions distinct. */
object AuthNavigationModule {
    val module: Module = module {
        factory<NavEntryContributor>(named("auth.entries")) { AuthEntryContributor() }
        factory<NavKeySerializers>(named("auth.serializers")) { navKeySerializers() }
        factory<DeepLinkHandler>(named("auth.deepLink")) { AuthDeepLinkHandler() }
        single<SessionManager> { InMemorySessionManager() }
        factory<NavigationMiddleware>(named("auth.guard")) { AuthMiddleware(get(), get()) }
        viewModelOf(::AuthViewModel)
    }

    fun navKeySerializers(): NavKeySerializers =
        navKeys { subclass(AuthNavKey::class, AuthNavKey.serializer()) }
}
