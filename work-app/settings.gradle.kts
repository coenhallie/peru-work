pluginManagement {
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
        // Mapbox Maven repository
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            credentials {
                username = "mapbox"
                // Set your Mapbox access token with DOWNLOADS:READ scope
                password = providers.gradleProperty("MAPBOX_DOWNLOADS_TOKEN").orNull
                    ?: System.getenv("MAPBOX_DOWNLOADS_TOKEN")
                    ?: "YOUR_MAPBOX_ACCESS_TOKEN"
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}

rootProject.name = "WorkApp"
include(":app")