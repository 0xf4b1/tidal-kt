plugins {
    kotlin("jvm") version "1.8.0"
    application
}

group = "com.tiefensuche"
version = "0.2.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.json:json:20230227")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}