import org.gradle.kotlin.dsl.support.expectedKotlinDslPluginsVersion

plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
//    maven("https://raw.githubusercontent.com/kotlin-graphics/mary/master")
}

dependencies {

//    val platformVersion = "0.3.3+22"
//    implementation(platform("kotlin.graphics.platform:plugin:$platformVersion"))
//
//    implementation("elect86.magik:elect86.magik.gradle.plugin")
//
    implementation("org.gradle.kotlin:gradle-kotlin-dsl-plugins:$expectedKotlinDslPluginsVersion")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$embeddedKotlinVersion")
}