plugins {
    java
    id("com.android.application") version "9.4.1" apply false
    // AGP 9+ has built-in Kotlin support, org.jetbrains.kotlin.android is no
    // longer needed (and is rejected). The Compose compiler plugin is still
    // required separately.
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20" apply false
}

allprojects {
    group = "com.wildfiresync"
    version = "0.0.1-SNAPSHOT"

    repositories {
        google()
        mavenCentral()
    }
}

subprojects {
    // The device module is set up by the Android Gradle Plugin instead, which
    // manages its own Java/Kotlin compilation and conflicts with the plain
    // java plugin applied here.
    if (name != "device") {
        apply(plugin = "java")

        java {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(21))
            }
        }

        tasks.withType<Test> {
            useJUnitPlatform()
        }
    }
}