package dev.navplay.feature.requests.impl

import dev.navplay.core.navigation.NavEntryContributor
import dev.navplay.core.navigation.ScreenRegistry
import dev.navplay.core.navigation.NavKeySerializers
import dev.navplay.core.navigation.NavigationCommand
import dev.navplay.core.navigation.Navigator
import dev.navplay.core.navigation.navKeys
import dev.navplay.feature.requests.api.RequestDetailsNavKey
import dev.navplay.feature.requests.api.RequestsNavKey

class RequestsEntryContributor(private val navigator: Navigator) : NavEntryContributor {
    override fun register(scope: ScreenRegistry) {
        scope.entry(RequestsNavKey::class) {
            RequestsScreen { id -> navigator.execute(NavigationCommand.Forward(RequestDetailsNavKey(id))) }
        }
        scope.entry(RequestDetailsNavKey::class) { key ->
            RequestDetailsScreen(key.id) { navigator.execute(NavigationCommand.Back) }
        }
    }
}

object RequestsNavigationModule {
    fun navKeySerializers(): NavKeySerializers = navKeys {
        subclass(RequestsNavKey::class, RequestsNavKey.serializer())
        subclass(RequestDetailsNavKey::class, RequestDetailsNavKey.serializer())
    }
}
