
plugins {

    // Apply the Java Gradle plugin development plugin to add support for developing Gradle plugins
    `java-gradle-plugin`

    `kotlin-dsl`
    kotlin("jvm") version embeddedKotlinVersion
    //    id("elect86.magik")
    `maven-publish`
    `java-library`
    id("com.gradle.plugin-publish") version "0.16.0"
}

version = rootProject.version
group = rootProject.group

repositories { mavenCentral() }

dependencies {
    // Align versions of all Kotlin components
    implementation(platform(kotlin("bom", embeddedKotlinVersion)))

    // Use the Kotlin JDK 8 standard library.
    implementation(kotlin("stdlib-jdk8"))

//    implementation(projects.core)

    // Use the Kotlin test library.
    testImplementation(kotlin("test"))

    // Use the Kotlin JUnit integration.
    testImplementation(kotlin("test-junit"))

    api("org.eclipse.jgit:org.eclipse.jgit:5.9.0.202009080501-r")
    //    api("org.eclipse.jgit:org.eclipse.jgit:5.11.0.202103091610-r")

    // logging
    testImplementation("org.slf4j:slf4j-api:1.7.2")
    runtimeOnly("org.slf4j:slf4j-simple:1.7.2")
}

pluginBundle {
    website = "https://github.com/elect86/gik/tree/master"
    vcsUrl = "https://github.com/elect86/gik.git"
    tags = listOf("gik")
}

gradlePlugin {
    // Define the plugin
    plugins.create("gik") {
        id = "elect86.gik"
        displayName = "gik plugin"
        description = "The Kotlin way to use Git"
        implementationClass = "main.GikPlugin"
    }
}

// Add a source set for the functional test suite
val functionalTestSourceSet = sourceSets.create("functionalTest") {
}

gradlePlugin.testSourceSets(functionalTestSourceSet)
configurations.getByName("functionalTestImplementation").extendsFrom(configurations.getByName("testImplementation"))

// Add a task to run the functional tests
val functionalTest by tasks.registering(Test::class) {
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
}

val check by tasks.getting(Task::class) {
    // Run the functional tests as part of `check`
    dependsOn(functionalTest)
}

//java {
//    withJavadocJar()w
//    withSourcesJar()
//}