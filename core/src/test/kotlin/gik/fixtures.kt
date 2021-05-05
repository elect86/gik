package gik

import org.eclipse.jgit.api.Git
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.BeforeTest

abstract class BaseGit {

    @Rule @JvmField
    val tempDir = TemporaryFolder()

    val person = Person("Bruce Wayne", "bruce.wayne@wayneIndustries.com")

    fun baseSetup(repoDir: File): Gik {
        val git = Git.init().setDirectory(repoDir).call()

        // Don't want the user's git config to conflict with test expectations
        git.repository.fs.setUserHome(null)

        git.repository.config.apply {
            setString("user", null, "name", person.name)
            setString("user", null, "email", person.email)
            save()
        }
        return Gik.open(dir = repoDir)
    }
}

open class SimpleGit : BaseGit() {

    lateinit var gik: Gik

    @BeforeTest
    open fun setup() {
        gik = baseSetup(tempDir.newFolder("repo"))
    }

    protected fun repoFile(path: String, makeDirs: Boolean = true): File = gik.repoFile(path, makeDirs)

    protected infix fun repoDir(path: String): File = gik repoDir path
}

open class MultiGit : BaseGit() {

    fun init(name: String): Gik = baseSetup(tempDir.newFolder(name).canonicalFile)

    fun clone(name: String, remote: Gik): Gik =
        Gik.clone(dir = tempDir.newFolder(name),
                  uri = remote.repo.rootDir.toURI().toString())

    fun repoFile(gik: Gik, path: String, makeDirs: Boolean = true): File = gik.repoFile(path, makeDirs)
}
