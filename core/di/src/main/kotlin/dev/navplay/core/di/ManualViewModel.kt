package dev.navplay.core.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel

val LocalManualViewModelFactory = staticCompositionLocalOf<ViewModelProvider.Factory> {
    error("Provide the application ViewModel factory at the Compose root")
}

/** Uses the ViewModelStoreOwner attached to the current navigation entry. */
@Composable
inline fun <reified VM : ViewModel> manualViewModel(): VM {
    val owner = checkNotNull(LocalViewModelStoreOwner.current)
    var model by remember(owner) { mutableStateOf<VM?>(null) }
    // При pop Decompose уже очистил store, а выходящая сцена ещё может рекомпозироваться.
    // Удерживаем прежнюю модель до конца анимации, чтобы фабрика не создала новую в удалённой записи.
    if (model == null) model = viewModel(viewModelStoreOwner = owner, factory = LocalManualViewModelFactory.current)
    return checkNotNull(model)
}
