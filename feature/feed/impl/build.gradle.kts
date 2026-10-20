plugins {
    id("navplay.android.feature.impl")
}

android {
    namespace = "dev.navplay.feature.feed.impl"
}

dependencies {
    implementation(project(":feature:feed:api"))
    implementation(project(":feature:auth:api"))
    implementation(project(":feature:profile:api"))
    implementation(project(":feature:media:api"))

    testImplementation(project(":core:navigation-testing"))
}
