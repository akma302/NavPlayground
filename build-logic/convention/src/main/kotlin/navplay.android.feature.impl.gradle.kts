// :feature:<name>:impl — экраны, ViewModel, DI-регистрация в навигации.
// Видит свой api, чужие api и :core:navigation. Чужие impl — никогда (правило проверяется ревью/графом).

plugins {
    id("navplay.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    buildFeatures {
        compose = true
    }
}

dependencies {
    "implementation"(libs.lib("koin-android"))
    "implementation"(libs.lib("koin-compose-viewmodel"))
    "implementation"(project(":core:navigation"))

    "implementation"(platform(libs.lib("androidx-compose-bom")))
    "implementation"(libs.lib("androidx-compose-ui"))
    "implementation"(libs.lib("androidx-compose-foundation"))
    "implementation"(libs.lib("androidx-compose-material3"))
    "implementation"(libs.lib("androidx-lifecycle-runtime-compose"))
    "implementation"(libs.lib("androidx-lifecycle-viewmodel-compose"))

}
