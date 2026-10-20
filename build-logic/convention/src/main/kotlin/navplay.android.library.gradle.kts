// Базовая конфигурация любого Android-library модуля проекта.
//
// AGP 9: Kotlin встроен в AGP, плагин org.jetbrains.kotlin.android НЕ применяется
// (его применение в AGP 9 даёт ошибку). Источники лежат в src/main/kotlin.

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    compileSdk = 37
    defaultConfig {
        minSdk = 30
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    "testImplementation"(libs.lib("junit"))
    "testImplementation"(libs.lib("kotlinx-coroutines-test"))
}
