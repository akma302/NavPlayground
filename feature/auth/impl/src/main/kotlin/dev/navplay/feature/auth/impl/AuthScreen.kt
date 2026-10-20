package dev.navplay.feature.auth.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.navplay.core.di.manualViewModel
import dev.navplay.core.navigation.NavKey

@Composable
fun AuthScreen(
    returnTo: NavKey?,
    viewModel: AuthViewModel = manualViewModel(),
) {
    var name by rememberSaveable { mutableStateOf("Ada") }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Sign in", style = MaterialTheme.typography.headlineMedium)
            Text(
                if (returnTo != null) {
                    "The auth-guard sent you here because you opened ${returnTo::class.simpleName}. " +
                        "After signing in you land there."
                } else {
                    "Sign in to continue."
                },
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = { viewModel.signIn(name, returnTo) }) { Text("Sign in") }
            TextButton(onClick = { viewModel.cancel() }) { Text("Cancel") }
        }
    }
}
