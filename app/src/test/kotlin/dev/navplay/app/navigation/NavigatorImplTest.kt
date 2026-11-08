package dev.navplay.app.navigation

import com.arkivanov.decompose.DecomposeSettings
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry

import dev.navplay.core.navigation.DeepLinkResolver
import dev.navplay.core.navigation.DeepLinkSource
import dev.navplay.core.navigation.InMemoryFeatureFlags
import dev.navplay.core.navigation.NavKey
import dev.navplay.core.navigation.NavigationCommand
import dev.navplay.core.navigation.NavigationLog
import dev.navplay.core.navigation.NavigationMiddleware
import dev.navplay.core.navigation.NavigationResultException
import dev.navplay.core.navigation.StubNavKey
import dev.navplay.core.navigation.middleware.DeepLinkResolutionMiddleware
import dev.navplay.core.navigation.middleware.DeepLinkValidationMiddleware
import dev.navplay.core.navigation.middleware.FeatureFlagMiddleware
import dev.navplay.feature.auth.api.AuthNavKey
import dev.navplay.feature.auth.impl.AuthDeepLinkHandler
import dev.navplay.feature.auth.impl.AuthMiddleware
import dev.navplay.feature.auth.impl.AuthViewModel
import dev.navplay.feature.auth.impl.InMemorySessionManager
import dev.navplay.feature.feed.api.FeedNavKey
import dev.navplay.feature.feed.impl.FeedDeepLinkHandler
import dev.navplay.feature.media.api.MediaId
import dev.navplay.feature.media.api.MediaPickerNavKey
import dev.navplay.feature.profile.api.ProfileNavKey
import dev.navplay.feature.profile.impl.ProfileDeepLinkHandler
import dev.navplay.feature.requests.api.RequestDetailsNavKey
import dev.navplay.feature.requests.api.RequestsNavKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тесты навигатора на «боевых» кусках: настоящий AuthMiddleware, настоящие обработчики deep link,
 * настоящая цепочка middleware и два наблюдаемых стека из production.
 */
