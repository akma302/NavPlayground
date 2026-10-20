package dev.navplay.app.di

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation3.runtime.entryProvider
import dev.navplay.app.navigation.NavigationScope
import dev.navplay.app.navigation.NavigatorImpl
import dev.navplay.core.di.AppScope
import dev.navplay.core.navigation.BackStackCodec
import dev.navplay.core.navigation.DeepLinkHandler
import dev.navplay.core.navigation.FeatureFlags
import dev.navplay.core.navigation.MutableFeatureFlags
import dev.navplay.core.navigation.NavEntryContributor
import dev.navplay.core.navigation.NavKey
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
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.createGraph
import dev.zacsweers.metrox.viewmodel.ViewModelGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Same production contributions, with extra accessors to verify identity and clean up jobs. */
@DependencyGraph(AppScope::class)
interface InspectableAppGraph : ViewModelGraph {
    val navigator: NavigatorImpl
    val contributors: Set<NavEntryContributor>
    val codec: BackStackCodec
    val log: NavigationLog
    val navigatorApi: Navigator
    val sessions: SessionManager
    val flags: FeatureFlags
    val mutableFlags: MutableFeatureFlags
    val middleware: Set<NavigationMiddleware>
    val deepLinks: Set<DeepLinkHandler>
    @NavigationScope val navigationScope: CoroutineScope
}

class MetroGraphTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private lateinit var graph: InspectableAppGraph
    private val stores = mutableListOf<ViewModelStore>()

    @Before fun setUp() {
        graph = createGraph<InspectableAppGraph>()
    }

    @After fun tearDown() {
        stores.forEach(ViewModelStore::clear)
        graph.navigationScope.cancel()
    }

    @Test fun `aliases share application instances`() {
        assertSame(graph.navigator, graph.navigatorApi)
        assertSame(graph.navigator, graph.navigator)
        assertSame(graph.sessions, graph.sessions)
        assertSame(graph.flags, graph.mutableFlags)
        assertSame(graph.codec, graph.codec)
        assertSame(graph.log, graph.log)
    }

    @Test fun `generated graph aggregates every feature contribution`() {
        assertEquals(5, graph.contributors.size)
        assertEquals(7, graph.middleware.size)
        assertEquals(3, graph.deepLinks.size)
        val keys = listOf(
            FeedNavKey, AuthNavKey(returnTo = ProfileNavKey("42")),
            ProfileNavKey("42"), MediaPickerNavKey(maxCount = 3),
            StubNavKey(feature = "media_picker", reason = "disabled"),
        )
        val provider = entryProvider<NavKey>(fallback = { error("Missing entry: $it") }) {
            graph.contributors.forEach { it.register(this) }
        }
        keys.forEach { assertNotNull(provider(it)) }
        assertEquals(keys, graph.codec.decode(graph.codec.encode(keys)))
    }

    @Test fun `factory creates all viewmodels and store owns their lifetime`() {
        fun provider(): ViewModelProvider {
            val store = ViewModelStore().also(stores::add)
            return ViewModelProvider.create(store, graph.metroViewModelFactory, CreationExtras.Empty)
        }
        val first = provider()
        val auth = first[AuthViewModel::class]
        val profile = first[ProfileViewModel::class]
        assertNotNull(first[FeedViewModel::class])
        assertNotNull(first[MediaPickerViewModel::class])
        assertSame(profile, first[ProfileViewModel::class])
        val secondProfile = provider()[ProfileViewModel::class]
        assertNotSame(profile, secondProfile)

        auth.signIn("metro-user", null)
        assertEquals("metro-user", graph.sessions.session.value?.userName)
        assertEquals("metro-user", profile.session.value?.userName)
        assertEquals("metro-user", secondProfile.session.value?.userName)
        assertEquals(FeedNavKey, graph.navigator.backStack.last())
    }
}
