plugins {
    kotlin("jvm") version "2.4.20"
    application
}

dependencies {
    implementation(project(":core"))
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.wildfiresync.device.MainKt")
}
