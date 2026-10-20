plugins {
    id("navplay.android.feature.impl")
}

android {
    namespace = "dev.navplay.feature.auth.impl"
}

dependencies {
    implementation(project(":feature:auth:api"))
    implementation(project(":feature:feed:api"))

    testImplementation(project(":core:navigation-testing"))
}
