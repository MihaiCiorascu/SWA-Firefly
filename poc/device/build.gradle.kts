plugins {
    kotlin("jvm") version "2.4.20"
    application
}

dependencies {
    implementation(project(":core"))
    implementation("org.xerial:sqlite-jdbc:3.53.4.0")
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.wildfiresync.device.MainKt")
}
