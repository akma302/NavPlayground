package dev.navplay.feature.media.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import dagger.multibindings.IntoSet
import dev.navplay.core.navigation.NavEntryContributor
import dev.navplay.core.navigation.NavKey
import dev.navplay.core.navigation.NavKeySerializers
import dev.navplay.core.navigation.navKeys
import dev.navplay.feature.media.api.MediaPickerNavKey
import javax.inject.Inject

class MediaEntryContributor @Inject constructor(
    private val factory: ViewModelProvider.Factory,
) : NavEntryContributor {
    override fun register(scope: EntryProviderScope<NavKey>) {
        scope.entry<MediaPickerNavKey> { key -> MediaPickerScreen(key, viewModel(factory = factory)) }
    }
}

@Module
abstract class MediaNavigationModule {
    @Binds
    @IntoMap
    @ClassKey(MediaPickerViewModel::class)
    abstract fun viewModel(impl: MediaPickerViewModel): ViewModel

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
