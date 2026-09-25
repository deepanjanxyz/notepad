pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "EliteMemoPro"
include(":app")
include(":core:model", ":core:domain", ":core:database", ":core:data",
        ":core:work", ":core:ui", ":core:designsystem")
include(":feature:notes", ":feature:editor", ":feature:drawing", ":feature:settings")
