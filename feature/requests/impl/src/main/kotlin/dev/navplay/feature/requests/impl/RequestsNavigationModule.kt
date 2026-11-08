package dev.navplay.feature.requests.impl

import androidx.navigation3.runtime.EntryProviderScope
import dev.navplay.core.di.AppScope
import dev.navplay.core.navigation.NavEntryContributor
import dev.navplay.core.navigation.NavKey
import dev.navplay.core.navigation.NavKeySerializers
import dev.navplay.core.navigation.NavigationCommand
import dev.navplay.core.navigation.Navigator
import dev.navplay.core.navigation.navKeys
import dev.navplay.feature.requests.api.RequestDetailsNavKey
import dev.navplay.feature.requests.api.RequestsNavKey
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides

@Inject
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

@BindingContainer
@ContributesTo(AppScope::class)
abstract class RequestsNavigationModule {
    @Binds
    @IntoSet
    abstract val RequestsEntryContributor.entries: NavEntryContributor

    companion object {
        @Provides
        @IntoSet
        fun navKeySerializers(): NavKeySerializers = navKeys {
            subclass(RequestsNavKey::class, RequestsNavKey.serializer())
            subclass(RequestDetailsNavKey::class, RequestDetailsNavKey.serializer())
        }
    }
}
