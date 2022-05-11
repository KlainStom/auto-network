plugins {
    alias(libs.plugins.blossom)
    alias(libs.plugins.shadowJar)
    id("java")
}

dependencies {
    implementation(projects.autoNetworkCommon)
    compileOnly(libs.viavelocity)
    compileOnly(libs.netty)
    compileOnly(libs.velocityapi)
    annotationProcessor(libs.velocityapi)
}

tasks {
    shadowJar {
        dependencies {
            include(project(":auto-network-common"))
        }
    }

    java {
        withSourcesJar()
    }

    build {
        dependsOn(shadowJar)
    }
}
