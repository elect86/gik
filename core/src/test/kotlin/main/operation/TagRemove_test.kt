package main.operation

import main.SimpleGit
import main.plusAssign
import kotlin.test.BeforeTest
import kotlin.test.Test

class TagRemove_test : SimpleGit() {

    @BeforeTest
    override fun setup() {
        super.setup()
        repoFile("1.txt") += "1"
        gik.commit(message = "do", all = true)
        gik.tag.add(name = "tag1")

        repoFile("1.txt") += "2"
        gik.commit(message = "do", all = true)
        gik.tag.add(name = "tag2", annotate = false)
    }

    @Test
    fun `tag remove with empty list does nothing`() {
        assert(gik.tag.remove().isEmpty())
        assert(gik.tag.list().map { it.fullName } == listOf("refs/tags/tag1", "refs/tags/tag2"))
    }

    @Test
    fun `tag remove with one tag removes tag`() {
        assert(gik.tag.remove(names = listOf("tag2")) == listOf("refs/tags/tag2"))
        assert(gik.tag.list().map { it.fullName } == listOf("refs/tags/tag1"))
    }

    @Test
    fun `tag remove with multiple tags removes tags`() {
        assert(gik.tag.remove(names = listOf("tag2", "tag1")).toSet() == setOf("refs/tags/tag2", "refs/tags/tag1"))
        assert(gik.tag.list().isEmpty())
    }

    @Test
    fun `tag remove with invalid tags skips invalid and removes others`() {
        assert(gik.tag.remove(names = listOf("tag2", "blah4")) == listOf("refs/tags/tag2"))
        assert(gik.tag.list().map { it.fullName } == listOf("refs/tags/tag1"))
    }
}
