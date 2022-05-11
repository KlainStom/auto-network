plugins {
    alias(libs.plugins.blossom)
    alias(libs.plugins.shadowJar)
    id("java")
}

val displayName = "AutoNetwork"

dependencies {
    implementation(projects.autoNetworkCommon)
    implementation(projects.autoNetworkBackend)
    compileOnly(libs.minestom)
}

tasks {
    blossom {
        replaceToken("\$version\$", version)
        replaceToken("\$group\$", group)
        replaceToken("\$name\$", displayName)
    }

    processResources {
        expand(mapOf(
            "version" to version,
            "group" to group,
            "name" to displayName
        ))
    }

    test {
        useJUnitPlatform()
    }

    shadowJar {
        dependencies {
            include(project(":auto-network-common"))
            include(project(":auto-network-backend"))
        }
    }

    build {
        dependsOn(shadowJar)
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        withSourcesJar()
    }
}