plugins {
    kotlin("jvm") version "1.9.0"
}

group = "me.alex_s168"
version = "0.2"

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

dependencies {
    testImplementation(kotlin("test"))

    implementation(kotlin("reflect"))

    implementation("com.github.SuperCraftAlex:ktlib:4a380bf749")
    implementation("com.github.ajalt.mordant:mordant:2.1.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}