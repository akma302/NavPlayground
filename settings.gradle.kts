pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "NavPlayground"

include(":app")
include(":core:navigation")
include(":core:navigation-testing")

include(":feature:auth:api", ":feature:auth:impl")
include(":feature:feed:api", ":feature:feed:impl")
include(":feature:profile:api", ":feature:profile:impl")
include(":feature:media:api", ":feature:media:impl")
