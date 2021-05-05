package gik.operation

import gik.Commit
import gik.SimpleGit
import gik.Tag
import gik.plusAssign
import org.eclipse.jgit.api.errors.GitAPIException
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoField
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class TagAdd_test : SimpleGit() {

    val commits = ArrayList<Commit>()

    @BeforeTest
    override fun setup() {
        super.setup()
        repoFile("1.txt") += "1"
        commits += gik.commit(message = "do", all = true)

        repoFile("1.txt") += "2"
        commits += gik.commit(message = "do", all = true)

        repoFile("1.txt") += "3"
        commits += gik.commit(message = "do", all = true)
    }

    @Test
    fun `tag add creates annotated tag pointing to current HEAD`() {
        val instant = Instant.now().with(ChronoField.NANO_OF_SECOND, 0)
        val zone = ZoneId.ofOffset("GMT", ZoneId.systemDefault().rules.getOffset(instant))
        val tagTime = ZonedDateTime.ofInstant(instant, zone)
        gik.tag.add(name = "test-tag")
        assert(gik.tag.list() == listOf(Tag(commits[2],
                                            person,
                                            fullName = "refs/tags/test-tag",
                                            fullMessage = "",
                                            shortMessage = "",
                                            tagTime)))
        assert(gik.resolve.toCommit("test-tag") == gik.head)
    }

    @Test
    fun `tag add with annotate false creates unannotated tag pointing to current HEAD`() {
        gik.tag.add(name = "test-tag", annotate = false)
        assert(gik.tag.list() == listOf(Tag(commits[2],
                                            tagger = null,
                                            fullName = "refs/tags/test-tag",
                                            fullMessage = null,
                                            shortMessage = null,
                                            dateTime = null)))
        assert(gik.resolve.toCommit("test-tag") == gik.head)
    }

    @Test
    fun `tag add with name and pointsTo creates tag pointing to pointsTo`() {
        val instant = Instant.now().with(ChronoField.NANO_OF_SECOND, 0)
        val zone = ZoneId.ofOffset("GMT", ZoneId.systemDefault().rules.getOffset(instant))
        val tagTime = ZonedDateTime.ofInstant(instant, zone)
        gik.tag.add(name = "test-tag", pointsTo = commits[0].id)
        assert(gik.tag.list() == listOf(Tag(commits[0],
                                            person,
                                            fullName = "refs/tags/test-tag",
                                            fullMessage = "",
                                            shortMessage = "",
                                            dateTime = tagTime)))
        assert(gik.resolve.toCommit("test-tag") == commits[0])
    }

    @Test
    fun `tag add without force fails to overwrite existing tag`() {
        assertFailsWith<GitAPIException> {
            gik.tag.add(name = "test-tag", pointsTo = commits[0].id)
            gik.tag.add(name = "test-tag")
        }
    }

    @Test
    fun `tag add with force overwrites existing tag`() {
        gik.tag.add(name = "test-tag", pointsTo = commits[0].id)
        gik.tag.add(name = "test-tag", force = true)
        assert(gik.resolve.toCommit("test-tag") == gik.head)
    }
}
