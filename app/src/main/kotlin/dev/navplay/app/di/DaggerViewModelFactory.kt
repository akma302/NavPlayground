package dev.navplay.app.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import dagger.Binds
import dagger.Module
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.reflect.KClass

/**
 * Dagger собирает способы создания ViewModel из модулей фич. Карта Provider откладывает создание
 * каждой модели до запроса её ViewModelStore. Сам store принадлежит записи Navigation 3:
 * два профиля используют общую сессию, но получают разные экземпляры ProfileViewModel.
 */
@Singleton
class DaggerViewModelFactory @Inject constructor(
    private val creators: Map<Class<*>, @JvmSuppressWildcards Provider<ViewModel>>,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
        val creator = requireNotNull(creators[modelClass.java]) {
            "No Dagger ViewModel binding for ${modelClass.qualifiedName}"
        }
        return modelClass.java.cast(creator.get())
    }
}

@Module
interface ViewModelBindingModule {
    @Binds
    fun factory(impl: DaggerViewModelFactory): ViewModelProvider.Factory
}
