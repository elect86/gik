package gik.operation

import gik.Gik
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test

class TestGeneric {

    @Rule @JvmField
    val tempDir = TemporaryFolder()

    @Test
    fun consumerOperationWorks() {
        val dir = tempDir.newFolder()
        val gik = Gik.init(dir = dir)
        gik.add(patterns = setOf("."))
        gik.commit(message = "First commit")
        assert(gik.log().size == 1)
        gik.close()
    }
}

class TestHead {

    @Rule @JvmField
    val tempDir = TemporaryFolder()

    lateinit var repoDir: File

    @BeforeTest
    fun setup() {
        repoDir = tempDir.newFolder("repo")
    }

    @Test
    fun `head on a newly initialized repo returns null`() {
        val gik = Gik.init(dir = repoDir)
        assert(gik.head == null)
    }
}

