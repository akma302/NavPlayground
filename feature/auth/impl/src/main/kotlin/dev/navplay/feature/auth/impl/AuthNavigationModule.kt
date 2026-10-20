package dev.navplay.feature.auth.impl

import androidx.navigation3.runtime.EntryProviderScope
import dev.navplay.core.navigation.NavEntryContributor
import dev.navplay.core.navigation.NavKey
import dev.navplay.core.navigation.NavKeySerializers
import dev.navplay.core.navigation.navKeys
import dev.navplay.feature.auth.api.AuthNavKey

class AuthEntryContributor() : NavEntryContributor {
    override fun register(scope: EntryProviderScope<NavKey>) {
        scope.entry<AuthNavKey> { key -> AuthScreen(returnTo = key.returnTo) }
    }
}

/** Feature-owned serialization; AppContainer explicitly registers the feature. */
object AuthNavigationModule {
    fun navKeySerializers(): NavKeySerializers =
        navKeys { subclass(AuthNavKey::class, AuthNavKey.serializer()) }
}
