package dev.navplay.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import dev.navplay.app.navigation.AppNavigation
import dev.navplay.app.navigation.AppSection
import dev.navplay.app.navigation.SectionHistory
import dev.navplay.app.navigation.NavigatorImpl
import dev.navplay.core.navigation.BackStackCodec
import dev.navplay.core.navigation.DeepLinkSource
import dev.navplay.core.navigation.NavEntryContributor
import dev.navplay.core.navigation.NavigationCommand
import dev.navplay.core.navigation.NavigationLog
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val navigator: NavigatorImpl by inject()

    private val contributors by lazy { getKoin().getAll<NavEntryContributor>().toSet() }

    private val codec: BackStackCodec by inject()

    private val log: NavigationLog by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        navigator.initialize(
            savedInstanceState?.let { state ->
                SectionHistory(
                    selected = if (state.getBoolean(STATE_REQUESTS_SELECTED)) AppSection.Requests else AppSection.Home,
                    // Старый Bundle с одним стеком всё ещё подходит для Home.
                    home = state.getString(STATE_BACKSTACK)?.let(codec::decodeOrNull).orEmpty(),
                    requests = state.getString(STATE_REQUESTS)?.let(codec::decodeOrNull).orEmpty(),
                )
            },
        )

        // Холодный старт по ссылке. При пересоздании активити ссылку повторно не обрабатываем.
        if (savedInstanceState == null) handleDeepLink(intent)

        setContent {
            MaterialTheme {
                AppNavigation(navigator = navigator, contributors = contributors)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
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
    }
}
