package dev.navplay.feature.media.impl

import dev.navplay.core.navigation.NavEntryContributor
import dev.navplay.core.navigation.ScreenRegistry
import dev.navplay.core.navigation.NavKeySerializers
import dev.navplay.core.navigation.navKeys
import dev.navplay.feature.media.api.MediaPickerNavKey

class MediaEntryContributor() : NavEntryContributor {
    override fun register(scope: ScreenRegistry) {
        scope.entry(MediaPickerNavKey::class) { key -> MediaPickerScreen(key = key) }
    }
}

/** Feature-owned serialization; AppContainer explicitly registers the feature. */
object MediaNavigationModule {
    fun navKeySerializers(): NavKeySerializers =
        navKeys { subclass(MediaPickerNavKey::class, MediaPickerNavKey.serializer()) }
}
