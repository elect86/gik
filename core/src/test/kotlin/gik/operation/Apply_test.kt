package gik.operation

import gik.SimpleGit
import gik.plusAssign
import gik.text
import kotlin.test.Test
import kotlin.test.assertFailsWith

class Apply_test : SimpleGit() {

    @Test
    fun `apply with no patch fails`() {
        assertFailsWith<IllegalStateException> {
            gik.apply()
        }
    }

    @Test
    fun `apply with patch succeeds`() {

        repoFile("1.txt") += "something"
        repoFile("2.txt") += "something else\n"
        gik.add(pattern = ".")
        gik.commit(message = "Test")
        val patch = tempDir.newFile()
        patch.appendBytes(javaClass.classLoader.getResourceAsStream("sample.patch").readAllBytes())

        gik.apply(patch = patch)

        assert(repoFile("1.txt").text == "something")
        assert(repoFile("2.txt").text == "something else\nis being added\n")
        assert(repoFile("3.txt").text == "some new stuff\n")
    }
}