class NavigatorImplTest {
    private val log = NavigationLog()
    private val sessions = InMemorySessionManager()
    private val flags = InMemoryFeatureFlags()
    private val resolver = DeepLinkResolver(
        listOf(ProfileDeepLinkHandler(), AuthDeepLinkHandler(), FeedDeepLinkHandler()),
    )
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    private fun navigator(vararg extra: NavigationMiddleware, saved: SectionHistory? = null): NavigatorImpl {
        val middleware = listOf(
            DeepLinkValidationMiddleware(resolver, log, allowedSchemes = setOf("navplay")),
            DeepLinkResolutionMiddleware(resolver, log),
            FeatureFlagMiddleware(flags, log),
            AuthMiddleware(sessions, log),
        ) + extra
        return NavigatorImpl(middleware.toSet(), log, scope, sessions).also { it.attachTestRouter(saved) }
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    private suspend fun NavigatorImpl.run(command: NavigationCommand) {
        execute(command)
        awaitIdle()
    }

    // ------------------------------------------------------------------ базовые команды

    @Test
    fun `forward pushes, back pops`() = runBlocking {
        val nav = navigator()
        sessions.login("ada")

        nav.run(NavigationCommand.Forward(ProfileNavKey("1")))
        assertEquals(listOf(FeedNavKey, ProfileNavKey("1")), nav.backStack)

        nav.run(NavigationCommand.Back)
        assertEquals(listOf<NavKey>(FeedNavKey), nav.backStack)
    }

    @Test
    fun `back on the root is ignored`() = runBlocking {
        val nav = navigator()

        nav.run(NavigationCommand.Back)

        assertEquals(listOf<NavKey>(FeedNavKey), nav.backStack)
    }

    @Test
    fun `replace swaps the top, newRoot clears everything`() = runBlocking {
        val nav = navigator()
        sessions.login("ada")
        nav.run(NavigationCommand.Forward(ProfileNavKey("1")))

        nav.run(NavigationCommand.Replace(ProfileNavKey("2")))
        assertEquals(listOf(FeedNavKey, ProfileNavKey("2")), nav.backStack)

        nav.run(NavigationCommand.Forward(ProfileNavKey("3")))
        nav.run(NavigationCommand.NewRoot(FeedNavKey))
        assertEquals(listOf<NavKey>(FeedNavKey), nav.backStack)
    }

    @Test
    fun `backTo exclusive keeps the target, inclusive removes it`() = runBlocking {
        val nav = navigator()
        sessions.login("ada")
        nav.run(NavigationCommand.Forward(ProfileNavKey("1")))
        nav.run(NavigationCommand.Forward(ProfileNavKey("2")))
        nav.run(NavigationCommand.Forward(ProfileNavKey("3")))

        nav.run(NavigationCommand.BackTo(ProfileNavKey("2"), inclusive = false))
        assertEquals(listOf(FeedNavKey, ProfileNavKey("1"), ProfileNavKey("2")), nav.backStack)

        nav.run(NavigationCommand.BackTo(ProfileNavKey("2"), inclusive = true))
        assertEquals(listOf(FeedNavKey, ProfileNavKey("1")), nav.backStack)
    }

    @Test
    fun `backTo a missing key or to an emptying key changes nothing`() = runBlocking {
        val nav = navigator()
        sessions.login("ada")
        nav.run(NavigationCommand.Forward(ProfileNavKey("1")))

        nav.run(NavigationCommand.BackTo(ProfileNavKey("404")))
        nav.run(NavigationCommand.BackTo(FeedNavKey, inclusive = true))

        assertEquals(listOf(FeedNavKey, ProfileNavKey("1")), nav.backStack)
    }

    // ------------------------------------------------------------------ auth guard

    @Test
    fun `guarded screen redirects to sign-in and sign-in returns to it`() = runBlocking {
        val nav = navigator()

        nav.run(NavigationCommand.Forward(ProfileNavKey("7")))
        assertEquals(
            listOf(FeedNavKey, AuthNavKey(returnTo = ProfileNavKey("7"))),
            nav.backStack,
        )

        // Ровно то, что делает AuthViewModel.signIn.
        sessions.login("ada")
        nav.run(NavigationCommand.Replace(ProfileNavKey("7")))
        assertEquals(listOf(FeedNavKey, ProfileNavKey("7")), nav.backStack)
    }

    @Test
    fun `authorized user goes straight to the guarded screen`() = runBlocking {
        val nav = navigator()
        sessions.login("ada")

        nav.run(NavigationCommand.Forward(ProfileNavKey("7")))

        assertEquals(listOf(FeedNavKey, ProfileNavKey("7")), nav.backStack)
    }

    // ------------------------------------------------------------------ deep links

    @Test
    fun `external deep link goes through the same guard as internal navigation`() = runBlocking {
        val nav = navigator()

        nav.run(NavigationCommand.DeepLink("navplay://profile/42"))

        assertEquals(
            listOf(FeedNavKey, AuthNavKey(returnTo = ProfileNavKey("42"))),
            nav.backStack,
        )
    }

    @Test
    fun `external deep link opens the screen when authorized`() = runBlocking {
        val nav = navigator()
        sessions.login("ada")

        nav.run(NavigationCommand.DeepLink("navplay://profile/42"))

        assertEquals(listOf(FeedNavKey, ProfileNavKey("42")), nav.backStack)
    }

    @Test
    fun `external deep links to internal screens or foreign schemes are rejected`() = runBlocking {
        val nav = navigator()

        nav.run(NavigationCommand.DeepLink("navplay://auth"))
        nav.run(NavigationCommand.DeepLink("https://evil.example/profile/1"))
        nav.run(NavigationCommand.DeepLink("navplay://nothing"))

        assertEquals(listOf<NavKey>(FeedNavKey), nav.backStack)
    }

    @Test
    fun `internal deep link may open an internal-only screen`() = runBlocking {
        val nav = navigator()

        nav.run(NavigationCommand.DeepLink("navplay://auth", DeepLinkSource.Internal))

        assertEquals(listOf(FeedNavKey, AuthNavKey()), nav.backStack)
    }

    // ------------------------------------------------------------------ типизированные результаты

    private val picker = MediaPickerNavKey(maxCount = 3)

    @Test
    fun `forResult returns the typed result and closes the screen`() = runBlocking {
        val nav = navigator()
        val result = async(start = CoroutineStart.UNDISPATCHED) { nav.forResult(picker) }
        nav.awaitIdle()
        assertEquals(listOf(FeedNavKey, picker), nav.backStack)

        nav.finishWithResult(picker, listOf(MediaId("a"), MediaId("b")))
        nav.awaitIdle()

        assertEquals(listOf(MediaId("a"), MediaId("b")), result.await().getOrNull())
        assertEquals(listOf<NavKey>(FeedNavKey), nav.backStack)
    }

    @Test
    fun `closing the screen without a result cancels the request`() = runBlocking {
        val nav = navigator()
        val result = async(start = CoroutineStart.UNDISPATCHED) { nav.forResult(picker) }
        nav.awaitIdle()

        nav.run(NavigationCommand.Back)

        val failure = result.await().exceptionOrNull()
        assertTrue("was $failure", failure is NavigationResultException.Cancelled)
    }

    @Test
    fun `result request redirected by a feature flag fails with NotNavigated`() = runBlocking {
        val nav = navigator()
        flags.setEnabled(MediaPickerNavKey.FEATURE_ID, false)

        val result = async(start = CoroutineStart.UNDISPATCHED) { nav.forResult(picker) }
        nav.awaitIdle()

        val failure = result.await().exceptionOrNull()
        assertTrue("was $failure", failure is NavigationResultException.NotNavigated)
        // Пользователь при этом видит заглушку, а не пустоту.
        assertTrue(nav.backStack.last() is StubNavKey)
    }

    @Test
    fun `second request for the same key is rejected while the first is pending`() = runBlocking {
        val nav = navigator()
        val first = async(start = CoroutineStart.UNDISPATCHED) { nav.forResult(picker) }
        nav.awaitIdle()

        val second = nav.forResult(picker)

        assertTrue(second.exceptionOrNull() is NavigationResultException.AlreadyPending)
        first.cancel()
    }

    @Test
    fun `cancelled caller does not leave a stale pending request`() = runBlocking {
        val nav = navigator()
        val first = async(start = CoroutineStart.UNDISPATCHED) { nav.forResult(picker) }
        nav.awaitIdle()
        first.cancelAndJoin() // join: ждём, пока отработает finally в forResult

        val again = async(start = CoroutineStart.UNDISPATCHED) { nav.forResult(picker) }
        nav.awaitIdle()
        nav.finishWithResult(picker, listOf(MediaId("x")))
        nav.awaitIdle()

        assertEquals(listOf(MediaId("x")), again.await().getOrNull())
    }

    // ------------------------------------------------------------------ устойчивость и восстановление

    @Test
    fun `failing middleware does not kill the navigator`() = runBlocking {
        val boom = object : NavigationMiddleware {
            override val order = -1_000
            override suspend fun intercept(
                command: NavigationCommand,
                proceed: suspend (NavigationCommand) -> Unit,
            ) {
                if (command is NavigationCommand.Replace) error("boom")
                proceed(command)
            }
        }
        val nav = navigator(boom)
        sessions.login("ada")

        nav.run(NavigationCommand.Replace(ProfileNavKey("1")))
        nav.run(NavigationCommand.Forward(ProfileNavKey("2")))

        assertEquals(listOf(FeedNavKey, ProfileNavKey("2")), nav.backStack)
    }

    @Test
    fun `switching preserves both histories and reselecting keeps only the root`() = runBlocking {
        sessions.login("ada")
        val nav = navigator()
        val profile = ProfileNavKey("1")
        val details = RequestDetailsNavKey("REQ-101")

        nav.run(NavigationCommand.Forward(profile))
        nav.run(NavigationCommand.SelectSection(RequestsNavKey))
        nav.run(NavigationCommand.Forward(details))
        nav.run(NavigationCommand.SelectSection(FeedNavKey))
        assertEquals(listOf(FeedNavKey, profile), nav.backStack)
        nav.run(NavigationCommand.SelectSection(RequestsNavKey))
        assertEquals(listOf(RequestsNavKey, details), nav.backStack)

        nav.run(NavigationCommand.SelectSection(RequestsNavKey))
        assertEquals(listOf(RequestsNavKey), nav.backStack)
        assertEquals(listOf(FeedNavKey, profile), nav.backStack(AppSection.Home))
        nav.run(NavigationCommand.SelectSection(FeedNavKey))
        nav.run(NavigationCommand.SelectSection(FeedNavKey))
        assertEquals(listOf(FeedNavKey), nav.backStack)
    }

    @Test
    fun `Back pops request details then goes Home without cycling through tabs`() = runBlocking {
        sessions.login("ada")
        val nav = navigator()
        nav.run(NavigationCommand.SelectSection(RequestsNavKey))
        nav.run(NavigationCommand.Forward(RequestDetailsNavKey("REQ-101")))

        nav.run(NavigationCommand.Back)
        assertEquals(AppSection.Requests, nav.selectedSection)
        assertEquals(listOf(RequestsNavKey), nav.backStack)
        nav.run(NavigationCommand.Back)
        assertEquals(AppSection.Home, nav.selectedSection)
        nav.run(NavigationCommand.Back)
        assertEquals(listOf(FeedNavKey), nav.backStack)
        assertEquals(listOf(RequestsNavKey), nav.backStack(AppSection.Requests))
    }

    @Test
    fun `guest can cancel entering Requests then sign in without leaving Login in either history`() = runBlocking {
        val nav = navigator()
        val auth = AuthViewModel(nav, sessions)
        val select = NavigationCommand.SelectSection(RequestsNavKey)
        nav.run(select)
        val signIn = AuthNavKey(returnTo = RequestsNavKey, selectSection = true)
        assertEquals(AppSection.Home, nav.selectedSection)
        assertEquals(listOf(FeedNavKey, signIn), nav.backStack)
        assertTrue(nav.backStack(AppSection.Requests).isEmpty())

        auth.cancel()
        nav.awaitIdle()
        assertFalse(sessions.isAuthorized)
        assertEquals(listOf(FeedNavKey), nav.backStack)

        nav.run(select)
        auth.signIn("ada", signIn.returnTo, signIn.selectSection)
        nav.awaitIdle()
        assertEquals(AppSection.Requests, nav.selectedSection)
        assertEquals(listOf(RequestsNavKey), nav.backStack)
        assertEquals(listOf(FeedNavKey), nav.backStack(AppSection.Home))
    }

    @Test
    fun `a delayed sign-in continuation does not switch sections after Back`() = runBlocking {
        val nav = navigator()
        nav.run(NavigationCommand.SelectSection(RequestsNavKey))
        nav.run(NavigationCommand.Back)

        AuthViewModel(nav, sessions).signIn("ada", RequestsNavKey, selectSection = true)
        nav.awaitIdle()

        assertEquals(AppSection.Home, nav.selectedSection)
        assertTrue(nav.backStack(AppSection.Requests).isEmpty())
    }

    @Test
    fun `logout clears Requests even when another section is visible`() = runBlocking {
        sessions.login("ada")
        val nav = navigator()
        nav.run(NavigationCommand.SelectSection(RequestsNavKey))
        nav.run(NavigationCommand.Forward(RequestDetailsNavKey("REQ-101")))
        nav.run(NavigationCommand.SelectSection(FeedNavKey))
        nav.run(NavigationCommand.Forward(ProfileNavKey("1")))

        nav.run(NavigationCommand.Logout)
        assertFalse(sessions.isAuthorized)
        assertEquals(AppSection.Home, nav.selectedSection)
        assertEquals(listOf(FeedNavKey), nav.backStack)
        assertTrue(nav.backStack(AppSection.Requests).isEmpty())

        sessions.login("another-user")
        nav.run(NavigationCommand.SelectSection(RequestsNavKey))
        assertEquals(listOf(RequestsNavKey), nav.backStack)
        nav.run(NavigationCommand.Logout)
        assertEquals(AppSection.Home, nav.selectedSection)
        assertTrue(nav.backStack(AppSection.Requests).isEmpty())
    }

    @Test
    fun `cold restoration keeps both stacks and recreating the host uses the live snapshot`() {
        sessions.login("ada")
        val saved = SectionHistory(
            selected = AppSection.Requests,
            home = listOf(FeedNavKey, ProfileNavKey("1")),
            requests = listOf(RequestsNavKey, RequestDetailsNavKey("REQ-101")),
        )
        val nav = navigator()
        val router = nav.attachTestRouter(saved, longArrayOf(30, 1, 7, 9, 20))
        val ids = nav.saveEntryIds().toList()
        nav.detach(router)
        nav.attachTestRouter(saved.copy(selected = AppSection.Home, requests = emptyList()))

        assertEquals(saved, nav.saveHistory())
        assertEquals(listOf(30L, 1L, 7L, 9L, 20L), ids)
        assertEquals(ids, nav.saveEntryIds().toList())
    }

    @Test
    fun `guest restoration keeps the public prefix and never restores the protected section`() {
        val saved = SectionHistory(
            selected = AppSection.Requests,
            home = listOf(FeedNavKey, picker, ProfileNavKey("1"), MediaPickerNavKey(maxCount = 5)),
            requests = listOf(RequestsNavKey, RequestDetailsNavKey("REQ-101")),
        )
        val nav = navigator(saved = saved)

        assertEquals(SectionHistory(AppSection.Home, listOf(FeedNavKey, picker), emptyList()), nav.saveHistory())
    }

    @Test
    fun `equal keys can open separate children and switching retains both owners`() = runBlocking {
        sessions.login("ada")
        val nav = navigator()
        val router = nav.attachTestRouter()
        val profile = ProfileNavKey("1")
        nav.run(NavigationCommand.Forward(profile))
        val first = router.screens.value.displayStack().active.instance.owner
        nav.run(NavigationCommand.Forward(profile))
        val second = router.screens.value.displayStack().active.instance.owner
        assertNotSame(first, second)

        nav.run(NavigationCommand.SelectSection(RequestsNavKey))
        nav.run(NavigationCommand.SelectSection(FeedNavKey))
        assertSame(second, router.screens.value.displayStack().active.instance.owner)
        nav.run(NavigationCommand.Back)
        assertSame(first, router.screens.value.displayStack().active.instance.owner)
    }
}

internal fun NavigatorImpl.attachTestRouter(
    saved: SectionHistory? = null,
    ids: LongArray? = null,
): DecomposeRouter {
    // JVM-тесты работают без Android Looper. Продакшен по-прежнему проверяет главный поток.
    DecomposeSettings.settings = DecomposeSettings.settings.copy(mainThreadCheckEnabled = false)
    return createRouter(DefaultComponentContext(LifecycleRegistry()), saved, ids).also(::attach)
}
