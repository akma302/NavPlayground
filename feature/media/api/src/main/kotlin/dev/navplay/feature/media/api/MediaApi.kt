package dev.navplay.feature.media.api

import dev.navplay.core.navigation.FeatureGated
import dev.navplay.core.navigation.ResultKey
import kotlinx.serialization.Serializable

@JvmInline
value class MediaId(val value: String)

/**
 * Выбор медиа. Типизированный результат: вызывающий получает `Result<List<MediaId>>`
 * через `navigator.forResult(MediaPickerNavKey(...))`. Спрятан за фича-флагом [FEATURE_ID].
 */
@Serializable
data class MediaPickerNavKey(val maxCount: Int) : ResultKey<List<MediaId>>, FeatureGated {
    override val featureId: String get() = FEATURE_ID

    companion object {
        const val FEATURE_ID = "media_picker"
    }
}
