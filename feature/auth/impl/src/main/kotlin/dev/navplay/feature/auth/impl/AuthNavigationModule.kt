package dev.navplay.feature.auth.impl

import dev.navplay.core.navigation.NavEntryContributor
import dev.navplay.core.navigation.ScreenRegistry
import dev.navplay.core.navigation.NavKeySerializers
import dev.navplay.core.navigation.navKeys
import dev.navplay.feature.auth.api.AuthNavKey

class AuthEntryContributor() : NavEntryContributor {
    override fun register(scope: ScreenRegistry) {
        scope.entry(AuthNavKey::class) { key ->
            AuthScreen(returnTo = key.returnTo, selectSection = key.selectSection)
        }
    }
}

/** Feature-owned serialization; AppContainer explicitly registers the feature. */
object AuthNavigationModule {
    fun navKeySerializers(): NavKeySerializers =
        navKeys { subclass(AuthNavKey::class, AuthNavKey.serializer()) }
}
