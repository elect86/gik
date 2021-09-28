package main.service

import main.Branch
import main.Repo
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.lib.BranchTrackingStatus

class BranchService(val repo: Repo) {

    /**
     * Gets the branch associated with the current HEAD.
     * @return the branch or {@code null} if the HEAD is detached
     */
    val current: Branch?
        get() = repo.jgit.repository.exactRef("HEAD")?.target?.let { repo resolveBranch it }

    enum class ListMode(val jGit: ListBranchCommand.ListMode?) {
        all(ListBranchCommand.ListMode.ALL),
        remote(ListBranchCommand.ListMode.REMOTE),
        local(null)
    }

    /**
     * Lists branches in the repository. Returns a list of [Branch].
     * @see [git-branch Manual Page](http://git-scm.com/docs/git-branch)
     * @param[mode] Which branches to return
     * @param[contains] Commit ref branches must contains
     */
    fun list(mode: ListMode = ListMode.local, contains: Any? = null): List<Branch?> {
        val cmd = repo.jgit.branchList()
        cmd.setListMode(mode.jGit)
        if (contains != null)
            cmd.setContains(ResolveService(repo) toRevisionString contains)
        return cmd.call().map { repo.resolveBranch(it.name) }
    }

    enum class UpstreamMode(val jGit: SetupUpstreamMode) { track(SetupUpstreamMode.TRACK), noTrack(SetupUpstreamMode.NOTRACK) }

    /**
     * Adds a branch to the repository. Returns the newly created {@link Branch}.
     * @see [git-branch Manual Page](http://git-scm.com/docs/git-branch)
     * @param[name] The name of the branch to add
     * @param[startPoint] The commit the branch should start at. If this is a remote branch it will be automatically tracked
     * @param[mode] The tracking mode to use. If `null`, will use the default behavior
     */
    fun add(name: String? = null, startPoint: Any? = null, mode: UpstreamMode? = null): Branch? {
        if (mode != null && startPoint == null)
            throw IllegalStateException("Cannot set mode if no start point.")
        val cmd = repo.jgit.branchCreate()
            .setName(name)
            .setForce(false)
        if (startPoint != null) {
            val rev = ResolveService(repo) toRevisionString startPoint
            cmd.setStartPoint(rev)
        }
        mode?.let {
            cmd.setUpstreamMode(it.jGit)
        }

        val ref = cmd.call()
        return repo resolveBranch ref
    }

    /**
     * Removes one or more branches from the repository. Returns a list of the fully qualified branch names that were removed.
     * @see [git-branch Manual Page](http://git-scm.com/docs/git-branch)
     * @param[names] List of all branche names to remove
     * @param[force] If `false` (the default), only remove branches that are merged into another branch.
     *               If `true` will delete regardless
     */
    fun remove(name: String, force: Boolean = false): String = remove(listOf(name), force)[0]

    /**
     * Removes one or more branches from the repository. Returns a list of the fully qualified branch names that were removed.
     * @see [git-branch Manual Page](http://git-scm.com/docs/git-branch)
     * @param[names] List of all branche names to remove
     * @param[force] If `false` (the default), only remove branches that are merged into another branch.
     *               If `true` will delete regardless
     */
    fun remove(names: List<String> = emptyList(), force: Boolean = false): List<String> =
        repo.jgit.branchDelete()
            .setBranchNames(*names.map { ResolveService(repo).toBranchName(it) }.toTypedArray())
            .setForce(force)
            .call()

    /**
     * Changes a branch's start point and/or upstream branch. Returns the changed [Branch].
     * @see [git-branch Manual Page](http://git-scm.com/docs/git-branch)
     * @param[name] The name of the branch to change
     * @param[startPoint] The commit the branch should now start at
     * @param[mode] The tracking mode to use
     */
    fun change(name: String, startPoint: Any? = null, mode: UpstreamMode? = null): Branch? {
        if (repo.resolveBranch(name) == null)
            throw IllegalStateException("Branch does not exist: $name")
        if (startPoint == null)
            throw IllegalArgumentException("Must set new startPoint.")
        val cmd = repo.jgit.branchCreate()
            .setName(name)
            .setForce(true)
            .setStartPoint(ResolveService(repo) toRevisionString startPoint)
        if (mode != null)
            cmd.setUpstreamMode(mode.jGit)

        val ref = cmd.call()
        return repo resolveBranch ref
    }

    /**
     * Gets the tracking status of a branch. Returns a {@link BranchStatus}.
     *
     * <pre>
     * def status = grgit.branch.status(name: 'the-branch')
     * </pre>
     * @param[name] The branch to get the status of
     */
    fun status(name: Any?): Branch.Status {
        val realBranch = ResolveService(repo).toBranch(name)!!
        return when {
            realBranch.trackingBranch != null -> when (val status = BranchTrackingStatus.of(repo.jgit.repository, realBranch.fullName)) {
                null -> throw IllegalStateException("Could not retrieve status for $name")
                else -> Branch.Status(realBranch, status.aheadCount, status.behindCount)
            }
            else -> throw IllegalStateException("$name is not set to track another branch")
        }
    }

}