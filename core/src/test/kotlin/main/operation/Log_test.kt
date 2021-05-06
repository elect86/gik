package main.operation

import main.Commit
import main.SimpleGit
import main.plusAssign
import org.eclipse.jgit.merge.MergeStrategy
import org.eclipse.jgit.revwalk.RevCommit
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class Log_test : SimpleGit() {

    val commits = ArrayList<Commit>()

    @BeforeTest
    override fun setup() {
        super.setup()
        // TODO: Convert to Grgit when merge available
        val testFile1 = repoFile("1.txt")
        val testFile2 = repoFile("2.txt")

        testFile1 += "1"
        testFile2 += "2.1"
        gik.add(patterns = setOf("."))
        commits += gik.commit(message = "first commit\ntesting")

        testFile1 += "2"
        gik.add(patterns = setOf("."))
        commits += gik.commit(message = "second commit")
        gik.tag.add(name = "v1.0.0", message = "annotated tag")

        gik.checkout(branch = commits[0].id)
        testFile1 += "3"
        gik.add(patterns = setOf("."))
        commits += gik.commit(message = "third commit")

        gik.checkout(branch = "master")
        val jgitId = gik.repo resolveObject commits[2].id
        val mergeCommit = gik.repo.jgit.merge().include(jgitId).setStrategy(MergeStrategy.OURS).call().newHead
        commits += Commit(gik.repo, mergeCommit as RevCommit)

        testFile1 += "4"
        gik.add(patterns = setOf("."))
        commits += gik.commit(message = "fifth commit")

        testFile2 += "2.2"
        gik.add(patterns = setOf("."))
        commits += gik.commit(message = "sixth commit")
    }

    @Test
    fun `log with no arguments returns all commits`() =
        assert(gik.log() == listOf(5, 4, 3, 1, 2, 0).map { commits[it] })

    @Test
    fun `log with max commits returns that number of commits`() =
        assert(gik.log(maxCommits = 2) == listOf(5, 4).map { commits[it] })

    @Test
    fun `log with skip commits does not return the first x commits`() =
        assert(gik.log(skipCommits = 2) == listOf(3, 1, 2, 0).map { commits[it] })

    @Test
    fun `log with range returns only the commits in that range`() =
        assert(gik.log(commits[2].id, commits[4].id) == listOf(4, 3, 1).map { commits[it] })

    @Test
    fun `log with non-existing commit fails`() {
        assertFailsWith<IllegalArgumentException> {
            gik.log(includes = listOf("garbage", commits[1].id))
        }
    }

    @Test
    fun `log with path includes only commits with changes for that path`() =
        assert(gik.log(paths = listOf("2.txt")).map { it.id } == listOf(5, 0).map { commits[it] }.map { it.id })

    @Test
    fun `log with annotated tag short name works`() =
        assert(gik.log(includes = listOf("v1.0.0")) == listOf(1, 0).map { commits[it] })
}