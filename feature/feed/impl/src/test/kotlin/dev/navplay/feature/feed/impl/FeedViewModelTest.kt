package dev.navplay.feature.feed.impl

import dev.navplay.core.navigation.DeepLinkSource
import dev.navplay.core.navigation.InMemoryFeatureFlags
import dev.navplay.core.navigation.NavigationCommand
import dev.navplay.core.navigation.NavigationLog
import dev.navplay.core.navigation.NavigationResultException
import dev.navplay.core.navigation.testing.FakeNavigator
import dev.navplay.core.navigation.testing.MainDispatcherRule
import dev.navplay.feature.auth.api.AuthNavKey
import dev.navplay.feature.auth.api.Session
import dev.navplay.feature.auth.api.SessionManager
import dev.navplay.feature.media.api.MediaId
import dev.navplay.feature.media.api.MediaPickerNavKey
import dev.navplay.feature.profile.api.ProfileNavKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private class FakeSessions : SessionManager {
    private val _session = MutableStateFlow<Session?>(null)
    override val session: StateFlow<Session?> = _session
    override fun login(userName: String) {
        _session.value = Session(userName)
    }

    override fun logout() {
        _session.value = null
    }
}

class FeedViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val navigator = FakeNavigator()
    private val sessions = FakeSessions()
    private val flags = InMemoryFeatureFlags()
    private val log = NavigationLog()
    private fun viewModel() = FeedViewModel(navigator, sessions, flags, log)

    @Test
    fun `openProfile emits Forward with the profile key`() {
        viewModel().openProfile("1")

        assertEquals(
            listOf<NavigationCommand>(NavigationCommand.Forward(ProfileNavKey("1"))),
            navigator.commands,
        )
    }

    @Test
    fun `double tap emits two identical commands - throttling is middleware's job`() {
        viewModel().openProfileDoubleTap("1")

        assertEquals(2, navigator.commands.size)
        assertEquals(navigator.commands[0], navigator.commands[1])
    }

    @Test
    fun `openSignIn emits Forward to auth without return destination`() {
        viewModel().openSignIn()

        assertEquals(
            listOf<NavigationCommand>(NavigationCommand.Forward(AuthNavKey())),
            navigator.commands,
        )
    }

    @Test
    fun `pickMedia shows the typed result`() {
        navigator.resultProvider = { Result.success(listOf(MediaId("a"), MediaId("b"))) }
        val vm = viewModel()

        vm.pickMedia()

        assertEquals(listOf<Any>(MediaPickerNavKey(maxCount = 3)), navigator.requestedResults)
        assertEquals(PickResult.Picked(listOf(MediaId("a"), MediaId("b"))), vm.state.value.pickResult)
    }

    @Test
    fun `pickMedia shows why there is no result`() {
        navigator.resultProvider = { key -> Result.failure(NavigationResultException.Cancelled(key)) }
        val vm = viewModel()

        vm.pickMedia()

        val result = vm.state.value.pickResult
        assertTrue("was $result", result is PickResult.Failed)
        assertTrue((result as PickResult.Failed).reason.contains("without a result"))
    }

    @Test
    fun `feature flag and session are reflected in the state`() {
        val vm = viewModel()
        assertTrue(vm.state.value.mediaPickerEnabled)
        assertEquals(null, vm.state.value.userName)

        vm.setMediaPickerEnabled(false)
        sessions.login("ada")

        assertEquals(false, vm.state.value.mediaPickerEnabled)
        assertEquals("ada", vm.state.value.userName)
    }

    @Test
    fun `deep link demo is sent with its source`() {
        val vm = viewModel()

        vm.openDeepLink(FeedViewModel.demoLinks.first())

        assertEquals(
            listOf<NavigationCommand>(NavigationCommand.DeepLink("navplay://profile/42", DeepLinkSource.External)),
            navigator.commands,
        )
    }

    @Test
    fun `navigation log is exposed and can be cleared`() {
        val vm = viewModel()

        log.log("throttle", "dropped")
        assertEquals(1, vm.state.value.log.size)

        vm.clearLog()
        assertTrue(vm.state.value.log.isEmpty())
    }
}
