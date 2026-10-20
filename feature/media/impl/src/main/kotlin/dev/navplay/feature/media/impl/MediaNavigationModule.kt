package dev.navplay.feature.media.impl

import androidx.navigation3.runtime.EntryProviderScope
import dev.navplay.core.navigation.NavEntryContributor
import dev.navplay.core.navigation.NavKey
import dev.navplay.core.navigation.NavKeySerializers
import dev.navplay.core.navigation.navKeys
import dev.navplay.feature.media.api.MediaPickerNavKey

class MediaEntryContributor() : NavEntryContributor {
    override fun register(scope: EntryProviderScope<NavKey>) {
        scope.entry<MediaPickerNavKey> { key -> MediaPickerScreen(key = key) }
    }
}

/** Feature-owned serialization; AppContainer explicitly registers the feature. */
object MediaNavigationModule {
    fun navKeySerializers(): NavKeySerializers =
        navKeys { subclass(MediaPickerNavKey::class, MediaPickerNavKey.serializer()) }
}
