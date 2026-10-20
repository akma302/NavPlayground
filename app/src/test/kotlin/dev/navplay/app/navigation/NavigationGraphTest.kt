package dev.navplay.app.navigation

import androidx.navigation3.runtime.entryProvider
import dev.navplay.app.di.AppNavigationModule
import dev.navplay.core.navigation.BackStackCodec
import dev.navplay.core.navigation.NavEntryContributor
import dev.navplay.core.navigation.NavKey
import dev.navplay.core.navigation.StubNavKey
import dev.navplay.core.navigation.testing.FakeNavigator
import dev.navplay.feature.auth.api.AuthNavKey
import dev.navplay.feature.auth.impl.AuthEntryContributor
import dev.navplay.feature.auth.impl.AuthNavigationModule
import dev.navplay.feature.feed.api.FeedNavKey
import dev.navplay.feature.feed.impl.FeedEntryContributor
import dev.navplay.feature.feed.impl.FeedNavigationModule
import dev.navplay.feature.media.api.MediaPickerNavKey
import dev.navplay.feature.media.impl.MediaEntryContributor
import dev.navplay.feature.media.impl.MediaNavigationModule
import dev.navplay.feature.profile.api.ProfileNavKey
import dev.navplay.feature.profile.impl.ProfileEntryContributor
import dev.navplay.feature.profile.impl.ProfileNavigationModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Лечит главную слабость DI-мультибайндингов — неявность: если вклад не зарегистрировался,
 * без этих тестов узнаешь об этом только в рантайме.
 */
class NavigationGraphTest {
    /** Ручная проверка контрактов. Реальная DI-сборка проверяется в MetroGraphTest. */
    private val contributors: List<NavEntryContributor> = listOf(
        AuthEntryContributor(),
        FeedEntryContributor(),
        ProfileEntryContributor(),
        MediaEntryContributor(),
        StubEntryContributor(FakeNavigator()),
    )

    /** По одному образцу каждого NavKey, который есть в приложении. */
    private val sampleKeys: List<NavKey> = listOf(
        FeedNavKey,
        AuthNavKey(),
        AuthNavKey(returnTo = ProfileNavKey("1")),
        ProfileNavKey("1"),
        MediaPickerNavKey(maxCount = 3),
        StubNavKey(feature = "media_picker", reason = "disabled"),
    )

    @Test
    fun `every NavKey has an entry`() {
        val provider = entryProvider<NavKey>(
            fallback = { key -> error("No entry registered for $key") },
        ) {
            contributors.forEach { it.register(this) }
        }

        sampleKeys.forEach { key -> assertNotNull(provider(key)) }
    }

    @Test
    fun `every NavKey survives process death`() {
        val codec = BackStackCodec(
            listOf(
                AuthNavigationModule.navKeySerializers(),
                FeedNavigationModule.navKeySerializers(),
                ProfileNavigationModule.navKeySerializers(),
                MediaNavigationModule.navKeySerializers(),
                AppNavigationModule.stubNavKeySerializers(),
            ),
        )

        assertEquals(sampleKeys, codec.decode(codec.encode(sampleKeys)))
    }
}
