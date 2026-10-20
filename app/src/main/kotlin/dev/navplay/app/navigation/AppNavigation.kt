package dev.navplay.app.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.navplay.core.navigation.NavEntryContributor
import dev.navplay.core.navigation.NavKey
import dev.navplay.core.navigation.NavigationCommand

/**
 * Композиция: про конкретные фичи `:app` знает только через DI-набор [contributors].
 * Новая фича = подключённый Gradle-модуль; ни одна строка здесь не меняется.
 */
@Composable
fun AppNavigation(
    navigator: NavigatorImpl,
    contributors: Set<NavEntryContributor>,
) {
    val provider = remember(contributors) {
        entryProvider<NavKey>(
            // Ключ пришёл, а фича в этой сборке не подключена: показываем заглушку, а не падаем.
            fallback = { key -> NavEntry(key) { UnknownDestinationScreen(key) { navigator.execute(NavigationCommand.Back) } } },
        ) {
            contributors.forEach { it.register(this) }
        }
    }

    NavDisplay(
        backStack = navigator.backStack,
        onBack = { navigator.execute(NavigationCommand.Back) },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
            // ViewModel привязан к записи бэкстека: hiltViewModel() внутри экрана живёт, пока экран в стеке.
            rememberViewModelStoreNavEntryDecorator<NavKey>(),
        ),
        entryProvider = provider,
    )
}

@Composable
fun UnknownDestinationScreen(key: NavKey, onBack: () -> Unit) {
    PlaceholderScreen(
        title = "Unknown destination",
        text = "No entry is registered for $key. Is the feature module part of this build?",
        onBack = onBack,
    )
}

@Composable
fun PlaceholderScreen(title: String, text: String, onBack: () -> Unit) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Text(text)
            Button(onClick = onBack) { Text("Back") }
        }
    }
}
