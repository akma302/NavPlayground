plugins {
    id("navplay.android.feature.impl")
}

android {
    namespace = "dev.navplay.feature.media.impl"
}

dependencies {
    implementation(project(":feature:media:api"))

    testImplementation(project(":core:navigation-testing"))
}
