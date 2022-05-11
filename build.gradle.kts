plugins {
    alias(libs.plugins.blossom)
    alias(libs.plugins.shadowJar)
    id("java")
}

group = "com.github.klainstom"
version = "1.0-SNAPSHOT"

subprojects {
    group = parent!!.group
    version = parent!!.version

    repositories {
        mavenCentral()
        maven("https://nexus.velocitypowered.com/repository/maven-public/")
        maven("https://repo.viaversion.com/everything/")
        maven("https://jitpack.io")
    }
}
