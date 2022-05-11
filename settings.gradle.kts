enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.spongepowered.org/maven")
    }
}

rootProject.name = "auto-network"

var modulePrefix = ":auto-network-"

include(modulePrefix + "common")
include(modulePrefix + "backend")
include(modulePrefix + "velocity")
include(modulePrefix + "minestom")

project(modulePrefix + "common").projectDir = file("common")
project(modulePrefix + "backend").projectDir = file("backend")
project(modulePrefix + "velocity").projectDir = file("velocity")
project(modulePrefix + "minestom").projectDir = file("minestom")


