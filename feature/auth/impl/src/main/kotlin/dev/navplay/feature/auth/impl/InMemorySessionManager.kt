package dev.navplay.feature.auth.impl

import dev.navplay.core.di.AppScope
import dev.navplay.feature.auth.api.Session
import dev.navplay.feature.auth.api.SessionManager
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Игрушечная сессия в памяти — в реальном приложении тут токены и репозиторий. */
@SingleIn(AppScope::class)
@Inject
class InMemorySessionManager() : SessionManager {
    private val _session = MutableStateFlow<Session?>(null)
    override val session: StateFlow<Session?> = _session.asStateFlow()

    override fun login(userName: String) {
        _session.value = Session(userName)
    }

    override fun logout() {
        _session.value = null
    }
}
