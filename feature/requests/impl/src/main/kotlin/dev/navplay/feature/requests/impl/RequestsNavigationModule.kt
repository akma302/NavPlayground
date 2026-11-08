package dev.navplay.feature.requests.impl

import androidx.navigation3.runtime.EntryProviderScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dev.navplay.core.navigation.NavEntryContributor
import dev.navplay.core.navigation.NavKey
import dev.navplay.core.navigation.NavKeySerializers
import dev.navplay.core.navigation.NavigationCommand
import dev.navplay.core.navigation.Navigator
import dev.navplay.core.navigation.navKeys
import dev.navplay.feature.requests.api.RequestDetailsNavKey
import dev.navplay.feature.requests.api.RequestsNavKey

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

@Module
@InstallIn(SingletonComponent::class)
object RequestsNavigationModule {
    @Provides
    @IntoSet
    fun entries(navigator: Navigator): NavEntryContributor = RequestsEntryContributor(navigator)

    @Provides
    @IntoSet
    fun navKeySerializers(): NavKeySerializers = navKeys {
        subclass(RequestsNavKey::class, RequestsNavKey.serializer())
        subclass(RequestDetailsNavKey::class, RequestDetailsNavKey.serializer())
    }
}
