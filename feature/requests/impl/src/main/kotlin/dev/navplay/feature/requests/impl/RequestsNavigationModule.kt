package dev.navplay.feature.requests.impl

import androidx.navigation3.runtime.EntryProviderScope
import dev.navplay.core.navigation.NavEntryContributor
import dev.navplay.core.navigation.NavKey
import dev.navplay.core.navigation.NavKeySerializers
import dev.navplay.core.navigation.NavigationCommand
import dev.navplay.core.navigation.Navigator
import dev.navplay.core.navigation.navKeys
import dev.navplay.feature.requests.api.RequestDetailsNavKey
import dev.navplay.feature.requests.api.RequestsNavKey
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

class RequestsEntryContributor(private val navigator: Navigator) : NavEntryContributor {
    override fun register(scope: EntryProviderScope<NavKey>) {
        scope.entry<RequestsNavKey> {
            RequestsScreen { id -> navigator.execute(NavigationCommand.Forward(RequestDetailsNavKey(id))) }
        }
        scope.entry<RequestDetailsNavKey> { key ->
            RequestDetailsScreen(key.id) { navigator.execute(NavigationCommand.Back) }
        }
    }
}

object RequestsNavigationModule {
    val module: Module = module {
        factory<NavEntryContributor>(named("requests.entries")) { RequestsEntryContributor(get()) }
        factory<NavKeySerializers>(named("requests.serializers")) { navKeySerializers() }
    }

    fun navKeySerializers(): NavKeySerializers = navKeys {
        subclass(RequestsNavKey::class, RequestsNavKey.serializer())
        subclass(RequestDetailsNavKey::class, RequestDetailsNavKey.serializer())
    }
}
