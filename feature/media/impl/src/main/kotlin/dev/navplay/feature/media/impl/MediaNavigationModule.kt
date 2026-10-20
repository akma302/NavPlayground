package dev.navplay.feature.media.impl

import androidx.navigation3.runtime.EntryProviderScope
import dev.navplay.core.navigation.NavEntryContributor
import dev.navplay.core.navigation.NavKey
import dev.navplay.core.navigation.NavKeySerializers
import dev.navplay.core.navigation.navKeys
import dev.navplay.feature.media.api.MediaPickerNavKey
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

class MediaEntryContributor() : NavEntryContributor {
    override fun register(scope: EntryProviderScope<NavKey>) {
        scope.entry<MediaPickerNavKey> { key -> MediaPickerScreen(key = key) }
    }
}

/** Feature-owned registrations. Qualifiers keep collection contributions distinct. */
object MediaNavigationModule {
    val module: Module = module {
        factory<NavEntryContributor>(named("media.entries")) { MediaEntryContributor() }
        factory<NavKeySerializers>(named("media.serializers")) { navKeySerializers() }
        viewModelOf(::MediaPickerViewModel)
    }

    fun navKeySerializers(): NavKeySerializers =
        navKeys { subclass(MediaPickerNavKey::class, MediaPickerNavKey.serializer()) }
}
