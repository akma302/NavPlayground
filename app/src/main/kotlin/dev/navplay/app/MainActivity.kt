package dev.navplay.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import com.arkivanov.decompose.retainedComponent
import dev.navplay.core.di.LocalManualViewModelFactory
import dev.navplay.app.navigation.AppNavigation
import dev.navplay.app.navigation.AppSection
import dev.navplay.app.navigation.DecomposeRouter
import dev.navplay.app.navigation.SectionHistory
import dev.navplay.core.navigation.DeepLinkSource
import dev.navplay.core.navigation.NavigationCommand

class MainActivity : ComponentActivity() {

    private val container get() = (application as PlaygroundApp).appContainer
    private val navigator get() = container.navigator
    private val codec get() = container.codec
    private val log get() = container.log
    private lateinit var router: DecomposeRouter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Компонент переживает поворот. Фабрика не сохраняет Activity в дочерних компонентах.
        router = retainedComponent { context ->
            navigator.createRouter(
                context = context,
                saved = savedInstanceState?.let { state ->
                    SectionHistory(
                        selected = if (state.getBoolean(STATE_REQUESTS_SELECTED)) AppSection.Requests else AppSection.Home,
                        home = state.getString(STATE_BACKSTACK)?.let(codec::decodeOrNull).orEmpty(),
                        requests = state.getString(STATE_REQUESTS)?.let(codec::decodeOrNull).orEmpty(),
                    )
                },
                entryIds = savedInstanceState?.getLongArray(STATE_ENTRY_IDS),
            )
        }
        navigator.attach(router)

        // Холодный старт по ссылке. При пересоздании активити ссылку повторно не обрабатываем.
        if (savedInstanceState == null) handleDeepLink(intent)

        setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalManualViewModelFactory provides container.viewModelFactory) {
                    AppNavigation(navigator = navigator, router = router, contributors = container.contributors)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    override fun onDestroy() {
        if (::router.isInitialized) navigator.detach(router)
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        try {
            val history = navigator.saveHistory()
            // Кодируем обе истории до записи, чтобы ошибка сериализации не оставила полуснимок.
            val home = codec.encode(history.home)
            val requests = codec.encode(history.requests)
            outState.putString(STATE_BACKSTACK, home)
            outState.putString(STATE_REQUESTS, requests)
            outState.putBoolean(STATE_REQUESTS_SELECTED, history.selected == AppSection.Requests)
            outState.putLongArray(STATE_ENTRY_IDS, navigator.saveEntryIds())
        } catch (e: Exception) {
            // Ключ без зарегистрированного сериализатора не должен ронять приложение при сворачивании.
            log.log("nav", "backstack was not saved: $e")
        }
    }

    /** Внешняя ссылка — недоверенный источник: идёт через ту же цепочку, включая валидацию. */
    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data?.toString() ?: return
        navigator.execute(NavigationCommand.DeepLink(uri, DeepLinkSource.External))
    }

    private companion object {
        const val STATE_BACKSTACK = "navplay.backstack"
        const val STATE_REQUESTS = "navplay.requests.backstack"
        const val STATE_REQUESTS_SELECTED = "navplay.requests.selected"
        const val STATE_ENTRY_IDS = "navplay.decompose.entry.ids"
    }
}
