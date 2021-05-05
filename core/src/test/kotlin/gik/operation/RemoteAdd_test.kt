package gik.operation

import gik.Remote
import gik.SimpleGit
import kotlin.test.Test

class RemoteAdd_test : SimpleGit() {

    @Test
    fun `remote with given name and push-fetch urls is added`() {
        val remote = Remote(name = "newRemote",
                            url = "http://fetch.url/",
                            fetchRefSpecs = listOf("+refs/heads/*:refs/remotes/newRemote/*"))

        assert(remote == gik.remote.add(name =  "newRemote", url = "http://fetch.url/"))
        assert(listOf(remote) == gik.remote.list())
    }
}