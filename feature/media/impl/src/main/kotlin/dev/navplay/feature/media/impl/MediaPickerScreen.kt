package dev.navplay.feature.media.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.navplay.feature.media.api.MediaPickerNavKey
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
fun MediaPickerScreen(
    key: MediaPickerNavKey,
    viewModel: MediaPickerViewModel = metroViewModel(),
) {
    var selected by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Pick media", style = MaterialTheme.typography.headlineMedium)
            Text("Selected ${selected.size} of ${key.maxCount}")

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                items(viewModel.gallery) { id ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = MediaSelection.toggle(selected, id, key.maxCount) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = id in selected, onCheckedChange = null)
                        Text(id, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }

            Button(
                onClick = { viewModel.confirm(key, selected) },
                enabled = selected.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Done") }
            OutlinedButton(onClick = { viewModel.cancel() }, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel (back without result)")
            }
        }
    }
}
