package dev.navplay.feature.media.impl

import androidx.lifecycle.ViewModel
import dev.navplay.core.navigation.NavigationCommand
import dev.navplay.core.navigation.Navigator
import dev.navplay.feature.media.api.MediaId
import dev.navplay.feature.media.api.MediaPickerNavKey

/** Чистая логика выбора — отдельно от UI, чтобы тестировать без Compose. */
object MediaSelection {
    /** Добавляет/убирает [id]. Больше [maxCount] выбрать нельзя: лишний тап игнорируется. */
    fun toggle(current: List<String>, id: String, maxCount: Int): List<String> = when {
        id in current -> current - id
        current.size >= maxCount -> current
        else -> current + id
    }
}

class MediaPickerViewModel(
    private val navigator: Navigator,
) : ViewModel() {

    /** Игрушечная «галерея»: в реальном приложении тут репозиторий. */
    val gallery: List<String> = (1..12).map { "IMG_%03d".format(it) }

    /** Отдаёт результат вызывающему и закрывает экран. Тип результата проверяет компилятор: ключ — `ResultKey<List<MediaId>>`. */
    fun confirm(key: MediaPickerNavKey, selected: List<String>) {
        navigator.finishWithResult(key, selected.map { MediaId(it) })
    }

    /** Закрыть без результата: вызывающий получит `NavigationResultException.Cancelled`. */
    fun cancel() {
        navigator.execute(NavigationCommand.Back)
    }
}
