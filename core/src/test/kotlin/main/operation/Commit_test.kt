package main.operation

import main.*
import main.Status.Changes
import org.eclipse.jgit.api.errors.ServiceUnavailableException
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class Commit_test : SimpleGit() {

    @BeforeTest
    override fun setup() {
        super.setup()
        gik.configure {
            setString("user", null, "name", "Alfred Pennyworth")
            setString("user", null, "email", "alfred.pennyworth@wayneindustries.com")
        }

        repoFile("1.txt") += "1"
        repoFile("2.txt") += "1"
        repoFile("folderA/1.txt") += "1"
        repoFile("folderA/2.txt") += "1"
        repoFile("folderB/1.txt") += "1"
        repoFile("folderC/1.txt") += "1"
        gik.add(patterns = setOf("."))
        gik.commit(message = "Test")
        repoFile("1.txt") += "2"
        repoFile("folderA/1.txt") += "2"
        repoFile("folderA/2.txt") += "2"
        repoFile("folderB/1.txt") += "2"
        repoFile("folderB/2.txt") += "2"
    }

    @Test
    fun `commit with all false commits changes from index`() {
        gik.add(patterns = setOf("folderA"))
        gik.commit(message = "Test2")
        assert(gik.log().size == 2)
        assert(gik.status() == Status(unstaged = Changes(added = setOf("folderB/2.txt"), modified = setOf("1.txt", "folderB/1.txt"))))
    }

    @Test
    fun `commit with all true commits changes in previously tracked files`() {
        gik.commit(message = "Test2", all = true)
        assert(gik.log().size == 2)
        assert(gik.status() == Status(unstaged = Changes(added = setOf("folderB/2.txt"))))
    }

    @Test
    fun `commit amend changes the previous commit`() {
        gik.add(patterns = setOf("folderA"))
        gik.commit(message = "Test2", amend = true)
        assert(gik.log().size == 1)
        assert(gik.status() == Status(unstaged = Changes(added = setOf("folderB/2.txt"), modified = setOf("1.txt", "folderB/1.txt"))))
    }

    @Test
    fun `commit with paths only includes the specified paths from the index`() {
        gik.add(patterns = setOf("."))
        gik.commit(message = "Test2", paths = setOf("folderA"))
        assert(gik.log().size == 2)
        assert(gik.status() == Status(staged = Changes(added = setOf("folderB/2.txt"), modified = setOf("1.txt", "folderB/1.txt"))))
    }

    @Test
    fun `commit without specific committer or author uses repo config`() {
        gik.add(patterns = setOf("folderA"))
        val commit = gik.commit(message = "Test2")
        assert(commit.committer == Person("Alfred Pennyworth", "alfred.pennyworth@wayneindustries.com"))
        assert(commit.author == Person("Alfred Pennyworth", "alfred.pennyworth@wayneindustries.com"))
        assert(gik.log().size == 2)
        assert(gik.status() == Status(unstaged = Changes(added = setOf("folderB/2.txt"), modified = setOf("1.txt", "folderB/1.txt"))))
    }

    @Test
    fun `commit with specific committer and author uses those`() {
        gik.add(patterns = setOf("folderA"))
        val bruce = Person("Bruce Wayne", "bruce.wayne@wayneindustries.com")
        val lucius = Person("Lucius Fox", "lucius.fox@wayneindustries.com")
        val commit = gik.commit(
            message = "Test2",
            committer = lucius,
            author = bruce)
        assert(commit.committer == lucius)
        assert(commit.author == bruce)
        assert(gik.log().size == 2)
        assert(gik.status() == Status(unstaged = Changes(added = setOf("folderB/2.txt"), modified = setOf("1.txt", "folderB/1.txt"))))
    }

    @Test
    fun `commit with sign=true tries to sign the commit`() {
        assertFailsWith<ServiceUnavailableException> {
            gik.add(patterns = setOf("folderA"))
            gik.commit(message = "Rest (signed)", sign = true)
        }
    }

    @Test
    fun `commit with sign=false overrides '(commit) gpgSign=true' from _gitconfig`() {
        gik.configure {
            setBoolean("commit", null, "gpgSign", true)
        }
        gik.add(patterns = setOf("."))
        gik.commit(message = "Rest (unsigned)", sign = false)
        assert(gik.log().size == 2)
    }
}
