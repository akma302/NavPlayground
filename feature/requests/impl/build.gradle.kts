plugins {
    id("navplay.android.feature.impl")
}

android {
    namespace = "dev.navplay.feature.requests.impl"
}

dependencies {
    implementation(project(":feature:requests:api"))
}
