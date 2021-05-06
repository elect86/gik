package main.operation

import main.*
import main.Gik.MergeMode
import main.Status.Changes
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class Merge_test : MultiGit() {

    lateinit var localGik: Gik
    lateinit var remoteGik: Gik

    @BeforeTest
    fun setup() {
        remoteGik = init("remote")

        repoFile(remoteGik, "1.txt") += "1.1\n"
        remoteGik.add(patterns = setOf("."))
        remoteGik.commit(message = "1.1", all = true)
        repoFile(remoteGik, "2.txt") += "2.1\n"
        remoteGik.add(patterns = setOf("."))
        remoteGik.commit(message = "2.1", all = true)

        localGik = clone("local", remoteGik)

        remoteGik.checkout(branch = "ff", createBranch = true)

        repoFile(remoteGik, "1.txt") += "1.2\n"
        remoteGik.commit(message = "1.2", all = true)
        repoFile(remoteGik, "1.txt") += "1.3\n"
        remoteGik.commit(message = "1.3", all = true)

        remoteGik.checkout(branch = "clean", startPoint = "master", createBranch = true)

        repoFile(remoteGik, "3.txt") += "3.1\n"
        remoteGik.add(patterns = setOf("."))
        remoteGik.commit(message = "3.1", all = true)
        repoFile(remoteGik, "3.txt") += "3.2\n"
        remoteGik.commit(message = "3.2", all = true)

        remoteGik.checkout(branch = "conflict", startPoint = "master", createBranch = true)

        repoFile(remoteGik, "2.txt") += "2.2\n"
        remoteGik.commit(message = "2.2", all = true)
        repoFile(remoteGik, "2.txt") += "2.3\n"
        remoteGik.commit(message = "2.3", all = true)

        localGik.checkout(branch = "merge-test", createBranch = true)

        repoFile(localGik, "2.txt") += "2.a\n"
        localGik.commit(message = "2.a", all = true)
        repoFile(localGik, "2.txt") += "2.b\n"
        localGik.commit(message = "2.b", all = true)

        localGik.fetch()
    }

    @Test
    fun `merging 'origin_ff' with default does a fast-forward merge`() = fastForward(MergeMode.default)

    @Test
    fun `merging 'origin_ff' with onlyFF does a fast-forward merge`() = fastForward(MergeMode.onlyFF)

    @Test
    fun `merging 'origin_ff' with noCommit does a fast-forward merge`() = fastForward(MergeMode.noCommit)

    fun fastForward(mode: MergeMode) {
        val head = "origin/ff"
        localGik.checkout(branch = "master")
        localGik.merge(head, mode = mode)
        assert(localGik.status().isClean)
        assert(localGik.head == remoteGik.resolve.toCommit(head.replace("origin/", "")))
    }

    @Test
    fun `merging 'origin_ff' with createCommit creates a merge commit`() = mergeCommits("origin/ff", MergeMode.createCommit)

    @Test
    fun `merging 'origin_clean' with default creates a merge commit`() = mergeCommits("origin/clean", MergeMode.default)

    @Test
    fun `merging 'origin_clean' with createCommit creates a merge commit`() = mergeCommits("origin/clean", MergeMode.createCommit)

    fun mergeCommits(head: String, mode: MergeMode) {
        val oldHead = localGik.head!!
        val mergeHead = remoteGik.resolve.toCommit(head.replace("origin/", ""))!!
        localGik.merge(head, mode = mode)
        assert(localGik.status().isClean)

        // has a merge commit
        assert(localGik.log(includes = listOf("HEAD"),
                            excludes = listOf(oldHead.id, mergeHead.id)).size == 1)
    }

    @Test
    fun `merging 'origin_clean' with #mode merges but leaves them uncommitted`() {
        val head = "origin/clean"
        val oldHead = localGik.head
        val mergeHead = remoteGik.resolve.toCommit(head.replace("origin/", ""))!!
        localGik.merge(head, mode = MergeMode.noCommit)
        assert(localGik.status() == Status(staged = Changes(added = setOf("3.txt"))))
        assert(localGik.head == oldHead)
        assert(repoFile(localGik, ".git/MERGE_HEAD").text.trim() == mergeHead.id)
    }

    @Test
    fun `merging 'origin_ff' with 'squash' squashes changes but leaves them uncommitted`() =
        squash("origin/ff", Status(staged = Changes(modified = setOf("1.txt"))))

    @Test
    fun `merging 'origin_clean' with 'squash' squashes changes but leaves them uncommitted`() =
        squash("origin/clean", Status(staged = Changes(added = setOf("3.txt"))))

    fun squash(head: String, status: Status) {
        val oldHead = localGik.head
        localGik.merge(head, mode = MergeMode.squash)
        assert(localGik.status() == status)
        assert(localGik.head == oldHead)
        assert(!repoFile(localGik, ".git/MERGE_HEAD").exists())
    }

    @Test
    fun `merging 'origin_clean' with 'onlyFF' fails with correct status`() =
        mergeFails("origin/clean", MergeMode.onlyFF, Status())

    @Test
    fun `merging 'origin_conflict' with 'default' fails with correct status`() =
        mergeFails("origin/conflict", MergeMode.default, Status(conflicts = setOf("2.txt")))

    @Test
    fun `merging 'origin_conflict' with 'onlyFF' fails with correct status`() =
        mergeFails("origin/conflict", MergeMode.onlyFF, Status())

    @Test
    fun `merging 'origin_conflict' with 'createCommit' fails with correct status`() =
        mergeFails("origin/conflict", MergeMode.createCommit, Status(conflicts = setOf("2.txt")))

    @Test
    fun `merging 'origin_conflict' with 'squash' fails with correct status`() =
        mergeFails("origin/conflict", MergeMode.squash, Status(conflicts = setOf("2.txt")))

    @Test
    fun `merging 'origin_conflict' with 'noCommit' fails with correct status`() =
        mergeFails("origin/conflict", MergeMode.noCommit, Status(conflicts = setOf("2.txt")))

    fun mergeFails(head: String, mode: MergeMode, status: Status) {
        assertFailsWith<IllegalStateException> {
            val oldHead = localGik.head
            localGik.merge(head, mode = mode)
            assert(localGik.head == oldHead)
            assert(localGik.status() == status)
        }
    }

    @Test
    fun `merge uses message if supplied`() {
        val oldHead = localGik.head!!
        val mergeHead = remoteGik.resolve.toCommit("clean")!!
        localGik.merge(head = "origin/clean", message = "Custom message")
        assert(localGik.status().isClean) { "all changes are committed" }
        assert(localGik.log(includes = listOf("HEAD"),
                            excludes = listOf(oldHead.id, mergeHead.id)).size == 1) { "a merge commit was created" }
        assert(localGik.head!!.shortMessage == "Custom message") { "the merge commits message is what was passed in" }
    }

    @Test
    fun `merge of a branch includes this in default message`() {
        val oldHead = localGik.head!!
        val mergeHead = remoteGik.resolve.toCommit("clean")!!
        localGik.merge(head = "origin/clean")
        assert(localGik.status().isClean) { "all changes are committed" }
        assert(localGik.log(includes = listOf("HEAD"),
                            excludes = listOf(oldHead.id, mergeHead.id)).size == 1) { "a merge commit was created" }
        assert(localGik.head!!.shortMessage == "Merge remote-tracking branch 'origin/clean' into merge-test") { "the merge commits message mentions branch name" }
    }

    @Test
    fun `merge of a commit includes this in default message`() {
        val oldHead = localGik.head!!
        val mergeHead = remoteGik.resolve.toCommit("clean")!!
        localGik.merge(head = mergeHead.id)
        assert(localGik.status().isClean) { "all changes are committed" }
        assert(localGik.log(includes = listOf("HEAD"),
                            excludes = listOf(oldHead.id, mergeHead.id)).size == 1) { "a merge commit was created" }
        assert(localGik.head!!.shortMessage == "Merge commit '${mergeHead.id}' into merge-test") { "the merge commits message mentions commit hash" }
    }
}