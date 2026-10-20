package dev.navplay.feature.profile.impl

import androidx.navigation3.runtime.EntryProviderScope
import dev.navplay.core.navigation.DeepLinkHandler
import dev.navplay.core.navigation.NavEntryContributor
import dev.navplay.core.navigation.NavKey
import dev.navplay.core.navigation.NavKeySerializers
import dev.navplay.core.navigation.navKeys
import dev.navplay.feature.profile.api.ProfileNavKey
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

class ProfileEntryContributor() : NavEntryContributor {
    override fun register(scope: EntryProviderScope<NavKey>) {
        scope.entry<ProfileNavKey> { key -> ProfileScreen(userId = key.userId) }
    }
}

/** Feature-owned registrations. Qualifiers keep collection contributions distinct. */
object ProfileNavigationModule {
    val module: Module = module {
        factory<NavEntryContributor>(named("profile.entries")) { ProfileEntryContributor() }
        factory<NavKeySerializers>(named("profile.serializers")) { navKeySerializers() }
        factory<DeepLinkHandler>(named("profile.deepLink")) { ProfileDeepLinkHandler() }
        viewModelOf(::ProfileViewModel)
    }

    fun navKeySerializers(): NavKeySerializers =
        navKeys { subclass(ProfileNavKey::class, ProfileNavKey.serializer()) }
}
