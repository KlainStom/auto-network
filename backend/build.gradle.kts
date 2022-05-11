plugins {
    alias(libs.plugins.blossom)
    alias(libs.plugins.shadowJar)
    id("java")
}

dependencies {
    compileOnly(projects.autoNetworkCommon)
    compileOnly(libs.gson)
}
