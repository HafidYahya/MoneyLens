pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // For TensorFlow Lite and other Maven artifacts
        maven(url = "https://repo1.maven.org/maven2/")
        maven(url = "https://maven.google.com")
    }
}

rootProject.name = "MoneyLens"
include(":app")
 