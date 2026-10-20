package dev.navplay.feature.auth.api

import dev.navplay.core.navigation.NavKey
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

/**
 * Экран входа. [returnTo] — куда пользователь хотел попасть, пока его не завернул auth-guard.
 * Поле интерфейсного типа сериализуется полиморфно, поэтому ключ целевого экрана тоже должен быть
 * зарегистрирован в `NavKeySerializers` своей фичи.
 */
@Serializable
data class AuthNavKey(val returnTo: NavKey? = null) : NavKey

data class Session(val userName: String)

/** Публичный контракт сессии: чужие фичи читают состояние, а реализация живёт в `:feature:auth:impl`. */
interface SessionManager {
    val session: StateFlow<Session?>

    val isAuthorized: Boolean get() = session.value != null

    fun login(userName: String)

    fun logout()
}
