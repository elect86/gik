package gik.operation

import gik.*
import gik.service.BranchService
import gik.service.BranchService.UpstreamMode
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class BranchChange_test : MultiGit() {

    lateinit var localGik: Gik
    lateinit var remoteGik: Gik

    val commits = ArrayList<Commit>()

    @BeforeTest
    fun setup() {
        remoteGik = init("remote")

        repoFile(remoteGik, "1.txt") += "1"
        commits += remoteGik.commit(message = "do", all = true)

        repoFile(remoteGik, "1.txt") += "2"
        commits += remoteGik.commit(message = "do", all = true)

        remoteGik.checkout(branch = "my-branch", createBranch = true)

        repoFile(remoteGik, "1.txt") += "3"
        commits += remoteGik.commit(message = "do", all = true)

        localGik = clone("local", remoteGik)
        localGik.branch.add(name = "local-branch")

        localGik.branch.add(name = "test-branch", startPoint = commits[0].id)
    }

    @Test
    fun `branch change with non-existent branch fails`() {
        assertFailsWith<IllegalStateException> {
            localGik.branch.change(name = "fake-branch", startPoint = "test-branch")
        }
    }

    @Test
    fun `branch change with no start point fails`() {
        assertFailsWith<IllegalArgumentException> {
            localGik.branch.change(name = "local-branch")
        }
    }

    @Test
    fun `branch change with null mode starting at origin_my-branch tracks refs_remotes_origin_my-branch`() =
        branchChange(null, "origin/my-branch", "refs/remotes/origin/my-branch")

    @Test
    fun `branch change with track mode starting at origin_my-branch tracks refs_remotes_origin_my-branch`() =
        branchChange(UpstreamMode.track, "origin/my-branch", "refs/remotes/origin/my-branch")

    @Test
    fun `branch change with noTrack mode starting at origin_my-branch tracks null`() =
        branchChange(UpstreamMode.noTrack, "origin/my-branch", null)

    @Test
    fun `branch change with null mode starting at test-branch tracks null`() =
        branchChange(null, "test-branch", null)

    @Test
    fun `branch change with track mode starting at test-branch tracks refs_heads_test-branch`() =
        branchChange(UpstreamMode.track, "test-branch", "refs/heads/test-branch")

    @Test
    fun `branch change with noTrack mode starting at test-branch tracks null`() =
        branchChange(UpstreamMode.noTrack, "test-branch", null)

    fun branchChange(mode: UpstreamMode?, startPoint: String?, trackingBranch: String?) {
        assert(localGik.branch.change(name = "local-branch", startPoint = startPoint, mode = mode) == branch("refs/heads/local-branch", trackingBranch))
        assert(localGik.resolve.toCommit("local-branch") == localGik.resolve.toCommit(startPoint))
    }
}
