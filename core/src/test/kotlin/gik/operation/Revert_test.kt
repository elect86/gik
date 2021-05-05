package gik.operation

import gik.Commit
import gik.SimpleGit
import gik.plusAssign
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class Revert_test : SimpleGit() {

    val commits = ArrayList<Commit>()

    @BeforeTest
    override fun setup() {
        super.setup()
        repeat(5) {
            repoFile("$it.txt") += "1"
            gik.add(patterns = setOf("."))
            commits += gik.commit(message = "Test", all = true)
        }
    }

    @Test
    fun `revert with no commits does nothing`() {
        gik.revert()
        assert(gik.log().size == 5)
    }

    @Test
    fun `revert with commits removes associated changes`() {
        gik.revert(commits = listOf(1, 3).map { commits[it].id })
        assert(gik.log().size == 7)
        assert(repoFile(".").listFiles().map { it.name }.filter { !it.startsWith('.') }.toSet() ==
                       listOf(0, 2, 4).map { "$it.txt" }.toSet())
    }

    @Test
    fun `revert with conflicts raises exception`() {
        assertFailsWith<IllegalStateException> {
            repoFile("1.txt") += "Edited"
            gik.add(patterns = setOf("."))
            commits += gik.commit(message = "Modified", all = true)
            gik.revert(commits = listOf(1, 3).map { commits[it].id })
            assert(gik.log().size == 6)
            assert(gik.status().conflicts.containsAll(listOf("1.txt")))
        }
    }
}