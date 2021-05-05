package gik.operation

import gik.SimpleGit
import gik.Status
import gik.Status.Changes
import gik.plusAssign
import kotlin.test.BeforeTest
import kotlin.test.Test

class Rm_test : SimpleGit() {

    @BeforeTest
    override fun setup() {
        super.setup()
        repoFile("1.bat") += "1"
        repoFile("something/2.txt") += "2"
        repoFile("test/3.bat") += "3"
        repoFile("test/4.txt") += "4"
        repoFile("test/other/5.txt") += "5"
        gik.add(patterns = setOf("."))
        gik.commit(message = "Test")
    }

    @Test
    fun `removing specific file only removes that file`() {
        val paths = setOf("1.bat")
        gik.remove(patterns = setOf("1.bat"))
        assert(gik.status() == Status(staged = Changes(removed = paths)))
        assert(paths.all { !repoFile(it).exists() })
    }

    @Test
    fun `removing specific directory removes all files within it`() {
        val paths = setOf("test/3.bat", "test/4.txt", "test/other/5.txt")
        gik.remove(patterns = setOf("test"))
        assert(gik.status() == Status(staged = Changes(removed = paths)))
        assert(paths.all { !repoFile(it).exists() })
    }

    @Test
    fun `removing file pattern does not work due to lack of JGit support`() {
        val paths = setOf("1.bat", "something/2.txt", "test/3.bat", "test/4.txt", "test/other/5.txt")
        gik.remove(patterns = setOf("**/*.txt"))
        assert(gik.status().isClean)
        /*
         * TODO: get it to work like this
         * status.removed == ["something/2.txt", "test/4.txt", "test/other/5.txt"] as Set
         */
        assert(paths.all { repoFile(it).exists() })
    }

    @Test
    fun `removing with cached true only removes files from index`() {
        val paths = setOf("something/2.txt")
        gik.remove(patterns = setOf("something"), cached = true)
        assert(gik.status() == Status(staged = Changes(removed = paths), unstaged = Changes(added = paths)))
        assert(paths.all { repoFile(it).exists() })
    }
}
