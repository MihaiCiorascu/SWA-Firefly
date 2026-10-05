plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":core"))
    implementation("org.xerial:sqlite-jdbc:3.53.4.0")
    implementation("io.nats:jnats:2.21.1")
    implementation("tools.jackson.core:jackson-databind:3.1.5")
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.wildfiresync.device.MainKt")
}
