plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}
rootProject.name = "exampleShard"

include(":aetherium")
project(":aetherium").projectDir = File("../../")