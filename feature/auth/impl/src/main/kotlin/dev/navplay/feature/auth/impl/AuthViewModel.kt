package dev.navplay.feature.auth.impl

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.navplay.core.navigation.NavKey
import dev.navplay.core.navigation.NavigationCommand
import dev.navplay.core.navigation.Navigator
import dev.navplay.feature.auth.api.SessionManager
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val navigator: Navigator,
    private val sessions: SessionManager,
) : ViewModel() {

    fun signIn(name: String, returnTo: NavKey?, selectSection: Boolean = false) {
        sessions.login(name.trim().ifEmpty { "guest" })
        // Обычный переход заменяет Login целью. Для раздела навигатор снимает Login с
        // исходного стека и переключает выбранную историю одной командой.
        val continuation = when {
            returnTo == null -> NavigationCommand.Back
            selectSection -> NavigationCommand.SelectSection(returnTo, fromSignIn = true)
            else -> NavigationCommand.Replace(returnTo)
        }
        navigator.execute(continuation)
    }

    fun cancel() {
        navigator.execute(NavigationCommand.Back)
    }
}
