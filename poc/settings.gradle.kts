pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "wildfire-sync"

include("core", "server", "device", "android")
