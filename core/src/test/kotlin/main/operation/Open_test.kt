package main.operation

import main.*
import main.Status.Changes
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.errors.RepositoryNotFoundException
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class Open_test : SimpleGit() {

    companion object {
        private val FILE_PATH = "the-dir/test.txt"
    }

    lateinit var commit: Commit
    lateinit var subDir: File

    @BeforeTest
    override fun setup() {
        super.setup()
        repoFile(FILE_PATH) += "1.1"
        gik.add(patterns = setOf("."))
        commit = gik.commit(message = "first commit")
        subDir = repoDir("the-dir")
    }

    @Test
    fun `open with dir fails if there is no repo in that dir`() {
        assertFailsWith<RepositoryNotFoundException> {
            Gik.open(dir = "dir/with/no/repo")
        }
    }

    @Test
    fun `open with dir succeeds if repo is in that directory`() {
        val opened = Gik.open(dir = repoDir("."))
        assert(opened.head == commit)
    }

    val `isJdk11+`: Boolean
        get() = Integer.parseInt(System.getProperty("java.version").split('.')[0]) >= 11

    //    @RestoreSystemProperties
    //    @IgnoreIf({ Integer.parseInt(System.properties["java.version"].split("\\.")[0]) >= 11})
    @Test
    fun `open without dir fails if there is no repo in the current dir`() {
        if (`isJdk11+`)
            return
        assertFailsWith<IllegalStateException> {
            val workingDir = tempDir.newFolder("no_repo")
            System.setProperty("user.dir", workingDir.absolutePath)
            Gik.open()
        }
    }

    //    @RestoreSystemProperties
    //    @IgnoreIf({ Integer.parseInt(System.properties["java.version"].split("\\.")[0]) >= 11 })
    @Test
    fun `open without dir succeeds if current directory is repo dir`() {
        if (`isJdk11+`)
            return
        val dir = repoDir(".")
        System.setProperty("user.dir", dir.absolutePath)
        val opened = Gik.open()
        repoFile(FILE_PATH) += "1.2"
        opened.add(patterns = setOf(FILE_PATH))
        assert(opened.head == commit)
        assert(opened.status() == Status(staged = Changes(modified = setOf(FILE_PATH))))
    }

    //    @RestoreSystemProperties
    //    @IgnoreIf({ Integer.parseInt(System.properties["java.version"].split("\\.")[0]) >= 11 })
    @Test
    fun `open without dir succeeds if current directory is subdir of a repo`() {
        if (`isJdk11+`)
            return
        System.setProperty("user.dir", subDir.absolutePath)
        val opened = Gik.open()
        repoFile(FILE_PATH) += "1.2"
        assert(opened.head == commit)
        assert(opened.status() == Status(unstaged = Changes(modified = setOf(FILE_PATH))))
    }

    //    @RestoreSystemProperties
    //    @IgnoreIf({ Integer.parseInt(System.properties["java.version"].split("\\.")[0]) >= 11 })
    @Test
    fun `open without dir succeeds if _git in current dir has gitdir`() {

        if (`isJdk11+`)
            return

        val workDir = tempDir.newFolder()
        val gitDir = tempDir.newFolder()

        Git.cloneRepository()
            .setDirectory(workDir)
            .setGitDir(gitDir)
            .setURI(repoDir(".").toURI().toString())
            .call()

        File(workDir, FILE_PATH) += "1.2"
        System.setProperty("user.dir", workDir.absolutePath)

        val opened = Gik.open()

        assert(opened.head == commit)
        assert(opened.status() == Status(unstaged = Changes(modified = setOf(FILE_PATH))))
    }

    //    @RestoreSystemProperties
    //    @IgnoreIf({ Integer.parseInt(System.properties["java.version"].split("\\.")[0]) >= 11 })
    @Test
    fun `open without dir succeeds if _git in parent dir has gitdir`() {

        if (`isJdk11+`)
            return

        val workDir = tempDir.newFolder()
        val gitDir = tempDir.newFolder()

        Git.cloneRepository()
            .setDirectory(workDir)
            .setGitDir(gitDir)
            .setURI(repoDir(".").toURI().toString())
            .call()

        File(workDir, FILE_PATH) += "1.2"
        System.setProperty("user.dir", File(workDir, "the-dir").absolutePath)

        val opened = Gik.open()

        assert(opened.head == commit)
        assert(opened.status() == Status(unstaged = Changes(modified = setOf(FILE_PATH))))
    }

    @Test
    fun `open with currentDir succeeds if current directory is subdir of a repo`() {
        val opened = Gik.open(currentDir = subDir)
        repoFile(FILE_PATH) += "1.2"
        assert(opened.head == commit)
        assert(opened.status() == Status(unstaged = Changes(modified = setOf(FILE_PATH))))
    }

    @Test
    fun `opened repo can be deleted after being closed`() {
        val opened = Gik.open(dir = repoDir(".").canonicalFile)
        opened.close()
        assert(opened.repo.rootDir.deleteRecursively())
    }

    @Test
    fun `credentials as param name should work`() {
        val opened = Gik.open(dir = repoDir("."), credentials = Credentials())
        assert(opened.head == commit)
    }

//    @Test
//    fun `creds as param name should work`() {
//        val opened = Gik.open(dir = repoDir("."), creds = Credentials())
//        assert(opened.head == commit)
//    }
}
