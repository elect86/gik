package main.operation

import main.SimpleGit
import main.branch
import main.plusAssign
import org.eclipse.jgit.api.errors.GitAPIException
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class BranchRemove_test : SimpleGit() {

    @BeforeTest
    override fun setup() {
        super.setup()
        repoFile("1.txt") += "1"
        gik.commit(message = "do", all = true)

        gik.branch.add(name = "branch1")

        repoFile("1.txt") += "2"
        gik.commit(message = "do", all = true)

        gik.branch.add(name = "branch2")

        gik.checkout(branch = "branch3", createBranch = true)
        repoFile("1.txt") += "3"
        gik.commit(message = "do", all = true)

        gik.checkout(branch = "master")
    }

    @Test
    fun `branch remove with empty list does nothing`() {
        assert(gik.branch.remove().isEmpty())
        assert(gik.branch.list() == branches("branch1", "branch2", "branch3", "master"))
    }

    @Test
    fun `branch remove with one branch removes branch`() {
        assert(gik.branch.remove(name = "branch2") == "refs/heads/branch2")
        assert(gik.branch.list() == branches("branch1", "branch3", "master"))
    }

    @Test
    fun `branch remove with multiple branches remvoes branches`() {
        assert(gik.branch.remove(names = listOf("branch2", "branch1")) == listOf("refs/heads/branch2", "refs/heads/branch1"))
        assert(gik.branch.list() == branches("branch3", "master"))
    }

    @Test
    fun `branch remove with invalid branches skips invalid and removes others`() {
        assert(gik.branch.remove(names = listOf("branch2", "blah4")) == listOf("refs/heads/branch2"))
        assert(gik.branch.list() == branches("branch1", "branch3", "master"))
    }

    @Test
    fun `branch remove with unmerged branch and force false fails`() {
        assertFailsWith<GitAPIException> {
            gik.branch.remove(name = "branch3")
        }
    }

    @Test
    fun `branch remove with unmerged branch and force true works`() {
        assert(gik.branch.remove(name = "branch3", force = true) == "refs/heads/branch3")
        assert(gik.branch.list() == branches("branch1", "branch2", "master"))
    }

    @Test
    fun `branch remove with current branch and force true fails`() = branchRemove(true)

    @Test
    fun `branch remove with current branch and force false fails`() = branchRemove(false)

    fun branchRemove(force: Boolean) {
        assertFailsWith<GitAPIException> {
            gik.branch.remove(name = "master", force = true)
        }
    }

    private fun branches(vararg branches: String) = branches.map { branch("refs/heads/$it") }
}