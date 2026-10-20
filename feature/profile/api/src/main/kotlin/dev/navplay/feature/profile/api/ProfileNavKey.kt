package dev.navplay.feature.profile.api

import dev.navplay.core.navigation.NavKey
import dev.navplay.core.navigation.RequiresAuth
import kotlinx.serialization.Serializable

/** Профиль пользователя. Передаём только id — данные экран добирает сам. Требует авторизации. */
@Serializable
data class ProfileNavKey(val userId: String) : NavKey, RequiresAuth
