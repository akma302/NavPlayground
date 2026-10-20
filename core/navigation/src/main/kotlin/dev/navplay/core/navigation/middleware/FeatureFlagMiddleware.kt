package dev.navplay.core.navigation.middleware

import dev.navplay.core.navigation.FeatureFlags
import dev.navplay.core.navigation.FeatureGated
import dev.navplay.core.navigation.NavigationCommand
import dev.navplay.core.navigation.NavigationLog
import dev.navplay.core.navigation.NavigationMiddleware
import dev.navplay.core.navigation.StubNavKey
import dev.navplay.core.navigation.targetKey
import dev.navplay.core.navigation.withTargetKey

/** Если фича выключена тоглом — команда заменяется на переход к экрану-заглушке. */
class FeatureFlagMiddleware(
    private val flags: FeatureFlags,
    private val log: NavigationLog,
) : NavigationMiddleware {
    override val order = NavigationMiddleware.Order.FEATURE_FLAGS

    override suspend fun intercept(
        command: NavigationCommand,
        proceed: suspend (NavigationCommand) -> Unit,
    ) {
        val key = command.targetKey
        if (key is FeatureGated && !flags.isEnabled(key.featureId)) {
            log.log("flags", "feature '${key.featureId}' is disabled -> stub")
            proceed(command.withTargetKey(StubNavKey(key.featureId, "disabled by feature flag")))
        } else {
            proceed(command)
        }
    }
}
