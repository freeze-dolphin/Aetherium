import java.io.ByteArrayOutputStream

val affComposeVersion: String = "11ab9d8e92"
val kotlinVersion: String by project
val ktorVersion: String by project
val ktomlVersion: String by project
val kamlVersion: String by project
val logbackVersion: String by project
val xmlutilVersion: String by project

plugins {
    kotlin("jvm") version "2.0.0-RC1"
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
    implementation("com.charleskorn.kaml:kaml:${kamlVersion}")
    implementation("com.github.ajalt.clikt:clikt:5.0.3")
    testImplementation("io.ktor:ktor-server-tests-jvm")

    implementation("org.reflections:reflections:0.10.2")

    implementation("com.github.freeze-dolphin:aff-compose:${affComposeVersion}")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

fun getGitCommitHash(): String {
    val stdout = ByteArrayOutputStream()
    exec {
        commandLine("git", "rev-parse", "--short", "HEAD") // 获取短格式的提交哈希
        standardOutput = stdout
    }
    return stdout.toString().trim()
}

application {
    mainClass.set("io.sn.aetherium.GenesisKt")

    val isDevelopment = true
    applicationDefaultJvmArgs =
        listOf("-Dio.ktor.development=$isDevelopment")
}

distributions.main {
    distributionBaseName = "Aetherium-${getGitCommitHash()}"
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