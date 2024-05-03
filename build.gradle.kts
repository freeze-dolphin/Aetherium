val kotlinVersion: String by project
val affComposeVersion: String by project
val ktorVersion: String by project
val ktomlVersion: String by project
val logbackVersion: String by project
val xmlutilVersion: String by project

plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.23"
    id("io.ktor.plugin") version "2.3.10"

    java
    application
    `maven-publish`
}

group = "io.sn"

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation(kotlin("reflect"))
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("ch.qos.logback:logback-classic:${logbackVersion}")
    implementation("io.ktor:ktor-serialization-kotlinx-json:${ktorVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.akuleshov7:ktoml-core:${ktomlVersion}")
    implementation("com.akuleshov7:ktoml-file:${ktomlVersion}")
    testImplementation("io.ktor:ktor-server-tests-jvm")

    implementation("org.reflections:reflections:0.10.2")

    implementation("com.github.freeze-dolphin:aff-compose:${affComposeVersion}")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

application {
    mainClass.set("io.sn.aetherium.GenesisKt")

    val isDevelopment = true
    applicationDefaultJvmArgs =
        listOf("-Dio.ktor.development=$isDevelopment")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            version = project.version.toString()
            groupId = project.group.toString()
            artifactId = "aetherium"
        }
    }
}