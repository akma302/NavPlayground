plugins {
    id("navplay.android.library")
}

android {
    namespace = "dev.navplay.core.navigation.testing"
}

// Тестовые фейки навигации. Подключается только как testImplementation.
dependencies {
    api(project(":core:navigation"))
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
}
