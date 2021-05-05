package gik.operation

import gik.*
import org.eclipse.jgit.api.errors.GitAPIException
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class Clone_test : MultiGit() {

    lateinit var repoDir: File

    lateinit var remoteGik: Gik
    var remoteUri = ""

    val remoteBranchesFilter: (String) -> Boolean = { "refs/remotes/origin/" in it }
    val localBranchesFilter: (String) -> Boolean = { "refs/heads/" in it }
    val lastName: (String) -> String  = { it.split('/').last() }

    @BeforeTest
    fun setup() {
        // TODO: Convert branching and tagging to Grgit.
        repoDir = tempDir.newFolder("local")

        remoteGik = init("remote")
        remoteUri = remoteGik.repo.rootDir.toURI().toString()

        repoFile(remoteGik, "1.txt") += "1"
        remoteGik.commit(message = "do", all = true)

        remoteGik.branch.add(name = "branch1")

        repoFile(remoteGik, "1.txt") += "2"
        remoteGik.commit(message = "do", all = true)

        remoteGik.tag.add(name = "tag1")

        repoFile(remoteGik, "1.txt") += "3"
        remoteGik.commit(message = "do", all = true)

        remoteGik.branch.add(name = "branch2")
    }

    @Test
    fun `clone with non-existent uri fails`() {
        assertFailsWith<GitAPIException> {
            Gik.clone(dir = repoDir, uri = "file:///bad/uri")
        }
    }

    @Test
    fun `clone with default settings clones as expected`() {
        val gik = Gik.clone(dir = repoDir, uri = remoteUri)
        assert(gik.head == remoteGik.head)
        assert(gik.branches().filter(remoteBranchesFilter).map(lastName) == remoteGik.branches().map(lastName))
        assert(gik.branches().filter(localBranchesFilter).map(lastName) == listOf("master"))
        assert(gik.tags.map(lastName) == listOf("tag1"))
        assert(gik.remotes == listOf("origin"))
    }

    @Test
    fun `clone with different remote does not use origin`() {
        val gik = Gik.clone(dir = repoDir, uri = remoteUri, remote = "oranges")
        assert(gik.remotes == listOf("oranges"))
    }

    @Test
    fun `clone with bare true does not have a working tree`() {
        val git = Gik.clone(dir = repoDir, uri = remoteUri, bare = true)
        assert(".git" !in repoFile(git, ".", false).listFiles().map { it.name })
    }

    @Test
    fun `clone with checkout false does not check out a working tree`() {
        val gik = Gik.clone(dir = repoDir, uri = remoteUri, checkout = false)
        assert(repoFile(gik, ".", false).listFiles().map { it.name } == listOf(".git"))
    }

    @Test
    fun `clone with checkout false and refToCheckout set fails`() {
        assertFailsWith<IllegalArgumentException> {
            Gik.clone(dir = repoDir, uri = remoteUri, checkout = false, refToCheckout = "branch2")
        }
    }

    @Test
    fun `clone with refToCheckout set to simple branch name works`() {
        val gik = Gik.clone(dir = repoDir, uri = remoteUri, refToCheckout = "branch1")
        assert(gik.head == remoteGik.resolve.toCommit("branch1"))
        assert(gik.branches().filter(remoteBranchesFilter).map(lastName) == remoteGik.branches().map(lastName))
        assert(gik.branches().filter(localBranchesFilter).map(lastName) == listOf("branch1"))
        assert(gik.tags.map(lastName) == listOf("tag1"))
        assert(gik.remotes == listOf("origin"))
    }

    @Test
    fun `clone with refToCheckout set to simple tag name works`() {
        val gik = Gik.clone(dir = repoDir, uri = remoteUri, refToCheckout = "tag1")
        assert(gik.head == remoteGik.resolve.toCommit("tag1"))
        assert(gik.branches().filter(remoteBranchesFilter).map(lastName) == remoteGik.branches().map(lastName))
        assert(gik.branches().filter(localBranchesFilter).map(lastName).isEmpty())
        assert(gik.tags.map(lastName) == listOf("tag1"))
        assert(gik.remotes == listOf("origin"))
    }

    @Test
    fun `clone with refToCheckout set to full ref name works`() {
        val gik = Gik.clone(dir = repoDir, uri = remoteUri, refToCheckout = "refs/heads/branch2")
        assert(gik.head == remoteGik.resolve.toCommit("branch2"))
        assert(gik.branches().filter(remoteBranchesFilter).map(lastName) == remoteGik.branches().map(lastName))
        assert(gik.branches().filter(localBranchesFilter).map(lastName) == listOf("branch2"))
        assert(gik.tags.map(lastName) == listOf("tag1"))
        assert(gik.remotes == listOf("origin"))
    }

    @Test
    fun `clone with all true works`() {
        val gik = Gik.clone(dir = repoDir, uri = remoteUri, all = true)
        assert(gik.head == remoteGik.resolve.toCommit("master"))
        assert(gik.branches().filter(remoteBranchesFilter).map(lastName) == remoteGik.branches().map(lastName))
        assert(gik.branches().filter(localBranchesFilter).map(lastName) == listOf("master"))
    }

    @Test
    fun `clone with all false has the same effect as all true`() {
        val gik = Gik.clone(dir = repoDir, uri = remoteUri, all = false)
        assert(gik.head == remoteGik.resolve.toCommit("master"))
        assert(gik.branches().filter(remoteBranchesFilter).map(lastName) == remoteGik.branches().map(lastName))
        assert(gik.branches().filter(localBranchesFilter).map(lastName) == listOf("master"))
    }

    @Test
    fun `clone with all false and explicitly set branches works`() {
        val branches = listOf("refs/heads/master", "refs/heads/branch1")
        val gik = Gik.clone(dir = repoDir, uri = remoteUri, all = false, branches = branches)
        assert(gik.head == remoteGik.resolve.toCommit("master"))
        assert(gik.branches().filter(remoteBranchesFilter).map(lastName).sorted() == listOf("master", "branch1").sorted())
        assert(gik.branches().filter(localBranchesFilter).map(lastName) == listOf("master"))
    }

    @Test
    fun `cloned repo can be deleted`() {
        val gik = Gik.clone(dir = repoDir, uri = remoteUri, refToCheckout = "refs/heads/branch2")
        gik.close()
        assert(repoDir.deleteRecursively())
    }
}
