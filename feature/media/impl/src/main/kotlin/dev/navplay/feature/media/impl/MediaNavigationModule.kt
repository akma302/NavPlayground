package dev.navplay.feature.media.impl

import androidx.navigation3.runtime.EntryProviderScope
import dev.navplay.core.di.AppScope
import dev.navplay.core.navigation.NavEntryContributor
import dev.navplay.core.navigation.NavKey
import dev.navplay.core.navigation.NavKeySerializers
import dev.navplay.core.navigation.navKeys
import dev.navplay.feature.media.api.MediaPickerNavKey
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides

@Inject
class MediaEntryContributor() : NavEntryContributor {
    override fun register(scope: EntryProviderScope<NavKey>) {
        scope.entry<MediaPickerNavKey> { key -> MediaPickerScreen(key = key) }
    }
}

@BindingContainer
@ContributesTo(AppScope::class)
abstract class MediaNavigationModule {
    @Binds
    @IntoSet
    abstract val MediaEntryContributor.entries: NavEntryContributor

    companion object {
        @Provides
        @IntoSet
        fun navKeySerializers(): NavKeySerializers =
            navKeys { subclass(MediaPickerNavKey::class, MediaPickerNavKey.serializer()) }
    }
}
