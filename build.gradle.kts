plugins {
    kotlin("jvm") version "1.8.0"
    application
    `maven-publish`
}

group = "com.tiefensuche"
version = "0.3.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.json:json:20250107")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}

publishing {
    publications {
        register<MavenPublication>("gpr") {
            from(components["java"])
        }
    }
}
