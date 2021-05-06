package main.operation

import main.*
import org.eclipse.jgit.api.errors.GitAPIException
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class Push_test : MultiGit() {

    lateinit var localGik: Gik
    lateinit var remoteGik: Gik

    @BeforeTest
    fun setup() {
        // TODO: Conver to Grgit after branch and tag

        remoteGik = init("remote")

        repoFile(remoteGik, "1.txt") += "1"
        remoteGik.commit(message = "do", all = true)

        remoteGik.branch.add(name = "my-branch")

        remoteGik.checkout(branch = "some-branch", createBranch = true)
        repoFile(remoteGik, "1.txt") += "1.5.1"
        remoteGik.commit(message = "do", all = true)
        remoteGik.checkout(branch = "master")

        localGik = clone("local", remoteGik)
        localGik.checkout(branch = "my-branch", createBranch = true)

        repoFile(localGik, "1.txt") += "1.5"
        localGik.commit(message = "do", all = true)

        localGik.tag.add(name = "tag1")

        localGik.checkout(branch = "master")

        repoFile(localGik, "1.txt") += "2"
        localGik.commit(message = "do", all = true)

        localGik.tag.add(name = "tag2")
    }

    @Test
    fun `push to non-existent remote fails`() {
        assertFailsWith<GitAPIException> {
            localGik.push(remote = "fake")
        }
    }

    @Test
    fun `push without other settings pushes correct commits`() {
        localGik.push()
        assert(localGik resolve "refs/heads/master" == remoteGik resolve "refs/heads/master")
        assert(localGik resolve "refs/heads/my-branch" != remoteGik resolve "refs/heads/my-branch")
        assert(remoteGik.tags.isEmpty())
    }

    @Test
    fun `push with all true pushes all branches`() {
        localGik.push(all = true)
        assert(localGik resolve "refs/heads/master" == remoteGik resolve "refs/heads/master")
        assert(localGik resolve "refs/heads/my-branch" == remoteGik resolve "refs/heads/my-branch")
        assert(remoteGik.tags.isEmpty())
    }

    @Test
    fun `push with tags true pushes all tags`() {
        localGik.push(tags = true)
        assert(localGik resolve "refs/heads/master" != remoteGik resolve "refs/heads/master")
        assert(localGik resolve "refs/heads/my-branch" != remoteGik resolve "refs/heads/my-branch")
        assert(localGik.tags == remoteGik.tags)
    }

    @Test
    fun `push with refs only pushes those refs`() {
        localGik.push(refsOrSpecs = listOf("my-branch"))
        assert(localGik resolve "refs/heads/master" != remoteGik resolve "refs/heads/master")
        assert(localGik resolve "refs/heads/my-branch" == remoteGik resolve "refs/heads/my-branch")
        assert(remoteGik.tags.isEmpty())
    }

    @Test
    fun `push with refSpecs only pushes those refs`() {
        localGik.push(refsOrSpecs = listOf("+refs/heads/my-branch:refs/heads/other-branch"))
        assert(localGik resolve "refs/heads/master" != remoteGik resolve "refs/heads/master")
        assert(localGik resolve "refs/heads/my-branch" != remoteGik resolve "refs/heads/my-branch")
        assert(localGik resolve "refs/heads/my-branch" == remoteGik resolve "refs/heads/other-branch")
        assert(remoteGik.tags.isEmpty())
    }

    @Test
    fun `push with non-fastforward fails`() {
        assertFailsWith<PushException> {
            localGik.push(refsOrSpecs = listOf("refs/heads/master:refs/heads/some-branch"))
            assert(localGik resolve "refs/heads/master" != remoteGik resolve "refs/heads/some-branch")
        }
    }

    @Test
    fun `push in dryRun mode does not push commits`() {
        val remoteMasterHead = remoteGik resolve "refs/heads/master"
        localGik.push(dryRun = true)
        assert(localGik resolve "refs/heads/master" != remoteGik resolve "refs/heads/master")
        assert(remoteGik resolve "refs/heads/master" == remoteMasterHead)
    }

    @Test
    fun `push in dryRun mode does not push tags`() {
        val remoteMasterHead = remoteGik resolve "refs/heads/master"
        localGik.push(dryRun = true, tags = true)
        assert(remoteGik.tags.isEmpty())
    }
}
