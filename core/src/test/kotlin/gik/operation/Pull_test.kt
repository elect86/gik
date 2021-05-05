package gik.operation

import gik.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class Pull_test : MultiGit() {

    lateinit var localGik: Gik
    lateinit var remoteGik: Gik
    lateinit var otherRemoteGik: Gik
    lateinit var ancestorHead: Commit

    @BeforeTest
    fun setup() {
        remoteGik = init("remote")

        repoFile(remoteGik, "1.txt") += "1.1\n"
        remoteGik.add(patterns = setOf("."))
        ancestorHead = remoteGik.commit(message = "1.1", all = true)

        remoteGik.branch.add(name = "test-branch")

        localGik = clone("local", remoteGik)
        localGik.branch.add(name = "test-branch", startPoint = "origin/test-branch")

        otherRemoteGik = clone("remote2", remoteGik)
        repoFile(otherRemoteGik, "4.txt") += "4.1\n"
        otherRemoteGik.add(patterns = setOf("."))
        otherRemoteGik.commit(message = "4.1", all = true)

        repoFile(remoteGik, "1.txt") += "1.2\n"
        remoteGik.commit(message = "1.2", all = true)
        repoFile(remoteGik, "1.txt") += "1.3\n"
        remoteGik.commit(message = "1.3", all = true)

        remoteGik.checkout(branch = "test-branch")

        repoFile(remoteGik, "2.txt") += "2.1\n"
        remoteGik.add(patterns = setOf("."))
        remoteGik.commit(message = "2.1", all = true)
        repoFile(remoteGik, "2.txt") += "2.2\n"
        remoteGik.commit(message = "2.2", all = true)
    }

    @Test
    fun `pull to local repo with no changes fast-forwards current branch only`() {
        val localTestBranchHead = localGik.resolve.toCommit("test-branch")
        localGik.pull()
        assert(localGik.head == remoteGik.resolve.toCommit("master"))
        assert(localGik.resolve.toCommit("test-branch") == localTestBranchHead)
    }

    @Test
    fun `pull to local repo with clean changes merges branches from origin`() {
        repoFile(localGik, "3.txt") += "3.1\n"
        localGik.add(patterns = setOf("."))
        localGik.commit(message = "3.1")
        val localHead = localGik.head!!
        val remoteHead = remoteGik.resolve.toCommit("master")!!
        localGik.pull()
        // includes all commits from remote
        assert((remoteGik.log(includes = listOf("master")) - localGik.log()).isEmpty())
        /*
         * Go back to one pass log command when bug is fixed:
         * https://bugs.eclipse.org/bugs/show_bug.cgi?id=439675
         */
        // localGrgit.log {
        //	 includes = [remoteHead.id]
        //	 excludes = ["HEAD"]
        // }.size() == 0

        // has merge commit
        assert(localGik.log(includes = listOf("HEAD"),
                            excludes = listOf(localHead.id, remoteHead.id)).size == 1)
    }

    @Test
    fun `pull to local repo with conflicting changes fails`() {
        assertFailsWith<IllegalStateException> {
            repoFile(localGik, "1.txt") += "1.4\n"
            localGik.commit(message = "1.4", all = true)
            val localHead = localGik.head
            localGik.pull()
            assert(localGik.status() == Status(conflicts = setOf("1.txt")))
            assert(localGik.head == localHead)
        }
    }

    @Test
    fun `pull to local repo with clean changes and rebase rebases changes on top of origin`() {
        repoFile(localGik, "3.txt") += "3.1\n"
        localGik.add(patterns = setOf("."))
        localGik.commit(message = "3.1")
        val localHead = localGik.head!!
        val remoteHead = remoteGik.resolve.toCommit("master")!!
        val localCommits = localGik.log(includes = listOf(localHead.id),
                                        excludes = listOf(ancestorHead.id))
        localGik.pull(rebase = true)

        // includes all commits from remote
        assert(localGik.log(includes = listOf(remoteHead.id),
                            excludes = listOf("HEAD")).isEmpty())

        // includes none of local commits
        assert(localGik.log(includes = listOf(localHead.id),
                            excludes = listOf("HEAD")) == localCommits)

        // has commit comments from local
        assert(localGik.log(includes = listOf("HEAD"),
                            excludes = listOf(remoteHead.id))
                   .map { it.fullMessage } == localCommits.map { it.fullMessage })

        // has state of all changes
        assert(repoFile(localGik, "1.txt").text == "1.1\n1.2\n1.3\n")
        assert(repoFile(localGik, "3.txt").text == "3.1\n")
    }

    @Test
    fun `pull to local repo from other remote fast-forwards current branch`() {
        val otherRemoteUri = otherRemoteGik.repo.rootDir.toURI().toString()
        localGik.remote.add(name = "other-remote", url = otherRemoteUri)
        localGik.pull(remote = "other-remote")
        assert(localGik.head == otherRemoteGik.head)
    }

    @Test
    fun `pull to local repo from specific remote branch merges changes`() {
        localGik.pull(branch = "test-branch")
        assert((remoteGik.log(includes = listOf("test-branch")) - localGik.log()).isEmpty())
    }
}
