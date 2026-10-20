package dev.navplay.core.navigation

import java.net.URLDecoder

/**
 * Каждая фича регистрирует свой обработчик deep link (`@Binds @IntoSet`).
 * Внешняя ссылка, пуш и внутренний переход идут одним путём: `DeepLink` -> resolver -> `Forward` -> те же middleware.
 */
interface DeepLinkHandler {
    /** Например `navplay://profile/{userId}`. */
    val pattern: String

    /**
     * Можно ли открыть этот экран из недоверенного источника (внешняя ссылка, пуш).
     * `false` — только внутренние переходы; DeepLinkValidationMiddleware отклонит внешние.
     */
    val exported: Boolean get() = true

    /** Собирает ключ из аргументов, извлечённых из URI. null — аргументы не подходят. */
    fun build(args: Map<String, String>): NavKey?
}

/** Контракт-тесты фичи: «этот URI превращается в этот NavKey». */
fun DeepLinkHandler.parse(uri: String): NavKey? =
    UriPattern(pattern).match(uri)?.let { args -> build(args) }

/**
 * Шаблон вида `scheme://host/seg/{arg}`. Хост считается первым сегментом пути.
 * Чистый Kotlin (без android.net.Uri), поэтому тестируется на JVM.
 * Аргументы пути перекрывают одноимённые query-параметры. Значения URL-декодируются.
 */
class UriPattern(pattern: String) {
    private val parsed = requireNotNull(ParsedUri.parse(pattern)) { "Invalid deep link pattern: $pattern" }

    fun match(uri: String): Map<String, String>? {
        val target = ParsedUri.parse(uri) ?: return null
        if (!target.scheme.equals(parsed.scheme, ignoreCase = true)) return null
        if (target.segments.size != parsed.segments.size) return null

        val args = LinkedHashMap<String, String>()
        for ((expected, actual) in parsed.segments.zip(target.segments)) {
            if (expected.startsWith("{") && expected.endsWith("}")) {
                args[expected.substring(1, expected.length - 1)] = decode(actual)
            } else if (expected != actual) {
                return null
            }
        }
        for ((name, value) in target.query) {
            args.putIfAbsent(name, decode(value))
        }
        return args
    }

    private fun decode(value: String): String =
        runCatching { URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)
}

private class ParsedUri(
    val scheme: String,
    val segments: List<String>,
    val query: Map<String, String>,
) {
    companion object {
        fun parse(raw: String): ParsedUri? {
            val schemeEnd = raw.indexOf("://")
            if (schemeEnd <= 0) return null
            val scheme = raw.substring(0, schemeEnd)
            val rest = raw.substring(schemeEnd + 3).substringBefore('#')
            val path = rest.substringBefore('?')
            val query = rest.substringAfter('?', missingDelimiterValue = "")
                .split('&')
                .filter { it.isNotEmpty() }
                .associate { it.substringBefore('=') to it.substringAfter('=', missingDelimiterValue = "") }
            val segments = path.split('/').filter { it.isNotEmpty() }
            return ParsedUri(scheme, segments, query)
        }
    }
}

/** Резолвер поверх мультибайндинг-сета обработчиков. Первый подошедший обработчик побеждает. */
class DeepLinkResolver(handlers: Collection<DeepLinkHandler>) {
    class Match(val handler: DeepLinkHandler, val key: NavKey)

    private val entries = handlers.map { it to UriPattern(it.pattern) }

    fun match(uri: String): Match? {
        for ((handler, pattern) in entries) {
            val args = pattern.match(uri) ?: continue
            val key = handler.build(args) ?: continue
            return Match(handler, key)
        }
        return null
    }

    fun resolve(uri: String): NavKey? = match(uri)?.key
}
