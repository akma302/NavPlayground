package dev.navplay.feature.media.impl

import androidx.navigation3.runtime.EntryProviderScope
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dev.navplay.core.navigation.NavEntryContributor
import dev.navplay.core.navigation.NavKey
import dev.navplay.core.navigation.NavKeySerializers
import dev.navplay.core.navigation.navKeys
import dev.navplay.feature.media.api.MediaPickerNavKey
import javax.inject.Inject

class MediaEntryContributor @Inject constructor() : NavEntryContributor {
    override fun register(scope: EntryProviderScope<NavKey>) {
        scope.entry<MediaPickerNavKey> { key -> MediaPickerScreen(key = key) }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class MediaNavigationModule {
    @Binds
    @IntoSet
    abstract fun entries(impl: MediaEntryContributor): NavEntryContributor

    companion object {
        @Provides
        @IntoSet
        fun navKeySerializers(): NavKeySerializers =
            navKeys { subclass(MediaPickerNavKey::class, MediaPickerNavKey.serializer()) }
    }
}
