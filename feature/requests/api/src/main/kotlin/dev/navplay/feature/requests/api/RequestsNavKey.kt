package dev.navplay.feature.requests.api

import dev.navplay.core.navigation.NavKey
import dev.navplay.core.navigation.RequiresAuth
import kotlinx.serialization.Serializable

/** Защищён и корень: гостя останавливаем до показа списка и создания истории раздела. */
@Serializable
data object RequestsNavKey : NavKey, RequiresAuth

/** ID позволяет восстановить экран без сериализации данных заявки. */
@Serializable
data class RequestDetailsNavKey(val id: String) : NavKey, RequiresAuth
