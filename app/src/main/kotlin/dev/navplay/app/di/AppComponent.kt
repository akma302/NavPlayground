package dev.navplay.app.di

import androidx.lifecycle.ViewModelProvider
import dagger.Component
import dev.navplay.app.navigation.NavigationScope
import dev.navplay.app.navigation.NavigatorImpl
import dev.navplay.core.navigation.BackStackCodec
import dev.navplay.core.navigation.NavEntryContributor
import dev.navplay.core.navigation.NavigationLog
import dev.navplay.feature.auth.impl.AuthNavigationModule
import dev.navplay.feature.feed.impl.FeedNavigationModule
import dev.navplay.feature.media.impl.MediaNavigationModule
import dev.navplay.feature.profile.impl.ProfileNavigationModule
import dev.navplay.feature.requests.impl.RequestsNavigationModule
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope

/**
 * Список модулей задаёт состав конкретной сборки. Само наличие фичи в classpath не подключает
 * её маршруты: `:app` явно выбирает, чьи вклады войдут в наборы и карту ViewModel.
 */
@Singleton
@Component(
    modules = [
        AppNavigationModule::class,
        NavigatorBindingModule::class,
        ViewModelBindingModule::class,
        AuthNavigationModule::class,
        FeedNavigationModule::class,
        MediaNavigationModule::class,
        ProfileNavigationModule::class,
        RequestsNavigationModule::class,
    ],
)
interface AppComponent {
    val navigator: NavigatorImpl
    val contributors: Set<NavEntryContributor>
    val codec: BackStackCodec
    val log: NavigationLog
    val viewModelFactory: ViewModelProvider.Factory

    @get:NavigationScope
    val navigationScope: CoroutineScope
}
