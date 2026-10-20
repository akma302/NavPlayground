package dev.navplay.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import dev.navplay.app.navigation.AppNavigation
import dev.navplay.app.navigation.NavigatorImpl
import dev.navplay.core.navigation.BackStackCodec
import dev.navplay.core.navigation.DeepLinkSource
import dev.navplay.core.navigation.NavEntryContributor
import dev.navplay.core.navigation.NavigationCommand
import dev.navplay.core.navigation.NavigationLog
import dev.navplay.feature.feed.api.FeedNavKey
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val navigator: NavigatorImpl by inject()

    private val contributors by lazy { getKoin().getAll<NavEntryContributor>().toSet() }

    private val codec: BackStackCodec by inject()

    private val log: NavigationLog by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Поворот экрана: Singleton-навигатор жив, стек не пуст — восстанавливать нечего.
        // Смерть процесса: навигатор новый и пустой — поднимаем стек из Bundle (все NavKey сериализуемы).
        if (savedInstanceState != null && navigator.backStack.isEmpty()) {
            savedInstanceState.getString(STATE_BACKSTACK)
                ?.let(codec::decodeOrNull)
                ?.let(navigator::restore)
        }
        navigator.ensureRoot(FeedNavKey)

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
            outState.putString(STATE_BACKSTACK, codec.encode(navigator.backStack.toList()))
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
    }
}
