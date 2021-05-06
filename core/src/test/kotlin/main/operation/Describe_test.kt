package main.operation

import main.Gik.ResetMode
import main.SimpleGit
import main.plusAssign
import kotlin.test.BeforeTest
import kotlin.test.Test

class Describe_test : SimpleGit() {

    @BeforeTest
    override fun setup() {
        super.setup()
        gik.commit(message = "initial commit")

        gik.commit(message = "second commit")
        gik.tag.add(name = "second")

        gik.commit(message = "another commit")
        gik.tag.add(name = "another")

        gik.commit(message = "other commit")
        gik.tag.add(name = "other", annotate = false)
    }

    @Test
    fun `without tag`() {
        gik.reset(commit = "HEAD~3", mode = ResetMode.hard)
        assert(gik.describe() == null)
        assert(gik.describe(always = true) == gik.head!!.abbreviatedId)
    }

    @Test
    fun `with tag`() {
        gik.reset(commit = "HEAD~1", mode = ResetMode.hard)
        assert(gik.describe() == "another")
    }

    @Test
    fun `with additional commit`() {
        repoFile("1.txt") += "1"
        gik.add(patterns = setOf("1.txt"))
        gik.commit(message = "another commit")
        assert(gik.describe()!!.startsWith("another-2-"))
    }

    @Test
    fun `from different commit`() {
        repoFile("1.txt") += "1"
        gik.add(patterns = setOf("1.txt"))
        gik.commit(message = "another commit")
        assert(gik.describe(commit = "HEAD~3") == "second")
    }

    @Test
    fun `with long description`() = assert(gik.describe(longDescr = true)!!.startsWith("another-1-"))

    @Test
    fun `with un-annotated tags`() = assert(gik.describe(tags = true) == "other")

    @Test
    fun `with match`() = assert(gik.describe(match = listOf("second*"))!!.startsWith("second-2-"))
}
