package gik.operation

import gik.*
import gik.Gik.TagMode
import org.eclipse.jgit.api.errors.GitAPIException
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class Fetch_test : MultiGit() {

    lateinit var localGik: Gik
    lateinit var remoteGik: Gik

    @BeforeTest
    fun setup() {
        // TODO: convert after branch and tag available
        remoteGik = init("remote")

        repoFile(remoteGik, "1.txt") += "1"
        remoteGik.commit(message = "do", all = true)

        remoteGik.branch.add(name = "my-branch")

        localGik = clone("local", remoteGik)

        repoFile(remoteGik, "1.txt") += "2"
        remoteGik.commit(message = "do", all = true)

        remoteGik.tag.add(name = "reachable-tag")
        remoteGik.branch.add(name = "sub/mine1")

        remoteGik.checkout(branch = "unreachable-branch",
                           createBranch = true)

        repoFile(remoteGik, "1.txt") += "2.5"
        remoteGik.commit(message = "do-unreachable", all = true)

        remoteGik.tag.add(name = "unreachable-tag")

        remoteGik.checkout(branch = "master")

        repoFile(remoteGik, "1.txt") += "3"
        remoteGik.commit(message = "do", all = true)

        remoteGik.branch.add(name = "sub/mine2")
        remoteGik.branch.remove(names = listOf("my-branch", "unreachable-branch"), force = true)
    }

    @Test
    fun `fetch from non-existent remote fails`() {
        assertFailsWith<GitAPIException> {
            localGik.fetch(remote = "fake")
        }
    }

    @Test
    fun `fetch without other settings, brings down correct commits`() {
        val remoteHead = remoteGik.log(maxCommits = 1).first()
        val localHead = { localGik resolve "refs/remotes/origin/master" }
        assert(localHead() != remoteHead)
        localGik.fetch()
        assert(localHead() == remoteHead)
    }

    @Test
    fun `fetch with prune true, removes refs deleted in the remote`() {
        assert((localGik.remoteBranches - remoteGik.branches(true)).isNotEmpty())
        localGik.fetch(prune = true)
        assert(localGik.remoteBranches == remoteGik.branches(true))
    }

    @Test
    fun `fetch with tag mode none fetches ()`() = fetch(TagMode.none, emptyList())

    @Test
    fun `fetch with tag mode auto fetches (reachable-tag)`() = fetch(TagMode.auto, listOf("reachable-tag"))

    @Test
    fun `fetch with tag mode all fetches (reachable-tag, unreachable-tag)`() = fetch(TagMode.all, listOf("reachable-tag", "unreachable-tag"))

    fun fetch(mode: TagMode, expectedTags: List<String>) {
        assert(localGik.tags.isEmpty())
        localGik.fetch(tagMode = mode)
        assert(localGik.tags == expectedTags)
    }

    @Test
    fun `fetch with refspecs fetches those branches`() {
        assert(localGik.branches() == listOf(
            "refs/heads/master",
            "refs/remotes/origin/master",
            "refs/remotes/origin/my-branch"))
        localGik.fetch(refSpecs = listOf("+refs/heads/sub/*:refs/remotes/origin/banana/*"))
        assert(localGik.branches() == listOf(
            "refs/heads/master",
            "refs/remotes/origin/banana/mine1",
            "refs/remotes/origin/banana/mine2",
            "refs/remotes/origin/master",
            "refs/remotes/origin/my-branch"))
    }
}