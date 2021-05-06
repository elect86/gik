package main

import org.eclipse.jgit.api.ListBranchCommand.ListMode
import org.eclipse.jgit.lib.StoredConfig
import org.eclipse.jgit.transport.RemoteConfig
import java.io.File


fun Gik.repoFile(path: String, makeDirs: Boolean = true): File =
    File(repo.rootDir, path).apply {
        if (makeDirs) parentFile.mkdirs()
    }

infix fun Gik.repoDir(path: String): File =
    File(repo.rootDir, path).apply {
        mkdirs()
    }

fun branch(fullName: String, trackingBranchFullName: String? = null): Branch {
    val trackingBranch = trackingBranchFullName?.let(::branch)
    return Branch(fullName, trackingBranch)
}

fun Gik.branches(trim: Boolean = false): List<String> =
    repo.jgit.branchList().apply {
        setListMode(ListMode.ALL)
    }.call().map {
        when {
            trim -> it.name.replace("refs/heads/", "")
            else -> it.name
        }
    }

val Gik.remoteBranches: List<String>
    get() = repo.jgit.branchList().apply { setListMode(ListMode.REMOTE) }.call()
        .map { it.name.replace("refs/remotes/origin/", "") }

val Gik.tags: List<String>
    get() = repo.jgit.tagList().call().map { it.name.replace("refs/tags/", "") }

val Gik.remotes: List<String>
    get() {
        val jgitConfig = repo.jgit.repository.config
        return RemoteConfig.getAllRemoteConfigs(jgitConfig).map { it.name }
    }

infix fun Gik.resolve(revStr: String): Commit? =
    repo resolveCommit revStr

fun Gik.configure(block: StoredConfig.() -> Unit) {
    val config = repo.jgit.repository.config
    config.block()
    config.save()
}