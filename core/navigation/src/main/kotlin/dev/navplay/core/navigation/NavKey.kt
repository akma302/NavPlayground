package dev.navplay.core.navigation

/**
 * Маркер пункта назначения. Реализации — `@Serializable` data class / data object в `:feature:*:api`.
 *
 * Сериализуемость — инвариант архитектуры: бэкстек (а значит, и ключи) должен переживать смерть процесса.
 * Поэтому в ключ кладём только идентификаторы и примитивы, а не объекты.
 */
interface NavKey

/** Маркер: экран доступен только авторизованному пользователю. Обрабатывается auth-middleware. */
interface RequiresAuth

/** Маркер: экран спрятан за фича-флагом [featureId]. Обрабатывается FeatureFlagMiddleware. */
interface FeatureGated {
    val featureId: String
}

/**
 * Маркер: экран возвращает вызывающему значение типа [T].
 * Вызывающий: `navigator.forResult(key)`; экран-результат: `navigator.finishWithResult(key, value)`.
 *
 * Тип результата зашит в тип ключа, поэтому `Result<T>` у вызывающего и `value: T` у экрана
 * проверяются компилятором — никаких строковых ключей и кастов в фичах.
 */
interface ResultKey<T : Any> : NavKey
