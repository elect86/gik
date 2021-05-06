package main.operation

import main.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class BranchStatus_test : MultiGit() {

    lateinit var localGik: Gik
    lateinit var remoteGik: Gik

    @BeforeTest
    fun setup() {
        remoteGik = init("remote")

        repoFile(remoteGik, "1.txt") += "1"
        remoteGik.commit(message = "do", all = true)

        remoteGik.checkout(branch = "up-to-date", createBranch = true)

        repoFile(remoteGik, "1.txt") += "2"
        remoteGik.commit(message = "do", all = true)

        remoteGik.checkout(branch = "master")
        remoteGik.checkout(branch = "out-of-date", createBranch = true)

        localGik = clone("local", remoteGik)

        localGik.branch.add(name = "up-to-date", startPoint = "origin/up-to-date")
        localGik.branch.add(name = "out-of-date", startPoint = "origin/out-of-date")
        localGik.checkout(branch = "out-of-date")

        repoFile(remoteGik, "1.txt") += "3"
        remoteGik.commit(message = "do", all = true)

        repoFile(localGik, "1.txt") += "4"
        localGik.commit(message = "do", all = true)
        repoFile(localGik, "1.txt") += "5"
        localGik.commit(message = "do", all = true)

        localGik.branch.add(name = "no-track")

        localGik.fetch()
    }

    @Test
    fun `branch status on branch that is not tracking fails`() {
        assertFailsWith<IllegalStateException> {
            localGik.branch.status(name = "no-track")
        }
    }

    @Test
    fun `branch status on up-to-date gives correct counts`() {
        assert(localGik.branch.status(name = "up-to-date") ==
                       Branch.Status(branch = branch("refs/heads/up-to-date", "refs/remotes/origin/up-to-date"),
                                     aheadCount = 0, behindCount = 0))
    }

    @Test
    fun `branch status on out-of-date gives correct counts`() {
        assert(localGik.branch.status(name = "out-of-date") ==
                       Branch.Status(branch = branch("refs/heads/out-of-date", "refs/remotes/origin/out-of-date"),
                                     aheadCount = 2, behindCount = 1))
    }
}
