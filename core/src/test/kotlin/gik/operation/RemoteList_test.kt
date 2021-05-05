package gik.operation

import gik.MultiGit
import gik.Remote
import gik.plusAssign
import kotlin.test.Test

class RemoteList_test : MultiGit() {

    @Test
    fun `will list all remotes`() {

        val remoteGrgit = init("remote")

        repoFile(remoteGrgit, "1.txt") += "1"
        remoteGrgit.commit(message = "do", all = true)

        val localGrgit = clone("local", remoteGrgit)

        assert(localGrgit.remote.list() == listOf(
            Remote(name = "origin",
                   url = remoteGrgit.repo.rootDir.canonicalFile.toPath().toUri().toString(),
                   fetchRefSpecs = listOf("+refs/heads/*:refs/remotes/origin/*"))))
    }
}
