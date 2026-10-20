// :app — composition root. Единственный модуль, который видит все impl.

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("dev.zacsweers.metro")
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
    buildFeatures {
        compose = true
    }
}

dependencies {
    "implementation"(project(":core:di"))
    "implementation"(libs.lib("metrox-viewmodel-compose"))
    "implementation"(platform(libs.lib("androidx-compose-bom")))
    "implementation"(libs.lib("androidx-compose-ui"))
    "implementation"(libs.lib("androidx-compose-foundation"))
    "implementation"(libs.lib("androidx-compose-material3"))
    "implementation"(libs.lib("androidx-activity-compose"))
    "implementation"(libs.lib("androidx-lifecycle-runtime-compose"))


    "testImplementation"(libs.lib("junit"))
    "testImplementation"(libs.lib("kotlinx-coroutines-test"))
}
