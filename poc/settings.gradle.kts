pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    // Lets Gradle download a matching JDK automatically when the toolchain a
    // module asks for (Java 21, for core/server/device) isn't installed
    // locally, instead of failing with "toolchain download repositories have
    // not been configured". No-op on a machine that already has a matching
    // JDK, so this is safe for everyone on the team regardless of what they
    // already have installed.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "wildfire-sync"

include("core", "server", "device", "android")
