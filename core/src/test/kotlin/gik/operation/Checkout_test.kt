package gik.operation

import gik.SimpleGit
import gik.Status
import gik.Status.Changes
import gik.plusAssign
import gik.text
import org.eclipse.jgit.api.errors.GitAPIException
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class Checkout_test : SimpleGit() {

    @BeforeTest
    override fun setup() {
        super.setup()
        repoFile("1.txt") += "1"
        gik.add(pattern = "1.txt")
        gik.commit(message = "do")

        repoFile("1.txt") += "2"
        gik.add(pattern = "1.txt")
        gik.commit(message = "do")

        gik.branch.add(name = "my-branch")

        repoFile("1.txt") += "3"
        gik.add(pattern = "1.txt")
        gik.commit(message = "do")
    }

    @Test
    fun `checkout with existing branch and createBranch false works`() {
        gik.checkout(branch = "my-branch")
        assert(gik.head == gik.resolve.toCommit("my-branch"))
        assert(gik.branch.current!!.fullName == "refs/heads/my-branch")
        assert(gik.log().size == 2)
        assert(repoFile("1.txt").text == "12")
    }

    @Test
    fun `checkout with existing branch, createBranch true fails`() {
        assertFailsWith<GitAPIException> {
            gik.checkout(branch = "my-branch", createBranch = true)
        }
    }

    @Test
    fun `checkout with non-existent branch and createBranch false fails`() {
        assertFailsWith<GitAPIException> {
            gik.checkout(branch = "fake")
        }
    }

    @Test
    fun `checkout with non-existent branch and createBranch true works`() {
        gik.checkout(branch = "new-branch", createBranch = true)
        assert(gik.branch.current!!.fullName == "refs/heads/new-branch")
        assert(gik.head == gik.resolve.toCommit("master"))
        assert(gik.log().size == 3)
        assert(repoFile("1.txt").text == "123")
    }

    @Test
    fun `checkout with non-existent branch, createBranch true, and startPoint works`() {
        gik.checkout(branch = "new-branch", createBranch = true, startPoint = "my-branch")
        assert(gik.branch.current!!.fullName == "refs/heads/new-branch")
        assert(gik.head == gik.resolve.toCommit("my-branch"))
        assert(gik.log().size == 2)
        assert(repoFile("1.txt").text == "12")
    }

    @Test
    fun `checkout with no branch name and createBranch true fails`() {
        assertFailsWith<IllegalArgumentException> {
            gik.checkout(createBranch = true)
        }
    }

    @Test
    fun `checkout with existing branch and orphan true fails`() {
        assertFailsWith<GitAPIException> {
            gik.checkout(branch = "my-branch", orphan = true)
        }
    }

    @Test
    fun `checkout with non-existent branch and orphan true works`() {
        gik.checkout(branch = "orphan-branch", orphan = true)
        assert(gik.branch.current!!.fullName == "refs/heads/orphan-branch")
        assert(gik.status() == Status(staged = Changes(added = setOf("1.txt"))))
        assert(repoFile("1.txt").text == "123")
    }

    @Test
    fun `checkout with non-existent branch, orphan true, and startPoint works`() {
        gik.checkout(branch = "orphan-branch", orphan = true, startPoint = "my-branch")
        assert(gik.branch.current!!.fullName == "refs/heads/orphan-branch")
        assert(gik.status() == Status(staged = Changes(added = setOf("1.txt"))))
        assert(repoFile("1.txt").text == "12")
    }

    @Test
    fun `checkout with no branch name and orphan true fails`() {
        assertFailsWith<IllegalArgumentException> {
            gik.checkout(orphan = true)
        }
    }
}