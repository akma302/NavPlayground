package dev.navplay.feature.requests.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class Request(val id: String, val title: String, val status: String)

// Одни данные для списка и карточки. Сценарий проверяет сохранение места работы,
// поэтому экрану пока не нужны репозиторий, загрузка или собственная ViewModel.
private val requests = listOf(
    Request("REQ-101", "VPN access", "In review"),
    Request("REQ-102", "Replace laptop", "Approved"),
    Request("REQ-103", "Project workspace", "Draft"),
)

@Composable
internal fun RequestsScreen(onOpen: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Requests", style = MaterialTheme.typography.headlineMedium)
        requests.forEach { request ->
            OutlinedButton(
                onClick = { onOpen(request.id) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("${request.id} · ${request.title}")
            }
        }
    }
}

@Composable
internal fun RequestDetailsScreen(id: String, onBack: () -> Unit) {
    val request = requests.firstOrNull { it.id == id }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(id, style = MaterialTheme.typography.headlineMedium)
        if (request == null) {
            Text("Request unavailable")
        } else {
            Text(request.title, style = MaterialTheme.typography.titleLarge)
            Text(request.status)
        }
        TextButton(onClick = onBack) { Text("Back to requests") }
    }
}
