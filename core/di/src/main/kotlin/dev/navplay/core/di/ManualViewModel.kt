package dev.navplay.core.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel

val LocalManualViewModelFactory = staticCompositionLocalOf<ViewModelProvider.Factory> {
    error("Provide the application ViewModel factory at the Compose root")
}

/** Uses the current NavEntry's ViewModelStoreOwner supplied by Navigation 3. */
@Composable
inline fun <reified VM : ViewModel> manualViewModel(): VM =
    viewModel(factory = LocalManualViewModelFactory.current)
