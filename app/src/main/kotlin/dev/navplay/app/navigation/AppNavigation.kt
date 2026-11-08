package dev.navplay.app.navigation

import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimatable
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import dev.navplay.core.navigation.NavEntryContributor
import dev.navplay.core.navigation.NavKey
import dev.navplay.core.navigation.NavigationCommand
import dev.navplay.core.navigation.ScreenRegistry
import dev.navplay.feature.auth.api.AuthNavKey

/**
 * Shell знает корни разделов, а содержимое экранов получает через [contributors].
 */
@Composable
@OptIn(ExperimentalDecomposeApi::class)
internal fun AppNavigation(
    navigator: NavigatorImpl,
    router: DecomposeRouter,
    contributors: Set<NavEntryContributor>,
) {
    val registry = remember(contributors) { ScreenRegistry(contributors) }
    val screens by router.screens.subscribeAsState()
    val selected = screens.state.selected
    val holder = rememberSaveableStateHolder()
    // Видимый стек служит для переходов и Back. Состояние Compose удерживается по обеим
    // историям, иначе смена вкладки выглядела бы как удаление всех её экранов.
    val previousIds = remember { mutableSetOf<Long>() }
    val ids = screens.children.mapTo(mutableSetOf()) { it.configuration.id }
    SideEffect {
        (previousIds - ids).forEach(holder::removeState)
        previousIds.clear()
        previousIds.addAll(ids)
    }
    val animation = remember(router, navigator) {
        predictiveBackAnimation<RouteEntry, ScreenComponent>(
            backHandler = router.backHandler,
            // Сохраняем 700 мс crossfade из исходного NavDisplay и уменьшение уходящего экрана до 70%.
            fallbackAnimation = stackAnimation(fade(animationSpec = tween(700))),
            selector = { event, _, _ ->
                predictiveBackAnimatable(
                    initialBackEvent = event,
                    exitModifier = { progress, _ ->
                        Modifier.graphicsLayer {
                            scaleX = 1f - 0.3f * progress
                            scaleY = scaleX
                        }
                    },
                    enterModifier = { progress, _ -> Modifier.graphicsLayer { alpha = progress } },
                )
            },
            onBack = { navigator.execute(NavigationCommand.Back) },
        )
    }

    Scaffold(
        topBar = {
            // Login временно занимает текущий стек. Во время входа вкладки не запускают
            // ещё один выбор раздела поверх уже ожидающего продолжения.
            if (screens.state.active.key !is AuthNavKey) {
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
        animation(
            stack = screens.displayStack(),
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) { child ->
            val entry = child.configuration
            holder.SaveableStateProvider(entry.id) {
                CompositionLocalProvider(LocalViewModelStoreOwner provides child.instance.owner) {
                    val screen = registry[entry.key]
                    if (screen != null) {
                        screen(entry.key)
                    } else {
                        UnknownDestinationScreen(entry.key) { navigator.execute(NavigationCommand.Back) }
                    }
                }
            }
        }
    }
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
