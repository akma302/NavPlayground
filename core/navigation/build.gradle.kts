plugins {
    id("navplay.android.compose.library")
}

android {
    namespace = "dev.navplay.core.navigation"
}

dependencies {
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.coroutines.core)

    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.runtime)
}
