package dev.navplay.feature.profile.impl

import dev.navplay.core.navigation.DeepLinkHandler
import dev.navplay.core.navigation.NavKey
import dev.navplay.feature.profile.api.ProfileNavKey
import javax.inject.Inject

/** `navplay://profile/{userId}` — тот же путь, что и у внутренней навигации, плюс auth-guard бесплатно. */
class ProfileDeepLinkHandler @Inject constructor() : DeepLinkHandler {
    override val pattern = "navplay://profile/{userId}"

    override fun build(args: Map<String, String>): NavKey? =
        args["userId"]?.takeIf { it.isNotBlank() }?.let { ProfileNavKey(it) }
}
