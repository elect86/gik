//import magik.github

plugins {
    id("org.gradle.kotlin.kotlin-dsl")
    kotlin("jvm")
//    id("elect86.magik")
    `maven-publish`
    `java-library`
}

version = rootProject.version
group = rootProject.group

repositories {
    mavenCentral()
//    gradlePluginPortal()
//    github("kotlin-graphics/mary")
}

////magik { commitWithChanges.set(true) }
//
//publishing {
//    publications {
//        repositories {
//            //            maven {
//            //                name = "prova"
//            //                url = uri("$rootDir/prova")
//            //            }
//            github { domain = "kotlin-graphics/mary" }
//            mavenLocal()
//        }
//    }
//}

//publishing {
////    publications.register<MavenPublication>("maven") {
////        artifactId = "gik"
////        from(components["java"])
////    }
//    repositories {
//        maven {
//            name = "prova"
//            url = uri("$rootDir/prova")
//        }
//    }
//}

java {
    withJavadocJar()
    withSourcesJar()
}