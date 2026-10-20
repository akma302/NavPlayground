plugins {
    id("navplay.android.compose.library")
}

android {
    namespace = "dev.navplay.core.di"
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.runtime)
    api(libs.androidx.lifecycle.viewmodel.compose)
}
