package dev.navplay.feature.auth.impl

import dev.navplay.core.navigation.NavKey
import dev.navplay.core.navigation.NavigationCommand
import dev.navplay.core.navigation.NavigationLog
import dev.navplay.core.navigation.NavigationPipeline
import dev.navplay.core.navigation.RequiresAuth
import dev.navplay.core.navigation.parse
import dev.navplay.core.navigation.testing.FakeNavigator
import dev.navplay.core.navigation.testing.MainDispatcherRule
import dev.navplay.feature.auth.api.AuthNavKey
import dev.navplay.feature.feed.api.FeedNavKey
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private data class Secret(val id: String) : NavKey, RequiresAuth
private data class Open(val id: String) : NavKey

class AuthMiddlewareTest {
    private val sessions = InMemorySessionManager()
    private val seen = mutableListOf<NavigationCommand>()
    private val pipeline = NavigationPipeline(
        listOf(AuthMiddleware(sessions, NavigationLog())),
    ) { seen += it }

    @Test
    fun `unauthorized user is redirected to sign-in with a return destination`() = runBlocking {
        pipeline.run(NavigationCommand.Forward(Secret("1")))

        assertEquals(
            listOf<NavigationCommand>(NavigationCommand.Forward(AuthNavKey(returnTo = Secret("1")))),
            seen,
        )
    }

    @Test
    fun `command type is preserved when redirecting`() = runBlocking {
        pipeline.run(NavigationCommand.Replace(Secret("1")))
        pipeline.run(NavigationCommand.NewRoot(Secret("2")))

        assertEquals(
            listOf<NavigationCommand>(
                NavigationCommand.Replace(AuthNavKey(returnTo = Secret("1"))),
                NavigationCommand.NewRoot(AuthNavKey(returnTo = Secret("2"))),
            ),
            seen,
        )
    }

    @Test
    fun `authorized user passes`() = runBlocking {
        sessions.login("ada")
        val command = NavigationCommand.Forward(Secret("1"))

        pipeline.run(command)

        assertEquals(listOf<NavigationCommand>(command), seen)
    }

    @Test
    fun `open screens and non target commands are never redirected`() = runBlocking {
        pipeline.run(NavigationCommand.Forward(Open("1")))
        pipeline.run(NavigationCommand.Back)

        assertEquals(
            listOf(NavigationCommand.Forward(Open("1")), NavigationCommand.Back),
            seen,
        )
    }

    @Test
    fun `sign-in screen itself is not guarded - no redirect loop`() = runBlocking {
        val command = NavigationCommand.Forward(AuthNavKey())

        pipeline.run(command)

        assertEquals(listOf<NavigationCommand>(command), seen)
    }
}

class AuthDeepLinkHandlerTest {
    private val handler = AuthDeepLinkHandler()

    @Test
    fun `auth uri opens sign-in`() {
        assertEquals(AuthNavKey(), handler.parse("navplay://auth"))
    }

    @Test
    fun `auth is internal-only`() {
        assertFalse(handler.exported)
    }

    @Test
    fun `other uris are not ours`() {
        assertNull(handler.parse("navplay://auth/extra"))
    }
}

class AuthViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val navigator = FakeNavigator()
    private val sessions = InMemorySessionManager()
    private val viewModel = AuthViewModel(navigator, sessions)

    @Test
    fun `sign-in replaces itself with the screen the user wanted`() {
        viewModel.signIn("  Bob ", returnTo = Secret("7"))

        assertEquals("Bob", sessions.session.value?.userName)
        assertEquals(
            listOf<NavigationCommand>(NavigationCommand.Replace(Secret("7"))),
            navigator.commands,
        )
    }

    @Test
    fun `sign-in without return destination goes home and blank name becomes guest`() {
        viewModel.signIn("   ", returnTo = null)

        assertEquals("guest", sessions.session.value?.userName)
        assertEquals(
            listOf<NavigationCommand>(NavigationCommand.Replace(FeedNavKey)),
            navigator.commands,
        )
    }

    @Test
    fun `cancel is Back and does not sign in`() {
        viewModel.cancel()

        assertTrue(sessions.session.value == null)
        assertEquals(listOf<NavigationCommand>(NavigationCommand.Back), navigator.commands)
    }
}
