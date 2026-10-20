// :feature:<name>:impl — экраны, ViewModel, DI-регистрация в навигации.
// Видит свой api, чужие api, :core:navigation и :core:di. Чужие impl — никогда.

plugins {
    id("navplay.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
    id("dev.zacsweers.metro")
}

android {
    buildFeatures {
        compose = true
    }
}

dependencies {
    "implementation"(project(":core:di"))
    "implementation"(libs.lib("metrox-viewmodel-compose"))
    "implementation"(project(":core:navigation"))

    "implementation"(platform(libs.lib("androidx-compose-bom")))
    "implementation"(libs.lib("androidx-compose-ui"))
    "implementation"(libs.lib("androidx-compose-foundation"))
    "implementation"(libs.lib("androidx-compose-material3"))
    "implementation"(libs.lib("androidx-lifecycle-runtime-compose"))
    "implementation"(libs.lib("androidx-lifecycle-viewmodel-compose"))
}
