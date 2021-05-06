package main.operation

import main.Gik
import main.repoFile
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.BeforeTest

class Init {

    @Rule @JvmField
    val tempDir = TemporaryFolder()

    lateinit var repoDir: File

    @BeforeTest
    fun setup() {
        repoDir = tempDir.newFolder("repo")
    }

    @Test
    fun `init with bare true does not have a working tree`() {
        val gik = Gik.init(dir= repoDir, bare= true)
        assert(".git" !in gik.repoFile(".", false).listFiles().map { it.name })
    }

    @Test
    fun `init with bare false has a working tree`() {
        val gik = Gik.init(dir = repoDir, bare = false)
        assert(gik.repoFile(".", false).listFiles().map { it.name } == listOf(".git"))
    }

    @Test
    fun `init repo can be deleted after being closed`() {
        val gik = Gik.init(dir = repoDir, bare = false)
        gik.close()
        assert(repoDir.deleteRecursively())
    }
}
