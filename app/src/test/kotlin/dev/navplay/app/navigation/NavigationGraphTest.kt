package dev.navplay.app.navigation

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation3.runtime.entryProvider
import dev.navplay.app.di.DaggerAppComponent
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
import dev.navplay.feature.requests.api.RequestDetailsNavKey
import dev.navplay.feature.requests.api.RequestsNavKey
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test

/**
 * Проверяем сгенерированный граф приложения. Dagger умеет собрать неполный набор вкладов:
 * компилятор видит тип Set, а какие маршруты обязан покрыть этот набор, знает приложение.
 */
class NavigationGraphTest {
    @get:Rule val main = MainDispatcherRule()

    private val component = DaggerAppComponent.create()
    private val stores = mutableListOf<ViewModelStore>()

    @After
    fun tearDown() {
        stores.forEach(ViewModelStore::clear)
        component.navigationScope.cancel()
    }

    /** По одному образцу каждого NavKey, который есть в приложении. */
    private val sampleKeys: List<NavKey> = listOf(
        FeedNavKey,
        AuthNavKey(),
        AuthNavKey(returnTo = ProfileNavKey("1")),
        AuthNavKey(returnTo = RequestsNavKey, selectSection = true),
        ProfileNavKey("1"),
        RequestsNavKey,
        RequestDetailsNavKey("REQ-101"),
        MediaPickerNavKey(maxCount = 3),
        StubNavKey(feature = "media_picker", reason = "disabled"),
    )

    @Test
    fun `every NavKey has an entry`() {
        assertEquals(6, component.contributors.size)
        val provider = entryProvider<NavKey>(
            fallback = { key -> error("No entry registered for $key") },
        ) {
            component.contributors.forEach { it.register(this) }
        }

        sampleKeys.forEach { key -> assertNotNull(provider(key)) }
    }

    @Test
    fun `every NavKey survives process death`() {
        val codec = component.codec
        assertEquals(sampleKeys, codec.decode(codec.encode(sampleKeys)))
    }

    @Test
    fun `Dagger supplies all ViewModels and each store owns its instance`() {
        val store = ViewModelStore().also(stores::add)
        val provider = provider(store)
        assertNotNull(provider[AuthViewModel::class])
        assertNotNull(provider[FeedViewModel::class])
        assertNotNull(provider[MediaPickerViewModel::class])

        val profile = provider[ProfileViewModel::class]
        assertSame(profile, provider[ProfileViewModel::class])
        assertNotSame(profile, provider()[ProfileViewModel::class])
        store.clear()
        assertNotSame(profile, provider[ProfileViewModel::class])
    }

    @Test
    fun `AuthViewModel guard and navigator share the same app dependencies`() = runTest {
        val navigator = component.navigator
        navigator.initialize()
        navigator.execute(NavigationCommand.SelectSection(RequestsNavKey))
        navigator.awaitIdle()
        val login = AuthNavKey(returnTo = RequestsNavKey, selectSection = true)
        assertEquals(login, navigator.backStack.last())

        provider()[AuthViewModel::class].signIn("Ada", login.returnTo, login.selectSection)
        navigator.awaitIdle()
        assertEquals(AppSection.Requests, navigator.selectedSection)
        assertEquals(listOf(RequestsNavKey), navigator.backStack)
        assertEquals("Ada", provider()[ProfileViewModel::class].session.value?.userName)
    }

    private fun provider(store: ViewModelStore = ViewModelStore().also(stores::add)): ViewModelProvider =
        ViewModelProvider.create(store, component.viewModelFactory, CreationExtras.Empty)
}
