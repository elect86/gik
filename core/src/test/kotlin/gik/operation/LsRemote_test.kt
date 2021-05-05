package gik.operation

import gik.*
import org.eclipse.jgit.api.errors.GitAPIException
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class LsRemote_test : MultiGit() {

    lateinit var localGik: Gik
    lateinit var remoteGik: Gik

    val branches = ArrayList<Branch>()
    val tags = ArrayList<Tag>()

    @BeforeTest
    fun setup() {
        remoteGik = init("remote")

        branches += remoteGik.branch.current!!

        repoFile(remoteGik, "1.txt") += "1"
        remoteGik.commit(message = "do", all = true)

        branches += remoteGik.branch.add(name = "my-branch")!!

        localGik = clone("local", remoteGik)

        repoFile(remoteGik, "1.txt") += "2"
        remoteGik.commit(message = "do", all = true)

        tags += remoteGik.tag.add(name = "reachable-tag")
        branches += remoteGik.branch.add(name = "sub/mine1")!!

        remoteGik.checkout(branch = "unreachable-branch",
                           createBranch = true)
        branches += remoteGik.branch.list().find { it?.name == "unreachable-branch" }!!

        repoFile(remoteGik, "1.txt") += "2.5"
        remoteGik.commit(message = "do-unreachable", all = true)

        tags += remoteGik.tag.add(name = "unreachable-tag")

        remoteGik.checkout(branch = "master")

        repoFile(remoteGik, "1.txt") += "3"
        remoteGik.commit(message = "do", all = true)

        branches += remoteGik.branch.add(name = "sub/mine2")!!

        //        println remoteGrgit . branch . list ()
        //        println remoteGrgit . tag . list ()
    }

    @Test
    fun `lsremote from non-existent remote fails`() {
        assertFailsWith<GitAPIException> {
            localGik.lsRemote(remote = "fake")
        }
    }

    @Test
    fun `lsremote returns all refs`() =
        assert(localGik.lsRemote() == format(Ref("HEAD"), *branches.toTypedArray(), *tags.toTypedArray()))

    @Test
    fun `lsremote returns branches and tags`() =
        assert(localGik.lsRemote(heads = true, tags = true) == format(*branches.toTypedArray(), *tags.toTypedArray()))

    @Test
    fun `lsremote returns only branches`() =
        assert(localGik.lsRemote(heads = true) == format(*branches.toTypedArray()))

    @Test
    fun `lsremote returns only tags`() =
        assert(localGik.lsRemote(tags = true) == format(*tags.toTypedArray()))

    private fun format(vararg things: FullName): Map<Ref, String?> =
        things.associate { refish ->
            val ref = Ref(refish.fullName)
            ref to remoteGik.resolve.toObjectId(refish)
        }
}
