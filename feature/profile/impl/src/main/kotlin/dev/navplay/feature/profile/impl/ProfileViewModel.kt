package dev.navplay.feature.profile.impl

import androidx.lifecycle.ViewModel
import dev.navplay.core.di.AppScope
import dev.navplay.core.navigation.NavigationCommand
import dev.navplay.core.navigation.Navigator
import dev.navplay.feature.auth.api.Session
import dev.navplay.feature.auth.api.SessionManager
import dev.navplay.feature.feed.api.FeedNavKey
import dev.navplay.feature.profile.api.ProfileNavKey
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.StateFlow

@ViewModelKey
@ContributesIntoMap(AppScope::class)
@Inject
class ProfileViewModel(
    private val navigator: Navigator,
    private val sessions: SessionManager,
) : ViewModel() {

    val session: StateFlow<Session?> = sessions.session

    /** Forward к другому профилю: фича не знает про себя ничего, кроме собственного ключа. */
    fun openProfile(userId: String) {
        navigator.execute(NavigationCommand.Forward(ProfileNavKey(userId)))
    }

    fun replaceWith(userId: String) {
        navigator.execute(NavigationCommand.Replace(ProfileNavKey(userId)))
    }

    fun backToFeed() {
        navigator.execute(NavigationCommand.BackTo(FeedNavKey, inclusive = false))
    }

    fun logout() {
        sessions.logout()
        navigator.execute(NavigationCommand.NewRoot(FeedNavKey))
    }
}
