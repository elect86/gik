package gik.operation

import gik.Gik
import gik.MultiGit
import gik.branch
import gik.plusAssign
import gik.service.BranchService.ListMode
import kotlin.test.BeforeTest
import kotlin.test.Test

class BranchList_test : MultiGit() {

    lateinit var localGik: Gik
    lateinit var remoteGik: Gik

    @BeforeTest
    fun setup() {
        remoteGik = init("remote")

        repoFile(remoteGik, "1.txt") += "1"
        remoteGik.commit(message = "do", all = true)

        remoteGik.branch.add(name = "my-branch")

        repoFile(remoteGik, "2.txt") += "2"
        remoteGik.commit(message = "another", all = true)
        remoteGik.tag.add(name = "test-tag")

        localGik = clone("local", remoteGik)
    }

    @Test
    fun `list branch with (no mode, null contains) lists (refs_heads_master, refs_remotes_origin_master)`() =
        listBranch(listOf(listOf("refs/heads/master", "refs/remotes/origin/master")))

    @Test
    fun `list branch with (local mode, null contains) lists (refs_heads_master, refs_remotes_origin_master)`() =
        listBranch(listOf(listOf("refs/heads/master", "refs/remotes/origin/master")), ListMode.local)

    @Test
    fun `list branch with (remote mode, null contains) lists ((refs_remotes_origin_master), (refs_remotes_origin_my-branch))`() =
        listBranch(listOf(listOf("refs/remotes/origin/master"),
                          listOf("refs/remotes/origin/my-branch")), ListMode.remote)

    @Test
    fun `list branch with (all mode, null contains) lists ((refs_heads_master, refs_remotes_origin_master), (refs_remotes_origin_master), (refs_remotes_origin_my-branch))`() =
        listBranch(listOf(listOf("refs/heads/master", "refs/remotes/origin/master"),
                          listOf("refs/remotes/origin/master"),
                          listOf("refs/remotes/origin/my-branch")), ListMode.all)

    @Test
    fun `list branch with (remote mode, 'test-tag' contains) lists (refs_remotes_origin_master)`() =
        listBranch(listOf(listOf("refs/remotes/origin/master")), ListMode.remote, "test-tag")

    fun listBranch(expected: List<List<String>>, mode: ListMode? = null, contains: String? = null) {
        val expectedBranches = expected.map { branch(it[0], it.getOrNull(1)) }
        //        val head = localGik.head
        assert(when(mode) {
                   null -> localGik.branch.list(contains = contains)
                   else -> localGik.branch.list(mode, contains)
               } == expectedBranches)
    }

    @Test
    fun `list branch receives Commit object as contains flag`() {
        val expectedBranches = listOf(branch("refs/remotes/origin/master"))
        val head = localGik.head
        assert(localGik.branch.list(mode = ListMode.remote, contains = head) == expectedBranches)
    }
}
