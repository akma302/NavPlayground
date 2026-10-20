package dev.navplay.feature.profile.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileScreen(
    userId: String,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val nextId = ((userId.toIntOrNull() ?: 0) + 1).toString()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Profile #$userId", style = MaterialTheme.typography.headlineMedium)
            Text("Signed in as ${session?.userName ?: "nobody (session ended)"}")

            Button(onClick = { viewModel.openProfile(nextId) }) {
                Text("Forward -> profile #$nextId")
            }
            OutlinedButton(onClick = { viewModel.replaceWith("replaced-$userId") }) {
                Text("Replace this screen")
            }
            OutlinedButton(onClick = { viewModel.backToFeed() }) {
                Text("BackTo(Feed)")
            }
            OutlinedButton(onClick = { viewModel.logout() }) {
                Text("Log out -> NewRoot(Feed)")
            }
        }
    }
}
