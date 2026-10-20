package dev.navplay.feature.profile.impl

import dev.navplay.core.navigation.NavKey
import dev.navplay.core.navigation.NavigationCommand
import dev.navplay.core.navigation.parse
import dev.navplay.core.navigation.testing.FakeNavigator
import dev.navplay.core.navigation.testing.MainDispatcherRule
import dev.navplay.feature.auth.api.Session
import dev.navplay.feature.auth.api.SessionManager
import dev.navplay.feature.feed.api.FeedNavKey
import dev.navplay.feature.profile.api.ProfileNavKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/** Контрактные тесты deep link: они компенсируют отсутствие compile-time safety у строковых URI. */
class ProfileDeepLinkHandlerTest {
    private val handler = ProfileDeepLinkHandler()

    @Test
    fun `profile uri becomes the profile key`() {
        assertEquals(ProfileNavKey("42"), handler.parse("navplay://profile/42"))
    }

    @Test
    fun `query parameters do not break the key`() {
        assertEquals(ProfileNavKey("42"), handler.parse("navplay://profile/42?ref=push"))
    }

    @Test
    fun `other uris are not ours`() {
        assertNull(handler.parse("navplay://feed"))
        assertNull(handler.parse("navplay://profile"))
        assertNull(handler.parse("https://profile/42"))
    }

    @Test
    fun `profile is reachable from outside - the auth guard protects it, not the allowlist`() {
        assertEquals(true, handler.exported)
    }
}

class ProfileViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private class FakeSessions : SessionManager {
        private val _session = MutableStateFlow<Session?>(Session("ada"))
        override val session: StateFlow<Session?> = _session
        override fun login(userName: String) {
            _session.value = Session(userName)
        }

        override fun logout() {
            _session.value = null
        }
    }

    private val navigator = FakeNavigator()
    private val sessions = FakeSessions()
    private val viewModel = ProfileViewModel(navigator, sessions)

    @Test
    fun `openProfile and replaceWith emit the matching commands`() {
        viewModel.openProfile("2")
        viewModel.replaceWith("3")

        assertEquals(
            listOf<NavigationCommand>(
                NavigationCommand.Forward(ProfileNavKey("2")),
                NavigationCommand.Replace(ProfileNavKey("3")),
            ),
            navigator.commands,
        )
    }

    @Test
    fun `backToFeed keeps the feed`() {
        viewModel.backToFeed()

        assertEquals(
            listOf<NavigationCommand>(NavigationCommand.BackTo(FeedNavKey, inclusive = false)),
            navigator.commands,
        )
    }

    @Test
    fun `logout ends the session and resets the stack`() {
        viewModel.logout()

        assertNull(sessions.session.value)
        assertEquals(
            listOf<NavigationCommand>(NavigationCommand.NewRoot(FeedNavKey)),
            navigator.commands,
        )
    }

    @Test
    fun `keys are plain data - only ids are passed between features`() {
        val key: NavKey = ProfileNavKey("1")

        assertEquals(ProfileNavKey("1"), key)
    }
}
