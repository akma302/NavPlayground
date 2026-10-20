package dev.navplay.core.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import kotlin.reflect.KClass

/** Holds creation functions, never application-scoped ViewModel instances. */
class ManualViewModelFactory(
    private val creators: Map<KClass<out ViewModel>, () -> ViewModel>,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
        val creator = requireNotNull(creators[modelClass]) {
            "No manual ViewModel factory registered for ${modelClass.qualifiedName}"
        }
        @Suppress("UNCHECKED_CAST")
        return creator() as T
    }
}
