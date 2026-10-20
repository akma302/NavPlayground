package dev.navplay.core.navigation

import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/**
 * Вклад фичи в полиморфную сериализацию [NavKey].
 *
 * `NavKey` — открытый интерфейс (sealed через модули не растянешь), поэтому kotlinx.serialization
 * не знает его наследников, пока их не зарегистрируют. Без этого «сериализуемый бэкстек» падает
 * при первой же записи. Каждая фича регистрирует свои ключи через `@Provides @IntoSet`.
 */
class NavKeySerializers(val module: SerializersModule)

/** `navKeys { subclass(ProfileNavKey::class, ProfileNavKey.serializer()) }` */
fun navKeys(builder: PolymorphicModuleBuilder<NavKey>.() -> Unit): NavKeySerializers =
    NavKeySerializers(SerializersModule { polymorphic(NavKey::class, builderAction = builder) })

/** Кодирует бэкстек в строку и обратно. Строку кладут в `onSaveInstanceState`. */
class BackStackCodec(contributions: Collection<NavKeySerializers>) {
    private val json = Json {
        serializersModule = SerializersModule {
            contributions.forEach { include(it.module) }
        }
    }

    private val serializer: KSerializer<List<NavKey>> = ListSerializer(PolymorphicSerializer(NavKey::class))

    fun encode(keys: List<NavKey>): String = json.encodeToString(serializer, keys)

    fun decode(raw: String): List<NavKey> = json.decodeFromString(serializer, raw)

    /** Не бросает: повреждённое состояние или ключ удалённой фичи -> null, приложение стартует с нуля. */
    fun decodeOrNull(raw: String): List<NavKey>? = runCatching { decode(raw) }.getOrNull()
}
