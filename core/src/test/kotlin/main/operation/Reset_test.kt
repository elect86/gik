package main.operation

import main.*
import main.Gik.ResetMode
import main.Status.Changes
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class Reset_test : SimpleGit() {

    val commits = ArrayList<Commit>()

    @BeforeTest
    override fun setup() {
        super.setup()
        repoFile("1.bat") += "1"
        repoFile("something/2.txt") += "2"
        repoFile("test/3.bat") += "3"
        repoFile("test/4.txt") += "4"
        repoFile("test/other/5.txt") += "5"
        gik.add(patterns = setOf("."))
        commits += gik.commit(message = "Test")
        repoFile("1.bat") += "2"
        repoFile("test/3.bat") += "4"
        gik.add(patterns = setOf("."))
        commits += gik.commit(message = "Test")
        repoFile("1.bat") += "3"
        repoFile("something/2.txt") += "2"
        gik.add(patterns = setOf("."))
        repoFile("test/other/5.txt") += "6"
        repoFile("test/4.txt") += "5"
    }

    @Test
    fun `reset soft changes HEAD only`() {
        gik.reset(mode = ResetMode.soft, commit = commits[0].id)
        assert(commits[0] == gik.head)
        assert(gik.status() == Status(staged = Changes(modified = setOf("1.bat", "test/3.bat", "something/2.txt")),
                                      unstaged = Changes(modified = setOf("test/4.txt", "test/other/5.txt"))))
    }

    @Test
    fun `reset mixed changes HEAD and index`() {
        gik.reset(mode = ResetMode.mixed, commit = commits[0].id)
        assert(commits[0] == gik.head)
        assert(gik.status() == Status(unstaged = Changes(modified = setOf("1.bat", "test/3.bat", "test/4.txt", "something/2.txt", "test/other/5.txt"))))
    }

    @Test
    fun `reset hard changes HEAD, index, and working tree`() {
        gik.reset(mode = ResetMode.hard, commit = commits[0].id)
        assert(commits[0] == gik.head)
        assert(gik.status().isClean)
    }

    @Test
    fun `reset with paths changes index only`() {
        gik.reset(paths = setOf("something/2.txt"))
        assert(commits[1] == gik.head)
        assert(gik.status() == Status(staged = Changes(modified = setOf("1.bat")),
                                      unstaged = Changes(modified = setOf("test/4.txt", "something/2.txt", "test/other/5.txt"))))
    }

    @Test
    fun `reset with paths and mode set not supported`() {
        assertFailsWith<IllegalStateException> {
            gik.reset(mode = ResetMode.hard, paths = setOf("."))
        }
    }
}