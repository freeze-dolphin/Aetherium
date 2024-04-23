plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.23"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    compileOnly(project(":aetherium"))

    compileOnly("com.github.freeze-dolphin:aff-compose:5c46cf1187")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes(
            "Aetherium-Entry" to "org.example.aetheriumshard.ExampleShard",
        )
    }
}

kotlin {
    jvmToolchain(17)
}