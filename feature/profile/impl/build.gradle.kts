plugins {
    id("navplay.android.feature.impl")
}

android {
    namespace = "dev.navplay.feature.profile.impl"
}

dependencies {
    implementation(project(":feature:profile:api"))
    implementation(project(":feature:feed:api"))
    implementation(project(":feature:auth:api"))

    testImplementation(project(":core:navigation-testing"))
}
