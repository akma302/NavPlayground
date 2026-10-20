// :feature:<name>:impl — экраны, ViewModel, DI-регистрация в навигации.
// Видит свой api, чужие api и :core:navigation. Чужие impl — никогда (правило проверяется ревью/графом).

plugins {
    id("navplay.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    buildFeatures {
        compose = true
    }
}

dependencies {
    "implementation"(project(":core:navigation"))

    "implementation"(platform(libs.lib("androidx-compose-bom")))
    "implementation"(libs.lib("androidx-compose-ui"))
    "implementation"(libs.lib("androidx-compose-foundation"))
    "implementation"(libs.lib("androidx-compose-material3"))
    "implementation"(libs.lib("androidx-lifecycle-runtime-compose"))
    "implementation"(libs.lib("androidx-lifecycle-viewmodel-compose"))

    "implementation"(libs.lib("hilt-android"))
    "implementation"(libs.lib("androidx-hilt-lifecycle-viewmodel-compose"))
    "ksp"(libs.lib("hilt-compiler"))
}
