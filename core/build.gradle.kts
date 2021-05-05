plugins {
    `maven-publish`
    `java-library`
    kotlin("jvm") version "1.5.0-RC"
    //  id 'org.gradle.test-retry'
}

repositories {
    mavenCentral()
//    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

group = "org.example"
version = "1.0-SNAPSHOT"

dependencies {

    implementation(kotlin("stdlib-jdk8"))

    // jgit
    api("org.eclipse.jgit:org.eclipse.jgit:5.9.0.202009080501-r")
//    api("org.eclipse.jgit:org.eclipse.jgit:5.11.0.202103091610-r")

    // logging
    testImplementation("org.slf4j:slf4j-api:1.7.2")
    runtimeOnly("org.slf4j:slf4j-simple:1.7.2")

    // testing
//    testImplementation("junit:junit:4.13")

    // Use the Kotlin test library.
    testImplementation(kotlin("test"))

    // Use the Kotlin JUnit integration.
    testImplementation(kotlin("test-junit"))

//    testImplementation("pl.pragmatists:JUnitParams:1.1.1")
}

//jar {
//    manifest {
//        attributes 'Automatic-Module-Name': 'org.ajoberstar.grgit'
//    }
//}
//
//test {
//    retry {
//        maxFailures = 1
//        maxRetries = 1
//    }
//}
