package main.operation

import main.SimpleGit
import main.plusAssign
import kotlin.test.BeforeTest
import kotlin.test.Test

class Clean_test : SimpleGit() {

    @BeforeTest
    override fun setup() {
        super.setup()
        repoFile(".gitignore") += "build/\n.project"
        repoFile("1.txt") += "."
        repoFile("2.txt") += "."
        repoFile("3.txt") += "."
        repoFile("dir1/4.txt") += "."
        repoFile("dir1/5.txt") += "."
        repoFile("dir1/6.txt") += "."
        repoFile("dir2/7.txt") += "."
        repoFile("dir2/8.txt") += "."
        repoDir("dir1/dir3")
        repoDir("dir2/dir4")
        repoDir("dir5")
        repoFile("build/8.txt") += "."
        repoFile(".project") += "."

        gik.add(patterns = setOf(".gitignore", "1.txt", "2.txt", "dir1", "dir2/8.txt"))
        gik.commit(message = "do")
    }

    @Test
    fun `clean with defaults deletes untracked files only`() {
        val expected = setOf("3.txt", "dir2/7.txt")
        assert(gik.clean() == expected)
        assert(expected.all { !repoFile(it).exists() })
    }

    @Test
    fun `clean with paths only deletes from paths`() {
        val expected = setOf("dir2/7.txt")
        assert(gik.clean(paths = setOf("dir2/7.txt")) == expected)
        assert(expected.all { !repoFile(it).exists() })
    }

    @Test
    fun `clean with directories true also deletes untracked directories`() {
        val expected = setOf("3.txt", "dir2/7.txt", "dir5/", "dir2/dir4/", "dir1/dir3/")
        assert(gik.clean(directories = true) == expected)
        assert(expected.all { !repoFile(it).exists() })
    }

    @Test
    fun `clean with ignore false also deletes files ignored by _gitignore`() {
        val expected = setOf("3.txt", "dir2/7.txt", ".project")
        assert(gik.clean(ignore = false) == expected)
        assert(expected.all { !repoFile(it).exists() })
    }

    @Test
    fun `clean with dry run true returns expected but does not delete them`() {
        val expected = setOf("3.txt", "dir2/7.txt")
        assert(gik.clean(dryRun = true) == expected)
        assert(expected.all { repoFile(it).exists() })
    }
}
