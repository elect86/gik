package gik.operation

import gik.Status
import gik.SimpleGit
import gik.plusAssign
import gik.Status.Changes
import kotlin.test.Test

class Add_test : SimpleGit() {

    @Test
    fun `adding specific file only adds that file`() {

        repoFile("1.txt") += "1"
        repoFile("2.txt") += "2"
        repoFile("test/3.txt") += "3"

        gik.add(pattern = "1.txt")

        assert(gik.status() == Status(
            staged = Changes(added = setOf("1.txt")),
            unstaged = Changes(added = setOf("2.txt", "test/3.txt"))))
    }

    @Test
    fun `adding specific directory adds all files within it`() {

        repoFile("1.txt") += "1"
        repoFile("something/2.txt") += "2"
        repoFile("test/3.txt") += "3"
        repoFile("test/4.txt") += "4"
        repoFile("test/other/5.txt") += "5"

        gik.add(pattern = "test")

        assert(gik.status() == Status(
            staged = Changes(added = setOf("test/3.txt", "test/4.txt", "test/other/5.txt")),
            unstaged = Changes(added = setOf("1.txt", "something/2.txt"))))
    }

    @Test
    fun `adding file pattern does not work due to lack of JGit support`() {

        repoFile("1.bat") += "1"
        repoFile("something/2.txt") += "2"
        repoFile("test/3.bat") += "3"
        repoFile("test/4.txt") += "4"
        repoFile("test/other/5.txt") += "5"

        gik.add(pattern = "**/*.txt")

        assert(gik.status() == Status(
            unstaged = Changes(added = setOf("1.bat", "test/3.bat", "something/2.txt", "test/4.txt", "test/other/5.txt"))))
        /*
         * TODO: get it to work like this
         * status.added == ['something/2.txt', 'test/4.txt', 'test/other/5.txt'] as Set
         * status.untracked == ['1.bat', 'test/3.bat'] as Set
         */
    }

    @Test
    fun `adding with update true only adds-removes files already in the index`() {

        repoFile("1.bat") += "1"
        repoFile("something/2.txt") += "2"
        repoFile("test/3.bat") += "3"
        gik.add(pattern = ".")
        gik.repo.jgit.commit().setMessage("Test").call()
        repoFile("1.bat") += "1"
        repoFile("something/2.txt") += "2"
        assert(repoFile("test/3.bat").delete())
        repoFile("test/4.txt") += "4"
        repoFile("test/other/5.txt") += "5"

        gik.add(pattern = ".", update = true)

        assert(gik.status() == Status(
            staged = Changes(modified = setOf("1.bat", "something/2.txt"), removed = setOf("test/3.bat")),
            unstaged = Changes(added = setOf("test/4.txt", "test/other/5.txt"))))
    }
}