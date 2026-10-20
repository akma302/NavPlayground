package dev.navplay.feature.profile.impl

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.navplay.core.navigation.NavigationCommand
import dev.navplay.core.navigation.Navigator
import dev.navplay.feature.auth.api.Session
import dev.navplay.feature.auth.api.SessionManager
import dev.navplay.feature.feed.api.FeedNavKey
import dev.navplay.feature.profile.api.ProfileNavKey
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class ProfileViewModel @Inject constructor(
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
