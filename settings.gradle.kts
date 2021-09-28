rootProject.name = "gik"

include("core", "plugin")
//include 'grgit-gradle'


enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

gradle.rootProject {
    group = "elect86"
    version = "0.0.2"
}