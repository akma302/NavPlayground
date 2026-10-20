package dev.navplay.feature.auth.impl

import dev.navplay.core.navigation.DeepLinkHandler
import dev.navplay.core.navigation.NavKey
import dev.navplay.feature.auth.api.AuthNavKey

/** `navplay://auth` — только для внутренних переходов (`exported = false`): внешняя ссылка будет отклонена. */
class AuthDeepLinkHandler() : DeepLinkHandler {
    override val pattern = "navplay://auth"
    override val exported = false

    override fun build(args: Map<String, String>): NavKey = AuthNavKey()
}
