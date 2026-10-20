package dev.navplay.feature.auth.impl

import dev.navplay.core.navigation.DeepLinkHandler
import dev.navplay.core.navigation.NavKey
import dev.navplay.feature.auth.api.AuthNavKey
import dev.zacsweers.metro.Inject

/** `navplay://auth` — только для внутренних переходов (`exported = false`): внешняя ссылка будет отклонена. */
@Inject
class AuthDeepLinkHandler() : DeepLinkHandler {
    override val pattern = "navplay://auth"
    override val exported = false

    override fun build(args: Map<String, String>): NavKey = AuthNavKey()
}
