plugins {
    kotlin("jvm") version "1.9.0"
    application
}

group = "me.alex_s168"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

dependencies {
    testImplementation(kotlin("test"))

    implementation(kotlin("reflect"))

    implementation("com.github.SuperCraftAlex:ktlib:339f73db75")
    implementation("com.github.ajalt.mordant:mordant:2.1.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("MainKt")
}