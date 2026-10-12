plugins {
    application
}

dependencies {
    implementation(project(":core"))
    // Same serialiser and version as the device and the server, so the byte counts are the real wire sizes.
    implementation("tools.jackson.core:jackson-databind:3.1.5")

    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("com.wildfiresync.experiments.SyncCostExperiment")
}
