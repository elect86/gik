package main

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test

class FunctionalTest {

    @Rule @JvmField
    val tempDir = TemporaryFolder()
    lateinit var projectDir: File
    lateinit var buildFile: File

    @BeforeTest
    fun setup() {
        projectDir = tempDir.newFolder("project")
        buildFile = "build.gradle".projectFile()
    }

    @Test
    fun `with no repo, plugin sets gik to null`() {
        buildFile += """
            plugins {
                id 'gik'
            }
    
            task doStuff {
                doLast {
                    assert gik == null
                }
            }
        """.trimIndent()
        val result = build("doStuff")
        assert(result.task(":doStuff")!!.outcome == TaskOutcome.SUCCESS)
    }

    fun init() {
        val gik = Gik.init(dir = projectDir)
        "1.txt".projectFile() += "1"
        gik.add(patterns = setOf("1.txt"))
        gik.commit(message = "yay")
        gik.tag.add(name = "1.0.0")

        buildFile += """
            plugins {
                id 'gik'
            }
            
            task doStuff {
                doLast {
                    println gik.describe()
                }
            }""".trimIndent()
    }

    @Test
    fun `with repo, plugin opens the repo as gik`() {
        init()
        val result = build("doStuff", "--quiet")
        assert(result.task(":doStuff")!!.outcome == TaskOutcome.SUCCESS)
        assert(result.output == "1.0.0\n")
    }

    @Test
    fun `with repo, plugin closes the repo after build is finished`() {
        init()
        val result = build("doStuff", "--info")
        assert(result.task(":doStuff")!!.outcome == TaskOutcome.SUCCESS)
        assert("Closing Git repo" in result.output)
    }

    private fun build(vararg args: String): BuildResult =
        GradleRunner.create()
            .withGradleVersion("7.0")
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .forwardOutput()
            .withArguments(args.toList() + "--stacktrace")
            .build()

    private fun String.projectFile() = File(projectDir, this).apply { parentFile.mkdirs() }
}
