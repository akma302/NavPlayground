package dev.navplay.feature.auth.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import dagger.multibindings.IntoSet
import dev.navplay.core.navigation.DeepLinkHandler
import dev.navplay.core.navigation.NavEntryContributor
import dev.navplay.core.navigation.NavKey
import dev.navplay.core.navigation.NavKeySerializers
import dev.navplay.core.navigation.NavigationMiddleware
import dev.navplay.core.navigation.navKeys
import dev.navplay.feature.auth.api.AuthNavKey
import dev.navplay.feature.auth.api.SessionManager
import javax.inject.Inject
import javax.inject.Singleton

/** Фича сама вносит себя в граф: экран, middleware, deep link и сериализатор ключа. */
class AuthEntryContributor @Inject constructor(
    private val factory: ViewModelProvider.Factory,
) : NavEntryContributor {
    override fun register(scope: EntryProviderScope<NavKey>) {
        scope.entry<AuthNavKey> { key ->
            AuthScreen(key.returnTo, key.selectSection, viewModel(factory = factory))
        }
    }
}

@Module
abstract class AuthNavigationModule {
    @Binds
    @IntoMap
    @ClassKey(AuthViewModel::class)
    abstract fun viewModel(impl: AuthViewModel): ViewModel

    @Binds
    @Singleton
    abstract fun sessionManager(impl: InMemorySessionManager): SessionManager

    @Binds
    @IntoSet
    abstract fun middleware(impl: AuthMiddleware): NavigationMiddleware

    @Binds
    @IntoSet
    abstract fun entries(impl: AuthEntryContributor): NavEntryContributor

    @Binds
    @IntoSet
    abstract fun deepLink(impl: AuthDeepLinkHandler): DeepLinkHandler

    companion object {
        @Provides
        @IntoSet
        fun navKeySerializers(): NavKeySerializers =
            navKeys { subclass(AuthNavKey::class, AuthNavKey.serializer()) }
    }
}
