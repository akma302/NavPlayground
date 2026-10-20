package dev.navplay.feature.feed.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.navplay.core.navigation.DeepLinkSource
import dev.navplay.core.navigation.MutableFeatureFlags
import dev.navplay.core.navigation.NavigationCommand
import dev.navplay.core.navigation.NavigationLog
import dev.navplay.core.navigation.Navigator
import dev.navplay.feature.auth.api.AuthNavKey
import dev.navplay.feature.auth.api.SessionManager
import dev.navplay.feature.media.api.MediaId
import dev.navplay.feature.media.api.MediaPickerNavKey
import dev.navplay.feature.profile.api.ProfileNavKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface PickResult {
    data object None : PickResult
    data class Picked(val ids: List<MediaId>) : PickResult
    data class Failed(val reason: String) : PickResult
}

data class FeedUiState(
    val userName: String? = null,
    val mediaPickerEnabled: Boolean = true,
    val pickResult: PickResult = PickResult.None,
    val log: List<NavigationLog.Entry> = emptyList(),
)

data class DeepLinkDemo(val label: String, val uri: String, val source: DeepLinkSource)

/**
 * Экран-«пульт»: на нём можно потрогать каждый механизм навигации и сразу увидеть,
 * что сделала цепочка middleware (журнал внизу).
 */
class FeedViewModel(
    private val navigator: Navigator,
    private val sessions: SessionManager,
    private val flags: MutableFeatureFlags,
    private val log: NavigationLog,
) : ViewModel() {

    private val pickResult = MutableStateFlow<PickResult>(PickResult.None)

    val state: StateFlow<FeedUiState> = combine(
        sessions.session,
        flags.disabled,
        pickResult,
        log.entries,
    ) { session, disabled, pick, entries ->
        FeedUiState(
            userName = session?.userName,
            mediaPickerEnabled = MediaPickerNavKey.FEATURE_ID !in disabled,
            pickResult = pick,
            log = entries,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, FeedUiState())

    /** Forward к экрану, требующему авторизации: без сессии сработает auth-guard. */
    fun openProfile(userId: String) {
        navigator.execute(NavigationCommand.Forward(ProfileNavKey(userId)))
    }

    /** Две одинаковые команды подряд: вторую отбросит ThrottlingMiddleware (защита от двойного клика). */
    fun openProfileDoubleTap(userId: String) {
        val command = NavigationCommand.Forward(ProfileNavKey(userId))
        navigator.execute(command)
        navigator.execute(command)
    }

    fun openSignIn() {
        navigator.execute(NavigationCommand.Forward(AuthNavKey()))
    }

    fun logout() {
        sessions.logout()
    }

    /** Типизированный результат: вызывающий код читается линейно, без SavedStateHandle и строковых ключей. */
    fun pickMedia() {
        viewModelScope.launch {
            navigator.forResult(MediaPickerNavKey(maxCount = 3))
                .onSuccess { pickResult.value = PickResult.Picked(it) }
                .onFailure { pickResult.value = PickResult.Failed(it.message ?: it::class.simpleName.orEmpty()) }
        }
    }

    fun setMediaPickerEnabled(enabled: Boolean) {
        flags.setEnabled(MediaPickerNavKey.FEATURE_ID, enabled)
    }

    fun openDeepLink(demo: DeepLinkDemo) {
        navigator.execute(NavigationCommand.DeepLink(demo.uri, demo.source))
    }

    fun clearLog() {
        log.clear()
    }

    companion object {
        val demoLinks = listOf(
            DeepLinkDemo("External: navplay://profile/42", "navplay://profile/42", DeepLinkSource.External),
            DeepLinkDemo("External: navplay://auth (internal-only → rejected)", "navplay://auth", DeepLinkSource.External),
            DeepLinkDemo("External: https://evil.example/profile/1 (scheme → rejected)", "https://evil.example/profile/1", DeepLinkSource.External),
            DeepLinkDemo("Internal: navplay://auth", "navplay://auth", DeepLinkSource.Internal),
        )
    }
}
