package dev.navplay.feature.feed.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FeedScreen(viewModel: FeedViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text("Navigation playground", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Signed in as ${state.userName ?: "nobody"}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            item { Section("Auth guard + Replace/BackTo/NewRoot") }
            item {
                Button(onClick = { viewModel.openProfile("1") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Open profile #1 (RequiresAuth)")
                }
            }
            item {
                OutlinedButton(onClick = { viewModel.openSignIn() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Open sign-in directly")
                }
            }
            item {
                OutlinedButton(
                    onClick = { viewModel.logout() },
                    enabled = state.userName != null,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Log out (session only)") }
            }

            item { Section("Throttling") }
            item {
                OutlinedButton(onClick = { viewModel.openProfileDoubleTap("1") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Double tap: 2 identical commands")
                }
            }

            item { Section("Typed result + feature flag") }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("media_picker flag", modifier = Modifier.weight(1f))
                    Switch(
                        checked = state.mediaPickerEnabled,
                        onCheckedChange = viewModel::setMediaPickerEnabled,
                    )
                }
            }
            item {
                Button(onClick = { viewModel.pickMedia() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Pick media (forResult)")
                }
            }
            item {
                val text = when (val result = state.pickResult) {
                    PickResult.None -> "no result yet"
                    is PickResult.Picked -> "picked: ${result.ids.joinToString { it.value }}"
                    is PickResult.Failed -> "failed: ${result.reason}"
                }
                Text(text, style = MaterialTheme.typography.bodyMedium)
            }

            item { Section("Deep links (same pipeline)") }
            items(FeedViewModel.demoLinks) { demo ->
                OutlinedButton(onClick = { viewModel.openDeepLink(demo) }, modifier = Modifier.fillMaxWidth()) {
                    Text(demo.label)
                }
            }

            item { Section("Navigation log (middleware at work)") }
            item {
                OutlinedButton(onClick = { viewModel.clearLog() }) { Text("Clear log") }
            }
            items(state.log.takeLast(30).asReversed()) { entry ->
                Text(
                    "[${entry.source}] ${entry.message}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

@Composable
private fun Section(title: String) {
    HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    Text(title, style = MaterialTheme.typography.titleMedium)
}
