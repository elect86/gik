package gik

import gik.service.BranchService
import gik.service.RemoteService
import gik.service.ResolveService
import gik.service.TagService
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeCommand
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.diff.RenameDetector
import org.eclipse.jgit.lib.AnyObjectId
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevObject
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.RemoteRefUpdate
import org.eclipse.jgit.transport.TagOpt
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.treewalk.filter.PathFilter
import org.eclipse.jgit.diff.DiffEntry as JgitDiffEntry
import org.eclipse.jgit.diff.DiffEntry.ChangeType as CT


class Gik(

    /** The repository opened by this object. */
    val repo: Repo) : AutoCloseable {

    /** Supports operations on branches. */
    val branch = BranchService(repo)

    /** Supports operations on remotes. */
    val remote = RemoteService(repo)

    /** Convenience methods for resolving various objects. */
    val resolve = ResolveService(repo)

    /** Supports operations on tags. */
    val tag = TagService(repo)

    /** Returns the commit located at the current HEAD of the repository. */
    val head: Commit?
        get() = resolve.toCommit("HEAD")

    /**
     * Checks if {@code base} is an ancestor of {@code tip}.
     * @param base the version that might be an ancestor
     * @param tip the tip version
     * @since 0.2.2
     */
    fun isAncestorOf(base: Any, tip: Any): Boolean {
        val baseCommit = resolve.toCommit(base)!!
        val tipCommit = resolve.toCommit(tip)!!
        return baseCommit.isAncestorOf(tipCommit, repo)
    }

    override fun close() = repo.jgit.repository.close()

    /**
     * Adds files to the index.
     * @see [git-add Manual Page](http://git-scm.com/docs/git-add)
     * @param [pattern] Pattern of file to add to the index.
     * @param[update] if changes to all currently tracked files should be added to the index
     */
    fun add(pattern: String, update: Boolean = false) = add(setOf(pattern), update)

    /**
     * Adds files to the index.
     * @see [git-add Manual Page](http://git-scm.com/docs/git-add)
     * @param [patterns] Patterns of files to add to the index.
     * @param[update] if changes to all currently tracked files should be added to the index
     */
    fun add(patterns: Set<String> = emptySet(), update: Boolean = false) {
        val cmd = repo.jgit.add()
        patterns.forEach(cmd::addFilepattern)
        cmd.isUpdate = update
        cmd.call()
    }

    /**
     * Apply a patch to the index.
     * @see [git-apply Manual Page](http://git-scm.com/docs/git-apply)
     * @param [patch] The patch file to apply to the index.
     */
    fun apply(patch: Any? = null) {
        val cmd = repo.jgit.apply()
        if (patch == null)
            throw IllegalStateException("Must set a patch file.")
        patch.asFile.inputStream().use {
            cmd.setPatch(it).call()
        }
    }

    /**
     * Checks out a branch to the working tree. Does not support checking out specific paths.
     * @see [git-checkout Manual Page](http://git-scm.com/docs/git-checkout)
     * @param[branch] The branch or commit to checkout
     * @param[startPoint] If [createBranch] or [orphan] is `true`, start the new branch at this commit.
     * @param[createBranch] if the branch does not exist and should be created
     * @param[orphan] if the new branch is to be an orphan
     */
    fun checkout(branch: Any? = null, startPoint: Any? = null, createBranch: Boolean = false, orphan: Boolean = false) {
        if (startPoint != null && !createBranch && !orphan)
            throw IllegalArgumentException("Cannot set a start point if createBranch and orphan are false.")
        else if ((createBranch || orphan) && branch == null)
            throw IllegalArgumentException("Must specify branch name to create.")
        val cmd = repo.jgit.checkout()
        val resolve = ResolveService(repo)
        if (branch != null)
            cmd.setName(resolve.toBranchName(branch))
        cmd.setCreateBranch(createBranch)
            .setStartPoint(resolve.toRevisionString(startPoint))
            .setOrphan(orphan)
            .call()
    }

    /**
     * Remove untracked files from the working tree. Returns the list of file paths deleted.
     * @see [git-clean Manual Page](http://git-scm.com/docs/git-clean)
     * @param[paths] The paths to clean. `null` if all paths should be included
     * @param[directories] if untracked directories should also be deleted
     * @param[dryRun] if the files should be returned, but not deleted
     * @param[ignore] if files ignored by `.gitignore` should also be deleted
     */
    fun clean(paths: Set<String>? = null, directories: Boolean = false, dryRun: Boolean = false, ignore: Boolean = true): MutableSet<String>? {
        val cmd = repo.jgit.clean()
        paths?.let(cmd::setPaths)
        return cmd.setCleanDirectories(directories)
            .setDryRun(dryRun)
            .setIgnore(ignore)
            .call()
    }

    /**
     * Commits staged changes to the repository. Returns the new `Commit`.
     * @see [git-commit Manual Reference](http://git-scm.com/docs/git-commit)
     * @param[message] Commit message
     * @param[reflogComment] Comment to put in the reflog
     * @param[committer] The person who committed the changes. Uses the git-config setting, if `null`
     * @param[author] The person who authored the changes. Uses the git-config setting, if `null`
     * @param[paths] Only include these paths when committing. `null` to include all staged changes
     * @param[all] Commit changes to all previously tracked files, even if they aren't staged, if `true`
     * @param[amend] if the previous commit should be amended with these changes
     * @param[sign] `true` to sign, `false` to not sign, and `null` for default behavior (read from configuration)
     */
    fun commit(
        message: String? = null,
        reflogComment: String? = null,
        committer: Person? = null,
        author: Person? = null,
        paths: Set<String> = emptySet(),
        all: Boolean = false,
        amend: Boolean = false,
        sign: Boolean? = null): Commit {

        val cmd = repo.jgit.commit()
        cmd.message = message
        cmd.setReflogComment(reflogComment)
        committer?.let {
            cmd.committer = PersonIdent(it.name, it.email)
        }
        author?.let {
            cmd.author = PersonIdent(it.name, it.email)
        }
        paths.forEach(cmd::setOnly)
        if (all)
            cmd.setAll(all)
        val commit = cmd.setAmend(amend)
            .setSign(sign)
            .call()
        return Commit(repo, commit)
    }

    /**
     * Find the nearest tag reachable. Returns an [String]
     * @see [git-describe Manual Page](http://git-scm.com/docs/git-describe)
     * @param[commit] Sets the commit to be described. Defaults to HEAD
     * @param[always] Whether to show a uniquely abbreviated commit if no tags match
     * @param[longDescr] Whether to always use long output format or not
     * @param[tags] Include non-annotated tags when determining nearest tag
     * @param[match] glob patterns to match tags against before they are considered
     */
    fun describe(
        commit: Any? = null,
        always: Boolean = false,
        longDescr: Boolean = false,
        tags: Boolean = false,
        match: List<String> = emptyList()): String? {

        val cmd = repo.jgit.describe()
        commit?.let {
            cmd.setTarget(ResolveService(repo).toRevisionString(it))
        }
        cmd.setAlways(always)
            .setLong(longDescr)
            .setTags(tags)
        if (match.isNotEmpty())
            cmd.setMatch(*match.toTypedArray())
        return cmd.call()
    }

    /**
     * Show changed files between commits.
     * Returns changes made in commit in the form of {@link org.ajoberstar.grgit.DiffEntry}.
     * @see [git-diff Manual Page](http://git-scm.com/docs/git-diff)
     * @param[oldCommit] The commit to diff against, default HEAD
     * @param[newCommit] The commit to diff against, default HEAD // FIXME
     * @param[pathFilter] Used to limit the diff to the named path
     */
    fun diff(oldCommit: Any?, newCommit: Any? = null, pathFilter: String? = null): List<DiffEntry> {

        fun prepareTreeParser(repository: Repository, objectId: AnyObjectId): AbstractTreeIterator {
            // from the commit we can build the tree which allows us to construct the TreeParser
            val walk = RevWalk(repository)
            val commit = walk.parseCommit(objectId)
            val tree = walk.parseTree(commit.tree.id)
            val treeParser = CanonicalTreeParser()
            val reader = repository.newObjectReader()
            treeParser.reset(reader, tree.id)
            walk.dispose()
            return treeParser
        }

        fun convertChangeType(changeType: CT): DiffEntry.ChangeType = DiffEntry.ChangeType.valueOf(changeType.name.lowercase())

        val cmd = repo.jgit.diff()
        val resolve = ResolveService(repo)
        fun toObjectId(rev: Any): RevObject {
            val revStr = resolve toRevisionString rev
            val id = repo.resolveRevObject(revStr, true)
            return id ?: throw IllegalArgumentException("\"$revStr\" cannot be resolved to an object in this repository.")
        }

        cmd.setShowNameAndStatusOnly(true)
        if (pathFilter != null && pathFilter.isNotBlank())
            cmd.setPathFilter(PathFilter.create(pathFilter))
        if (oldCommit != null) {
            cmd.setOldTree(prepareTreeParser(repo.jgit.repository, toObjectId(oldCommit)))
            if (newCommit != null)
                cmd.setNewTree(prepareTreeParser(repo.jgit.repository, toObjectId(newCommit)))
        }
        return cmd.call().map {
            DiffEntry(changeType = convertChangeType(it.changeType),
                      oldPath = it.oldPath,
                      newPath = it.newPath)
        }
    }

    enum class TagMode(val jGit: TagOpt) { auto(TagOpt.AUTO_FOLLOW), all(TagOpt.FETCH_TAGS), none(TagOpt.NO_TAGS) }

    /**
     * Fetch changes from remotes.
     * @see [git-fetch Manual Reference](http://git-scm.com/docs/git-fetch)
     * @param[remote] Which remote should be fetched
     * @param[refSpecs] List of refspecs to fetch
     * @param[prune] if branches removed by the remote should be removed locally
     * @param[tagMode] How should tags be handled
     */
    fun fetch(remote: String? = null, refSpecs: List<String> = emptyList(), prune: Boolean = false, tagMode: TagMode = TagMode.auto) {

        //        /**
        //         * Provides a string conversion to the enums.
        //         */
        //        void setTagMode(String mode) {
        //            tagMode = mode.toUpperCase()
        //        }

        val cmd = repo.jgit.fetch()
        TransportOpUtil.configure(cmd, repo.credentials)
        if (remote != null && remote.isNotBlank())
            cmd.remote = remote
        cmd.refSpecs = refSpecs.map(::RefSpec)
        cmd.isRemoveDeletedRefs = prune
        cmd.setTagOpt(tagMode.jGit)
            .call()
    }

    /**
     * Gets a log of commits in the repository. Returns a list of {@link Commit}s.
     * Since a Git history is not necessarily a line, these commits may not be in a strict order.
     * @see [git-log Manual Page](http://git-scm.com/docs/git-log)
     */
    fun log(since: String, until: String): List<Commit> = log(listOf(until), listOf(since))

    /**
     * Gets a log of commits in the repository. Returns a list of {@link Commit}s.
     * Since a Git history is not necessarily a line, these commits may not be in a strict order.
     * @see [git-log Manual Page](http://git-scm.com/docs/git-log)
     */
    fun log(includes: List<String> = emptyList(),
            excludes: List<String> = emptyList(),
            paths: List<String> = emptyList(),
            skipCommits: Int = -1,
            maxCommits: Int = -1): List<Commit> {

        val cmd = repo.jgit.log()
        val resolve = ResolveService(repo)
        val toObjectId: (Any) -> RevObject = { rev ->
            val revStr = resolve toRevisionString rev
            repo.resolveRevObject(revStr, true) ?: throw IllegalArgumentException("\"$revStr\" cannot be resolved to an object in this repository.")
        }

        includes.map(toObjectId).forEach {
            cmd.add(it)
        }
        excludes.map(toObjectId).forEach {
            cmd.not(it)
        }
        paths.forEach {
            cmd.addPath(it)
        }
        cmd.setSkip(skipCommits)
            .setMaxCount(maxCommits)
        return cmd.call().map { Commit(repo, it) }
    }

    /**
     * List references in a remote repository.
     * @see [git-ls-remote Manual Page](https://git-scm.com/docs/git-ls-remote)
     */
    fun lsRemote(remote: String = "origin", heads: Boolean = false, tags: Boolean = false): Map<Ref, String> {
        val cmd = repo.jgit.lsRemote()
        TransportOpUtil.configure(cmd, repo.credentials)
        cmd.setRemote(remote)
            .setHeads(heads)
            .setTags(tags)
        return cmd.call().associate { jgitRef ->
            val ref = Ref(jgitRef.name)
            ref to ObjectId.toString(jgitRef.objectId)
        }
    }

    enum class MergeMode {
        /** Fast-forwards if possible, creates a merge commit otherwise. Behaves like --ff. */
        default,

        /** Only merges if a fast-forward is possible. Behaves like --ff-only. */
        onlyFF,

        /** Always creates a merge commit (even if a fast-forward is possible). Behaves like --no-ff. */
        createCommit,

        /** Squashes the merged changes into one set and leaves them uncommitted. Behaves like --squash. */
        squash,

        /** Merges changes, but does not commit them. Behaves like --no-commit. */
        noCommit
    }

    /**
     * Merges changes from a single head. This is a simplified version of
     * merge. If any conflict occurs the merge will throw an exception. The
     * conflicting files can be identified with {@code grgit.status()}.
     *
     * <p>Merge another head into the current branch.</p>
     *
     * <pre>
     * grgit.merge(head: 'some-branch')
     * </pre>
     *
     * <p>Merge with another mode.</p>
     *
     * <pre>
     * grgit.merge(mode: MergeOp.Mode.ONLY_FF)
     * </pre>
     *
     * @see [git-merge Manual Page](http://git-scm.com/docs/git-merge
     * @param[head] The head to merge into the current HEAD
     * @param[message] The message to use for the merge commit
     * @param[mode] How to handle the merge
     */
    fun merge(head: String?, message: String? = null, mode: MergeMode? = null) {

        //        void setMode(String mode) {
        //            this.mode = mode.toUpperCase().replace('-', '_')
        //        }

        val cmd = repo.jgit.merge()
        if (head != null && head.isNotBlank()) {
            // we want to preserve ref name in merge commit msg. if it's a ref, don't resolve down to commit id
            val ref = repo.jgit.repository.findRef(head)
            if (ref == null) {
                val revStr = ResolveService(repo) toRevisionString head
                cmd.include(repo resolveObject revStr)
            } else
                cmd.include(ref)
        }
        if (message != null && message.isNotBlank())
            cmd.setMessage(message)

        when (mode) {
            MergeMode.onlyFF -> cmd.setFastForward(MergeCommand.FastForwardMode.FF_ONLY)
            MergeMode.createCommit -> cmd.setFastForward(MergeCommand.FastForwardMode.NO_FF)
            MergeMode.squash -> cmd.setSquash(true)
            MergeMode.noCommit -> cmd.setCommit(false)
        }

        val result = cmd.call()
        if (!result.mergeStatus.isSuccessful)
            throw IllegalStateException("Could not merge (conflicting files can be retrieved with a call to grgit.status()): $result")
    }

    /**
     * Pulls changes from the remote on the current branch. If the changes conflict, the pull will fail, any conflicts
     * can be retrieved with `grgit.status()`, and throwing an exception.
     *
     * @see [git-pull Manual Page](http://git-scm.com/docs/git-pull)
     * @param[remote] The name of the remote to pull. If not set, the current branch's configuration will be used
     * @param[branch] The name of the remote branch to pull. If not set, the current branch's configuration will be used
     * @param[rebase] If rebasing on top of the changes when they are pulled in
     */
    fun pull(remote: String? = null, branch: String? = null, rebase: Boolean = false) {
        val cmd = repo.jgit.pull()
        if (remote != null && remote.isNotBlank())
            cmd.remote = remote
        if (branch != null && branch.isNotBlank())
            cmd.remoteBranchName = branch
        cmd.setRebase(rebase)
        TransportOpUtil.configure(cmd, repo.credentials)

        val result = cmd.call()
        if (!result.isSuccessful)
            throw IllegalStateException("Could not pull: $result")
    }

    /**
     * Push changes to a remote repository.
     * @see [git-push Manual Page](http://git-scm.com/docs/git-push)
     * @param[remote] The remote to push to
     * @param[refsOrSpecs] The refs or refspecs to use when pushing. If `null` and `all == false` only push the current branch
     * @param[all] to push all branches, or only the current one
     * @param[tags] to push tags
     * @param[force] if branches should be pushed even if they aren't a fast-forward
     * @param[dryRun] if result of this operation should be just estimation of real operation result, no real push is performed.
     * @param[pushOptions] The push options to send to the receiving remote
     */
    fun push(remote: String? = null,
             refsOrSpecs: List<String> = emptyList(),
             all: Boolean = false,
             tags: Boolean = false,
             force: Boolean = false,
             dryRun: Boolean = false,
             pushOptions: List<String> = emptyList()) {

        val cmd = repo.jgit.push()
        TransportOpUtil.configure(cmd, repo.credentials)
        if (remote != null && remote.isNotBlank())
            cmd.remote = remote
        refsOrSpecs.forEach(cmd::add)
        if (all)
            cmd.setPushAll()
        if (tags)
            cmd.setPushTags()
        cmd.isForce = force
        cmd.isDryRun = dryRun
        if (pushOptions.isNotEmpty())
            cmd.pushOptions = pushOptions

        val failures = ArrayList<String>()
        cmd.call().forEach { result ->
            result.remoteUpdates
                .filter { !(it.status == RemoteRefUpdate.Status.OK || it.status == RemoteRefUpdate.Status.UP_TO_DATE) }
                .forEach {
                    val info = "${it.srcRef} to ${it.remoteName}"
                    val m = it.message
                    val message = if (m != null && m.isNotBlank()) " ($m)" else ""
                    failures += "$info$message"
                }
        }
        if (failures.isNotEmpty())
            throw PushException("Failed to push: ${failures.joinToString()}")
    }

    enum class ResetMode(val jGit: ResetCommand.ResetType) {
        /** Reset the index and working tree */
        hard(ResetCommand.ResetType.HARD),

        /** Reset the index, but not the working tree */
        mixed(ResetCommand.ResetType.MIXED),

        /** Only reset the HEAD. Leave the index and working tree as-is */
        soft(ResetCommand.ResetType.SOFT)
    }

    /**
     * Reset changes in the repository.
     * @see [git-reset Manual Page](http://git-scm.com/docs/git-reset)
     * @param[paths] The paths to reset
     * @param[commit] The commit to reset back to. Defaults to HEAD
     * @param[mode] The mode to use when resetting
     */
    fun reset(paths: Set<String> = emptySet(), commit: Any? = null, mode: ResetMode = ResetMode.mixed) {

        //        void setMode(String mode) {
        //            this.mode = mode.toUpperCase()
        //        }

        if (paths.isNotEmpty() && mode != ResetMode.mixed)
            throw IllegalStateException("Cannot set mode when resetting paths.")

        val cmd = repo.jgit.reset()
        paths.forEach(cmd::addPath)
        if (commit != null)
            cmd.setRef(ResolveService(repo) toRevisionString commit)
        if (paths.isEmpty())
            cmd.setMode(mode.jGit)

        cmd.call()
    }

    /**
     * Revert one or more commits. Returns the new HEAD {@link Commit}.
     * @see [git-revert Manual Page](http://git-scm.com/docs/git-revert)
     * @param[commits] List of commits to revert
     */
    fun revert(commits: List<Any> = emptyList()): Commit {
        val cmd = repo.jgit.revert()
        commits.forEach {
            val revStr = ResolveService(repo) toRevisionString it
            cmd.include(repo resolveObject revStr)
        }
        val commit = cmd.call()
        if (cmd.failingResult != null)
            throw IllegalStateException("Could not merge reverted commits (conflicting files can be retrieved with a call to grgit.status()): ${cmd.failingResult}")
        return Commit(repo, commit)
    }

    /**
     * Remove files from the index and (optionally) delete them from the working tree. Note that wildcards are not supported.
     * @see [git-rm Manual Page](http://git-scm.com/docs/git-rm)
     * @param[patterns] The file patterns to remove
     * @param[cached] if files should only be removed from the index
     */
    fun remove(patterns: Set<String> = emptySet(), cached: Boolean = false) {
        val cmd = repo.jgit.rm()
        patterns.forEach(cmd::addFilepattern)
        cmd.setCached(cached)
            .call()
    }

    /**
     * Show changes made in a commit.
     * Returns changes made in commit in the form of [CommitDiff].
     * @see [git-show Manual Page](http://git-scm.com/docs/git-show)
     * @param[commit] The commit to show
     */
    fun show(commit: Any?): Commit.Diff {
        if (commit == null)
            throw IllegalArgumentException("You must specify which commit to show")
        val revString = ResolveService(repo) toRevisionString commit
        val commitId = repo.resolveRevObject(revString) as RevCommit
        val parentId = repo.resolveParents(commitId).firstOrNull() as? RevCommit

        val commit = repo resolveCommit commitId

        val walk = TreeWalk(repo.jgit.repository)
        walk.isRecursive = true

        if (parentId != null) {
            walk.addTree(parentId.tree)
            walk.addTree(commitId.tree)
            val initialEntries = JgitDiffEntry.scan(walk)
            val detector = RenameDetector(repo.jgit.repository)
            detector.addAll(initialEntries)
            val entries = detector.compute()
            val entriesByType = entries.groupBy { it.changeType }

            return Commit.Diff(
                commit,
                added = entriesByType[CT.ADD]?.map { it.newPath }?.toSet() ?: emptySet(),
                copied = entriesByType[CT.COPY]?.map { it.newPath }?.toSet() ?: emptySet(),
                modified = entriesByType[CT.MODIFY]?.map { it.newPath }?.toSet() ?: emptySet(),
                removed = entriesByType[CT.DELETE]?.map { it.oldPath }?.toSet() ?: emptySet(),
                renamed = entriesByType[CT.RENAME]?.map { it.newPath }?.toSet() ?: emptySet(),
                renamings = entriesByType[CT.RENAME]?.associate { it.oldPath to it.newPath } ?: emptyMap())
        } else {
            walk.addTree(commitId.tree)
            val added = mutableSetOf<String>()
            while (walk.next())
                added += walk.pathString
            return Commit.Diff(commit, added)
        }
    }

    /**
     * Gets the current status of the repository. Returns an {@link Status}.
     * @see [git-status Manual Page](http://git-scm.com/docs/git-status)
     */
    fun status(): Status {
        val cmd = repo.jgit.status()
        return Status(cmd.call())
    }

    companion object {

        /**
         * Initializes a new repository. Returns a [Gik] pointing
         * to the resulting repository.
         * @see [git-init Manual Reference](http://git-scm.com/docs/git-init)
         * @param[bare] if the repository should not have a working tree
         * @param[dir] The directory to initialize the repository in.
         */
        fun init(bare: Boolean = false, dir: Any): Gik {
            val d = dir.asFile
            val jgit = Git.init()
                .setBare(bare)
                .setDirectory(d)
                .call()
            return Gik(Repo(d, jgit))
        }

        /**
         * Clones an existing repository. Returns a {@link Grgit} pointing
         * to the resulting repository.
         * @see [git-clone Manual Reference](http://git-scm.com/docs/git-clone)
         * @param[dir] The directory to put the cloned repository.
         * @param[uri] The URI to the repository to be cloned.
         * @param[remote] The name of the remote for the upstream repository. Defaults to `origin`
         * @param[all] Whenever all branches have to be fetched
         * @param[bare] Whenever the resulting repository should be bare
         * @param[branches] The list of full refs to be cloned when `all = false`. Defaults to all available branches
         * @param[checkout] Whenever a working tree should be checked out
         * @param[refToCheckout] The remote ref that should be checked out after the repository is cloned. Defaults to `master`
         * @param[credentials] The username and credentials to use when checking out the repository and for subsequent
         * remote operations on the repository. This is only needed if hardcoded credentials should be used.
         */
        fun clone(
            dir: Any,
            uri: String,
            remote: String = "origin",
            all: Boolean = false,
            bare: Boolean = false,
            branches: List<String> = emptyList(),
            checkout: Boolean = true,
            refToCheckout: String? = null,
            credentials: Credentials? = null): Gik {

            if (!checkout && refToCheckout != null)
                throw IllegalArgumentException("Cannot specify a refToCheckout and set checkout to false.")

            val cmd = Git.cloneRepository()
            TransportOpUtil.configure(cmd, credentials)

            cmd.setDirectory(dir.asFile)
                .setURI(uri)
                .setRemote(remote)
                .setBare(bare)
                .setNoCheckout(!checkout)
            if (refToCheckout != null && refToCheckout.isNotBlank()) cmd.setBranch(refToCheckout)
            if (all) cmd.setCloneAllBranches(all)
            if (branches.isNotEmpty()) cmd.setBranchesToClone(branches)

            val jgit = cmd.call()
            val repo = Repo(dir.asFile, jgit, credentials)
            return Gik(repo)
        }

        /**
         * Opens an existing repository. Returns a [Gik] pointing to the resulting repository.
         * @param[credentials] Hardcoded credentials to use for remote operations
         * @param[dir] The directory to open the repository from. Incompatible with [currentDir]
         * @param[currentDir] The directory to begin searching from the repository from. Incompatible with [dir]
         */
        fun open(credentials: Credentials? = null, dir: Any? = null, currentDir: Any? = null): Gik {
            if (dir != null && currentDir != null)
                throw IllegalArgumentException("Cannot use both dir and currentDir.")
            val repo: Repo
            if (dir != null) {
                val dirFile = dir.asFile
                repo = Repo(dirFile, Git.open(dirFile), credentials)
            } else {
                val builder = FileRepositoryBuilder()
                builder.readEnvironment()
                if (currentDir != null)
                    builder.findGitDir(currentDir.asFile)
                else
                    builder.findGitDir()

                if (builder.gitDir == null)
                    throw IllegalStateException("No .git directory found!")

                val jgitRepo = builder.build()
                val jgit = Git(jgitRepo)
                repo = Repo(jgitRepo.directory, jgit, credentials)
            }
            return Gik(repo)
        }
    }
}