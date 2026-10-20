plugins {
    `kotlin-dsl`
}

dependencies {
    // implementation (а не compileOnly): precompiled script plugins применяют эти плагины по id.
    implementation(libs.android.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.kotlin.composeGradlePlugin)
    implementation(libs.kotlin.serializationGradlePlugin)
    implementation(libs.metro.gradlePlugin)
}
