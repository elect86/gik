package gik.operation

import gik.SimpleGit
import gik.Status
import gik.Status.Changes
import gik.plusAssign
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class Status_test : SimpleGit() {

    @BeforeTest
    override fun setup() {
        super.setup()
        repeat(4) { repoFile("$it.txt") += "1" }
        gik.add(patterns = setOf("."))
        gik.commit(message = "Test")
        gik.checkout(branch = "conflict", createBranch = true)
        repoFile("1.txt") += "2"
        gik.add(patterns = setOf("."))
        gik.commit(message = "conflicting change")
        gik.checkout(branch = "master")
        repoFile("1.txt") += "3"
        gik.add(patterns = setOf("."))
        gik.commit(message = "other change")
    }

    @Test
    fun `with no changes all methods return empty list`() {
        assert(gik.status() == Status())
    }

    @Test
    fun `new unstaged file detected`() {
        repoFile("5.txt") += "5"
        repoFile("6.txt") += "6"
        assert(gik.status() == Status(unstaged = Changes(added = setOf("5.txt", "6.txt"))))
    }

    @Test
    fun `unstaged modified files detected`() {
        repoFile("2.txt") += "2"
        repoFile("3.txt") += "3"
        assert(gik.status() == Status(unstaged = Changes(modified = setOf("2.txt", "3.txt"))))
    }

    @Test
    fun `unstaged deleted files detected`() {
        assert(repoFile("1.txt").delete())
        assert(repoFile("2.txt").delete())
        assert(gik.status() == Status(unstaged = Changes(removed = setOf("1.txt", "2.txt"))))
    }

    @Test
    fun `staged new files detected`() {
        repoFile("5.txt") += "5"
        repoFile("6.txt") += "6"
        gik.add(patterns = setOf("."))
        assert(gik.status() == Status(staged = Changes(added = setOf("5.txt", "6.txt"))))
    }

    @Test
    fun `staged modified files detected`() {
        repoFile("1.txt") += "5"
        repoFile("2.txt") += "6"
        gik.add(patterns = setOf("."))
        assert(gik.status() == Status(staged = Changes(modified = setOf("1.txt", "2.txt"))))
    }

    @Test
    fun `staged removed files detected`() {
        assert(repoFile("3.txt").delete())
        assert(repoFile("0.txt").delete())
        gik.add(patterns = setOf("."), update = true)
        assert(gik.status() == Status(staged = Changes(removed = setOf("3.txt", "0.txt"))))
    }

    @Test
    fun `conflict files detected`() {
        assertFailsWith<IllegalStateException> {
            gik.merge(head = "conflict")
            assert(gik.status() == Status(conflicts = setOf("1.txt")))
        }
    }
}
