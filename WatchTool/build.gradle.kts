plugins {
    kotlin("jvm") version "2.2.10"
    kotlin("plugin.serialization") version "2.2.10"
    application
}

group = "com.github.rain1208"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // YouTube Data API
    implementation("com.google.apis:google-api-services-youtube:v3-rev20240916-2.0.0")
    implementation("com.google.api-client:google-api-client:2.7.0")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.36.0")
    implementation("com.google.http-client:google-http-client-jackson2:1.45.0")

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Kotlinx Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    // Command line parsing
    implementation("com.github.ajalt.clikt:clikt:4.4.0")

    // Logging
    implementation("org.slf4j:slf4j-simple:2.0.16")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.github.rain1208.MainKt")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.github.raink1208.watchtool.MainKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
