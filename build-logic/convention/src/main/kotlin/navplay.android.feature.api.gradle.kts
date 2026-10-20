// :feature:<name>:api — публичный контракт фичи: только сериализуемые NavKey и маленькие интерфейсы.
// Зависит строго от :core:navigation.

plugins {
    id("navplay.android.library")
}

dependencies {
    "api"(project(":core:navigation"))
}
