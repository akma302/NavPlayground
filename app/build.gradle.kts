plugins {
    id("navplay.android.application")
}

android {
    namespace = "dev.navplay.app"

    defaultConfig {
        applicationId = "dev.navplay"
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false // playground: правила R8 для Hilt/serialization не пишем
        }
    }
}

dependencies {
    implementation(project(":core:navigation"))

    // :app — единственный модуль, который видит все impl (composition root).
    implementation(project(":feature:auth:impl"))
    implementation(project(":feature:feed:impl"))
    implementation(project(":feature:profile:impl"))
    implementation(project(":feature:media:impl"))

    // Из api-модулей нужен только стартовый ключ.
    implementation(project(":feature:feed:api"))

    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.kotlinx.coroutines.android)

    // Для тестов NavigatorImpl и графа навигации: настоящие ключи всех фич.
    testImplementation(project(":core:navigation-testing"))
    testImplementation(project(":feature:auth:api"))
    testImplementation(project(":feature:profile:api"))
    testImplementation(project(":feature:media:api"))
}
