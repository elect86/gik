package gik.operation

import gik.DiffEntry
import gik.SimpleGit
import gik.plusAssign
import kotlin.test.Test

class Diff_test : SimpleGit() {

    @Test
    fun `can show diffs in commit that added new file`() {
        val fooFile = repoFile("dir0/foo1.txt")
        fooFile += "foo!"
        gik.add(patterns = setOf("."))
        val commit = gik.commit(message = "Initial commit")

        val fooFile2 = repoFile("dir0/foo2.txt")
        fooFile2 += "foo!"
        gik.add(patterns = setOf("."))
        val commit2 = gik.commit(message = "Initial commit")

        assert(gik.diff(oldCommit = commit)[0] ==
                       DiffEntry(changeType = DiffEntry.ChangeType.add,
                                 oldPath = "/dev/null",
                                 newPath = "dir0/foo2.txt"))
    }

    @Test
    fun `can show diffs in commit that modified existing file`() {
        val fooFile = repoFile("dir1/foo.txt")
        fooFile += "foo!"
        gik.add(patterns = setOf("."))
        val commit = gik.commit(message = "Initial commit")
        // modify the file and commit again
        fooFile += "foo!!!"
        gik.add(patterns = setOf("."))
        val commit2 = gik.commit(message = "Second commit")

        assert(gik.diff(oldCommit = commit)[0] == DiffEntry(changeType = DiffEntry.ChangeType.modify,
                                                            oldPath = "dir1/foo.txt",
                                                            newPath = "dir1/foo.txt"))
    }

    @Test
    fun `can show diffs between two commits that modified existing file usig pathFilter`() {
        val fooFile = repoFile("dir2/foo.txt")
        fooFile += "foo!"
        gik.add(patterns = setOf("."))
        val commit = gik.commit(message = "Initial commit")
        // modify the file and commit again
        fooFile += "foo!!!"
        gik.add(patterns = setOf("."))
        val commit2 = gik.commit(message = "Second commit")

        assert(gik.diff(oldCommit = commit, newCommit = commit2, pathFilter = "dir2/foo.txt")[0] ==
                       DiffEntry(changeType = DiffEntry.ChangeType.modify,
                                 oldPath = "dir2/foo.txt",
                                 newPath = "dir2/foo.txt"))
    }

    @Test
    fun `can show diffs in commit that deleted existing file`() {
        val fooFile = repoFile("bar.txt")
        fooFile += "bar!"
        gik.add(patterns = setOf("."))
        val commit = gik.commit(message = "Initial commit")

        // Delete existing file
        gik.remove(patterns = setOf("bar.txt"))
        val removeCommit = gik.commit(message = "Deleted file")

        assert(gik.diff(oldCommit = commit)[0] == DiffEntry (changeType = DiffEntry.ChangeType.delete,
                                                             oldPath = "bar.txt",
                                                             newPath = "/dev/null"))
    }
}
