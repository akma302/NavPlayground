package dev.navplay.core.navigation

import androidx.navigation3.runtime.EntryProviderScope

/**
 * Фича сама вносит свои экраны в граф: реализация лежит в `:feature:x:impl` и подключается DI-мультибайндингом
 * (`@Binds @IntoSet`). `:app` собирает все вклады в один `entryProvider` и ничего не знает о конкретных фичах.
 *
 * Добавление фичи = подключение Gradle-модуля. Ни одна строка в навигации не меняется (Open/Closed).
 */
fun interface NavEntryContributor {
    fun register(scope: EntryProviderScope<NavKey>)
}
