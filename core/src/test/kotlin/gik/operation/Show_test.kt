package gik.operation

import gik.Commit
import gik.SimpleGit
import gik.plusAssign
import kotlin.test.Test

class Show_test : SimpleGit() {

    @Test
    fun `can show diffs in commit that added new file`() {
        val fooFile = repoFile("dir1/foo.txt")
        fooFile += "foo!"
        gik.add(patterns = setOf("."))
        val commit = gik.commit(message = "Initial commit")

        assert(gik.show(commit = commit) == Commit.Diff(commit, added = setOf("dir1/foo.txt")))
    }

    @Test
    fun `can show diffs in commit that modified existing file`() {
        val fooFile = repoFile("bar.txt")
        fooFile += "bar!"
        gik.add(patterns = setOf("."))
        gik.commit(message = "Initial commit")

        // Change existing file
        fooFile += "monkey!"
        gik.add(patterns = setOf("."))
        val changeCommit = gik.commit(message = "Added monkey")

        assert(gik.show(commit = changeCommit) == Commit.Diff(commit = changeCommit,
                                                              modified = setOf("bar.txt")))
    }

    @Test
    fun `can show diffs in commit that deleted existing file`() {
        val fooFile = repoFile("bar.txt")
        fooFile += "bar!"
        gik.add(patterns = setOf("."))
        gik.commit(message = "Initial commit")

        // Delete existing file
        gik.remove(patterns = setOf("bar.txt"))
        val removeCommit = gik.commit(message = "Deleted file")

        assert(gik.show(commit = removeCommit) == Commit.Diff(commit = removeCommit,
                                                              removed = setOf("bar.txt")))
    }

    @Test
    fun `can show diffs in commit with multiple changes`() {
        val animalFile = repoFile("animals.txt")
        animalFile += "giraffe!"
        gik.add(patterns = setOf("."))
        gik.commit(message = "Initial commit")

        // Change existing file
        animalFile += "zebra!"

        // Add new file
        val fishFile = repoFile("salmon.txt")
        fishFile += "salmon!"
        gik.add(patterns = setOf("."))
        val changeCommit = gik.commit(message = "Add fish and update animals with zebra")

        assert(gik.show(commit = changeCommit) == Commit.Diff(commit = changeCommit,
                                                              modified = setOf("animals.txt"),
                                                              added = setOf("salmon.txt")))
    }

    @Test
    fun `can show diffs in commit with rename`() {
        repoFile("elephant.txt") += "I have tusks."
        gik.add(patterns = setOf("."))
        gik.commit(message = "Adding elephant.")

        repoFile("elephant.txt").renameTo(repoFile("mammoth.txt"))
        gik.add(patterns = setOf("."))
        gik.remove(patterns = setOf("elephant.txt"))
        val renameCommit = gik.commit(message = "Renaming to mammoth.")

        assert(gik.show(commit = renameCommit) == Commit.Diff(commit = renameCommit,
                                                              renamed = setOf("mammoth.txt"),
                                                              renamings = mapOf("elephant.txt" to "mammoth.txt")))
    }

    @Test
    fun `can show diffs based on rev string`() {
        val fooFile = repoFile("foo.txt")
        fooFile += "foo!"
        gik.add(patterns = setOf("."))
        val commit = gik.commit(message = "Initial commit")

        assert(gik.show(commit = commit.id) == Commit.Diff(commit = commit,
                                                           added = setOf("foo.txt")))
    }
}
