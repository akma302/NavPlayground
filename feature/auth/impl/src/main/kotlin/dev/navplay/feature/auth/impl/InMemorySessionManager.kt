package dev.navplay.feature.auth.impl

import dev.navplay.feature.auth.api.Session
import dev.navplay.feature.auth.api.SessionManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Игрушечная сессия в памяти — в реальном приложении тут токены и репозиторий. */
@Singleton
class InMemorySessionManager @Inject constructor() : SessionManager {
    private val _session = MutableStateFlow<Session?>(null)
    override val session: StateFlow<Session?> = _session.asStateFlow()

    override fun login(userName: String) {
        _session.value = Session(userName)
    }

    override fun logout() {
        _session.value = null
    }
}
