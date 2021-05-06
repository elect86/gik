package main

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra

/**
 * Plugin adding a {@code grgit} property to all projects
 * that searches for a Git repo from the project's
 * directory.
 */
class GikPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        try {
            val gik = Gik.open(currentDir = target.rootDir)

            // Make sure Git repo is closed when the build is over. Ideally, this avoids issues with the daemon.
            target.gradle.buildFinished {
                target.logger.info("Closing Git repo: ${gik.repo.rootDir}")
                gik.close()
            }

            target.allprojects {
                if (extensions.findByName("gik") != null)
                    logger.warn("Project $path already has a gik property. Remove gik from either $path or ${target.path}.")
                extensions.add(Gik::class.java, "gik", gik)
            }
        } catch (e: Exception) {
            target.logger.debug("Failed trying to find git repository for ${target.path}", e)
            target.extra.set("gik", null)
        }
    }
}
