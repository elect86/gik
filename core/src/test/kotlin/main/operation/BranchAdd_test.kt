package main.operation

import main.*
import main.service.BranchService.UpstreamMode
import org.eclipse.jgit.api.errors.GitAPIException
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class BranchAdd_test : MultiGit() {

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

        remoteGik.branch.add(name = "my-branch")

        localGik = clone("local", remoteGik)
    }

    @Test
    fun `branch add with name creates branch pointing to current HEAD`() {

        localGik.branch.add(name = "test-branch")

        assert(localGik.branch.list() == listOf(branch("refs/heads/master", "refs/remotes/origin/master"),
                                                branch("refs/heads/test-branch")))
        assert(localGik.resolve.toCommit("test-branch") == localGik.head)
    }

    @Test
    fun `branch add with name and startPoint creates branch pointing to startPoint`() {

        localGik.branch.add(name = "test-branch", startPoint = commits[0].id)

        assert(localGik.branch.list() == listOf(branch("refs/heads/master", "refs/remotes/origin/master"),
                                                branch("refs/heads/test-branch")))
        assert(localGik.resolve.toCommit("test-branch") == commits[0])
    }

    @Test
    fun `branch add fails to overwrite existing branch`() {
        assertFailsWith<GitAPIException> {
            localGik.branch.add(name = "test-branch", startPoint = commits[0].id)
            localGik.branch.add(name = "test-branch")
        }
    }

    @Test
    fun `branch add with mode set but no start point fails`() {
        for (mode in UpstreamMode.values())
            assertFailsWith<IllegalStateException> {
                localGik.branch.add(name = "my-branch", mode = mode)
            }
    }

    @Test
    fun `branch add with null mode starting at origin_my-branch tracks refs_remotes_origin_my-branch`() =
        name(null, "origin/my-branch", "refs/remotes/origin/my-branch")

    @Test
    fun `branch add with track mode starting at origin_my-branch tracks refs_remotes_origin_my-branch`() =
        name(UpstreamMode.track, "origin/my-branch", "refs/remotes/origin/my-branch")

    @Test
    fun `branch add with noTrack mode starting at origin_my-branch tracks refs_remotes_origin_my-branch`() =
        name(UpstreamMode.noTrack, "origin/my-branch", null)

    @Test
    fun `branch add with null mode starting at test-branch tracks null`() =
        name(null, "test-branch", null)

    @Test
    fun `branch add with track mode starting at test-branch tracks refs_heads_test-branch`() =
        name(UpstreamMode.track, "test-branch", "refs/heads/test-branch")

    @Test
    fun `branch add with noTrack mode starting at test-branch tracks null`() =
        name(UpstreamMode.noTrack, "test-branch", null)

    fun name(mode: UpstreamMode?, startPoint: String?, trackingBranch: String?) {
        localGik.branch.add(name = "test-branch", startPoint = commits[0].id)
        assert(localGik.branch.add(name = "local-branch", startPoint = startPoint, mode = mode) == branch("refs/heads/local-branch", trackingBranch))
        assert(localGik.resolve.toCommit("local-branch") == localGik.resolve.toCommit(startPoint))
    }

    @Test
    fun `branch add with no name, null mode, and a start point fails`() = noName(null)

    @Test
    fun `branch add with no name, track mode, and a start point fails`() = noName(UpstreamMode.track)

    @Test
    fun `branch add with no name, noTrack mode, and a start point fails`() = noName(UpstreamMode.noTrack)

    fun noName(mode: UpstreamMode?) {
        assertFailsWith<GitAPIException> {
            localGik.branch.add(startPoint = "origin/my-branch", mode = mode)
        }
    }
}