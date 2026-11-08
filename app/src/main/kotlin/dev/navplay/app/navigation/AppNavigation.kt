package dev.navplay.app.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.navplay.core.navigation.NavEntryContributor
import dev.navplay.core.navigation.NavKey
import dev.navplay.core.navigation.NavigationCommand
import dev.navplay.feature.auth.api.AuthNavKey

/**
 * Shell знает корни разделов, а содержимое экранов получает через [contributors].
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

    // Обе истории остаются декорированными даже за пределами видимого раздела.
    // Если декорировать только видимый список, смена вкладки будет выглядеть как pop,
    // и Navigation 3 удалит saveable state и ViewModel экранов скрытого раздела.
    val homeEntries = sectionEntries(navigator.backStack(AppSection.Home), provider)
    val requestsProvider = remember(provider) {
        { key: NavKey ->
            val entry = provider(key)
            // Одна и та же цель (например, внешний Profile) может появиться в обоих стеках.
            NavEntry(key, contentKey = "requests:${entry.contentKey}", metadata = entry.metadata) {
                entry.Content()
            }
        }
    }
    val requestEntries = sectionEntries(navigator.backStack(AppSection.Requests), requestsProvider)
    val selected = navigator.selectedSection

    Scaffold(
        topBar = {
            // Login временно занимает текущий стек. Во время входа вкладки не запускают
            // ещё один выбор раздела поверх уже ожидающего продолжения.
            if (navigator.backStack.lastOrNull() !is AuthNavKey) {
                PrimaryTabRow(
                    selectedTabIndex = selected.ordinal,
                    modifier = Modifier.statusBarsPadding(),
                ) {
                    AppSection.entries.forEach { section ->
                        Tab(
                            selected = selected == section,
                            onClick = { navigator.execute(NavigationCommand.SelectSection(section.root)) },
                            text = { Text(section.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        // Home служит предыдущей сценой для Back с корня Requests. На Home не добавляем
        // историю Requests: иначе с корня Home системный Back не смог бы выйти из приложения.
        NavDisplay(
            entries = if (selected == AppSection.Home) homeEntries else homeEntries + requestEntries,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
            onBack = { navigator.execute(NavigationCommand.Back) },
        )
    }
}

@Composable
private fun sectionEntries(
    stack: List<NavKey>,
    provider: (NavKey) -> NavEntry<NavKey>,
): List<NavEntry<NavKey>> = rememberDecoratedNavEntries(
    backStack = stack,
    entryDecorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
        rememberViewModelStoreNavEntryDecorator<NavKey>(),
    ),
    entryProvider = provider,
)

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
