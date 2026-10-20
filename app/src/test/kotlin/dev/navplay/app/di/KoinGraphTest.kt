package dev.navplay.app.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation3.runtime.entryProvider
import dev.navplay.app.navigation.NavigationScope
import dev.navplay.app.navigation.NavigatorImpl
import dev.navplay.core.navigation.BackStackCodec
import dev.navplay.core.navigation.DeepLinkHandler
import dev.navplay.core.navigation.DeepLinkResolver
import dev.navplay.core.navigation.FeatureFlags
import dev.navplay.core.navigation.MutableFeatureFlags
import dev.navplay.core.navigation.NavEntryContributor
import dev.navplay.core.navigation.NavKey
import dev.navplay.core.navigation.NavKeySerializers
import dev.navplay.core.navigation.NavigationAnalytics
import dev.navplay.core.navigation.NavigationCommand
import dev.navplay.core.navigation.NavigationLog
import dev.navplay.core.navigation.NavigationMiddleware
import dev.navplay.core.navigation.Navigator
import dev.navplay.core.navigation.StubNavKey
import dev.navplay.core.navigation.testing.MainDispatcherRule
import dev.navplay.feature.auth.api.AuthNavKey
import dev.navplay.feature.auth.api.SessionManager
import dev.navplay.feature.auth.impl.AuthViewModel
import dev.navplay.feature.feed.api.FeedNavKey
import dev.navplay.feature.feed.impl.FeedViewModel
import dev.navplay.feature.media.api.MediaPickerNavKey
import dev.navplay.feature.media.impl.MediaPickerViewModel
import dev.navplay.feature.profile.api.ProfileNavKey
import dev.navplay.feature.profile.impl.ProfileViewModel
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineScope
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
import org.koin.core.KoinApplication
import org.koin.core.annotation.KoinInternalApi
import org.koin.dsl.koinApplication
import org.koin.viewmodel.resolveViewModel

/** Resolve the same modules that Application loads, with strict duplicate detection. */
class KoinGraphTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private lateinit var application: KoinApplication
    private val koin get() = application.koin
    private val stores = mutableListOf<ViewModelStore>()

    @Before fun setUp() {
        application = newApplication()
    }

    @After fun tearDown() {
        stores.forEach(ViewModelStore::clear)
        application.close()
    }

    private fun newApplication() = koinApplication {
        allowOverride(false)
        modules(appModules)
    }

    @Test fun `aliases and application services share single instances`() {
        assertSame(koin.get<NavigatorImpl>(), koin.get<Navigator>())
        assertSame(koin.get<MutableFeatureFlags>(), koin.get<FeatureFlags>())
        assertSame(koin.get<SessionManager>(), koin.get<SessionManager>())
        assertSame(koin.get<NavigationLog>(), koin.get<NavigationLog>())
        assertSame(koin.get<NavigationAnalytics>(), koin.get<NavigationAnalytics>())
        assertSame(koin.get<DeepLinkResolver>(), koin.get<DeepLinkResolver>())
        assertSame(koin.get<BackStackCodec>(), koin.get<BackStackCodec>())
        assertSame(koin.get<CoroutineScope>(NavigationScope), koin.get<CoroutineScope>(NavigationScope))
        val firstThrottle = koin.getAll<NavigationMiddleware>().single {
            it.order == NavigationMiddleware.Order.THROTTLE
        }
        val secondThrottle = koin.getAll<NavigationMiddleware>().single {
            it.order == NavigationMiddleware.Order.THROTTLE
        }
        assertSame(firstThrottle, secondThrottle)
    }

    @Test fun `qualified contributions include every feature without overrides`() {
        val contributors = koin.getAll<NavEntryContributor>()
        assertEquals(5, contributors.size)
        assertEquals(7, koin.getAll<NavigationMiddleware>().size)
        assertEquals(3, koin.getAll<DeepLinkHandler>().size)
        assertEquals(5, koin.getAll<NavKeySerializers>().size)
        val keys = listOf(
            FeedNavKey, AuthNavKey(returnTo = ProfileNavKey("42")),
            ProfileNavKey("42"), MediaPickerNavKey(maxCount = 3),
            StubNavKey(feature = "media_picker", reason = "disabled"),
        )
        val provider = entryProvider<NavKey>(fallback = { error("Missing entry: $it") }) {
            contributors.forEach { it.register(this) }
        }
        keys.forEach { assertNotNull(provider(it)) }
        val codec = koin.get<BackStackCodec>()
        assertEquals(keys, codec.decode(codec.encode(keys)))
    }

    @Test fun `viewmodel factory preserves stores and shares the session and navigator`() {
        val firstStore = newStore()
        val profile = viewModel(ProfileViewModel::class, firstStore)
        assertNotNull(viewModel(AuthViewModel::class, firstStore))
        assertNotNull(viewModel(FeedViewModel::class, firstStore))
        assertNotNull(viewModel(MediaPickerViewModel::class, firstStore))
        assertSame(profile, viewModel(ProfileViewModel::class, firstStore))
        val secondProfile = viewModel(ProfileViewModel::class, newStore())
        assertNotSame(profile, secondProfile)
        firstStore.clear()
        assertNotSame(profile, viewModel(ProfileViewModel::class, firstStore))

        // The independent store still observes the application-level session.
        val secondAuth = viewModel(AuthViewModel::class, newStore())
        secondAuth.signIn("koin-user", null)
        assertEquals("koin-user", koin.get<SessionManager>().session.value?.userName)
        assertEquals("koin-user", secondProfile.session.value?.userName)
        assertEquals(FeedNavKey, koin.get<NavigatorImpl>().backStack.last())
    }

    @Test fun `real middleware uses the same session and flags as viewmodels`() = runTest {
        val navigator = koin.get<NavigatorImpl>()
        navigator.ensureRoot(FeedNavKey)
        val target = ProfileNavKey("42")
        navigator.execute(NavigationCommand.Forward(target))
        navigator.awaitIdle()
        assertEquals(AuthNavKey(returnTo = target), navigator.backStack.last())
        viewModel(AuthViewModel::class, newStore()).signIn("tester", target)
        navigator.awaitIdle()
        assertEquals(target, navigator.backStack.last())

        koin.get<MutableFeatureFlags>().setEnabled(MediaPickerNavKey.FEATURE_ID, false)
        navigator.execute(NavigationCommand.Forward(MediaPickerNavKey(maxCount = 3)))
        navigator.awaitIdle()
        assertTrue(navigator.backStack.last() is StubNavKey)
    }

    @Test fun `closing container cancels navigation job and new container has fresh singles`() {
        val navigator = koin.get<NavigatorImpl>()
        val sessions = koin.get<SessionManager>()
        val scope = koin.get<CoroutineScope>(NavigationScope)
        application.close()
        assertTrue(scope.coroutineContext[Job]!!.isCancelled)
        application = newApplication()
        assertNotSame(navigator, koin.get<NavigatorImpl>())
        assertNotSame(sessions, koin.get<SessionManager>())
    }

    private fun newStore() = ViewModelStore().also(stores::add)

    @OptIn(KoinInternalApi::class)
    private fun <T : ViewModel> viewModel(type: KClass<T>, store: ViewModelStore): T =
        resolveViewModel(
            vmClass = type,
            viewModelStore = store,
            extras = CreationExtras.Empty,
            scope = koin.scopeRegistry.rootScope,
        )
}
