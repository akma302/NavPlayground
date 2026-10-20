package dev.navplay.feature.auth.impl

import androidx.lifecycle.ViewModel
import dev.navplay.core.di.AppScope
import dev.navplay.core.navigation.NavKey
import dev.navplay.core.navigation.NavigationCommand
import dev.navplay.core.navigation.Navigator
import dev.navplay.feature.auth.api.SessionManager
import dev.navplay.feature.feed.api.FeedNavKey
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey

@ViewModelKey
@ContributesIntoMap(AppScope::class)
@Inject
class AuthViewModel(
    private val navigator: Navigator,
    private val sessions: SessionManager,
) : ViewModel() {

    fun signIn(name: String, returnTo: NavKey?) {
        sessions.login(name.trim().ifEmpty { "guest" })
        // Replace, а не Forward: экран входа не должен остаться в бэкстеке под целевым экраном.
        navigator.execute(NavigationCommand.Replace(returnTo ?: FeedNavKey))
    }

    fun cancel() {
        navigator.execute(NavigationCommand.Back)
    }
}
