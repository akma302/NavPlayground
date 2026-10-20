package dev.navplay.app.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation3.runtime.entryProvider
import dev.navplay.core.navigation.NavKey
import dev.navplay.core.navigation.NavigationCommand
import dev.navplay.core.navigation.StubNavKey
import dev.navplay.core.navigation.testing.MainDispatcherRule
import dev.navplay.feature.auth.api.AuthNavKey
import dev.navplay.feature.auth.impl.AuthViewModel
import dev.navplay.feature.feed.api.FeedNavKey
import dev.navplay.feature.feed.impl.FeedViewModel
import dev.navplay.feature.media.api.MediaPickerNavKey
import dev.navplay.feature.media.impl.MediaPickerViewModel
import dev.navplay.feature.profile.api.ProfileNavKey
import dev.navplay.feature.profile.impl.ProfileViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Exercise the production composition root, including its ViewModel factory. */
class ManualGraphTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private lateinit var container: AppContainer
    private val stores = mutableListOf<ViewModelStore>()

    @Before fun setUp() {
        container = AppContainer()
    }

    @After fun tearDown() {
        stores.forEach(ViewModelStore::clear)
        container.close()
    }

    @Test fun `container registers every navigation contribution`() {
        assertEquals(5, container.contributors.size)
        assertEquals(7, container.middleware.size)
        assertEquals(3, container.deepLinkHandlers.size)
        assertEquals(5, container.serializers.size)
        val keys = listOf(
            FeedNavKey, AuthNavKey(returnTo = ProfileNavKey("42")),
            ProfileNavKey("42"), MediaPickerNavKey(maxCount = 3),
            StubNavKey(feature = "media_picker", reason = "disabled"),
        )
        val provider = entryProvider<NavKey>(fallback = { error("Missing entry: $it") }) {
            container.contributors.forEach { it.register(this) }
        }
        keys.forEach { assertNotNull(provider(it)) }
        assertEquals(keys, container.codec.decode(container.codec.encode(keys)))
    }

    @Test fun `factory creates all viewmodels with entry lifetime and shared application services`() {
        val store = newStore()
        val first = provider(store)
        val profile = first[ProfileViewModel::class]
        assertNotNull(first[FeedViewModel::class])
        assertNotNull(first[MediaPickerViewModel::class])
        assertSame(profile, first[ProfileViewModel::class])
        val other = provider(newStore())
        val otherProfile = other[ProfileViewModel::class]
        assertNotSame(profile, otherProfile)
        store.clear()
        assertNotSame(profile, first[ProfileViewModel::class])

        other[AuthViewModel::class].signIn("manual-user", null)
        assertEquals("manual-user", container.sessions.session.value?.userName)
        assertEquals("manual-user", otherProfile.session.value?.userName)
        assertEquals(FeedNavKey, container.navigator.backStack.last())
    }

    @Test fun `auth and feature flag middleware use the same services as viewmodels`() = runTest {
        val navigator = container.navigator
        navigator.ensureRoot(FeedNavKey)
        val target = ProfileNavKey("42")
        navigator.execute(NavigationCommand.Forward(target))
        navigator.awaitIdle()
        assertEquals(AuthNavKey(returnTo = target), navigator.backStack.last())
        provider(newStore())[AuthViewModel::class].signIn("tester", target)
        navigator.awaitIdle()
        assertEquals(target, navigator.backStack.last())
        container.flags.setEnabled(MediaPickerNavKey.FEATURE_ID, false)
        navigator.execute(NavigationCommand.Forward(MediaPickerNavKey(maxCount = 3)))
        navigator.awaitIdle()
        assertTrue(navigator.backStack.last() is StubNavKey)
    }

    @Test fun `closing container cancels worker and a new container owns fresh instances`() {
        val previous = container
        previous.close()
        assertTrue(previous.navigationScope.coroutineContext[Job]!!.isCancelled)
        container = AppContainer()
        assertNotSame(previous.navigator, container.navigator)
        assertNotSame(previous.sessions, container.sessions)
        assertNotSame(previous.flags, container.flags)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unknown viewmodel fails with an explicit missing registration`() {
        container.viewModelFactory.create(UnregisteredViewModel::class, CreationExtras.Empty)
    }

    private fun newStore() = ViewModelStore().also(stores::add)

    private fun provider(store: ViewModelStore): ViewModelProvider =
        ViewModelProvider.create(store, container.viewModelFactory, CreationExtras.Empty)
}

private class UnregisteredViewModel : ViewModel()
