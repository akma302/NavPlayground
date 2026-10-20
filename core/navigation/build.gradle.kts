plugins {
    id("navplay.android.library")
}

android {
    namespace = "dev.navplay.core.navigation"
}

dependencies {
    // EntryProviderScope (для NavEntryContributor) приходит из navigation3-runtime.
    api(libs.androidx.navigation3.runtime)
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.coroutines.core)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)
}
