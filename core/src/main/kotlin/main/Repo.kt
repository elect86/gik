package main

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.errors.IncorrectObjectTypeException
import org.eclipse.jgit.lib.BranchConfig
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevObject
import org.eclipse.jgit.revwalk.RevWalk
import java.io.File
import java.time.ZoneOffset
import java.time.ZonedDateTime

/** Shortened to avoid clashing with Jgit Repository class */
class Repo(
    /** The directory the repository is contained in. */
    val rootDir: File,

    /** The JGit instance opened for this repository. */
    val jgit: Git,

    /** The credentials used when talking to remote repositories. */
    val credentials: Credentials? = null) {

    override fun toString() = "Repository(${rootDir.canonicalPath})"

    /**
     * Resolves a JGit {@code ObjectId} using the given revision string.
     * @return the resolved object
     */
    infix fun resolveObject(revStr: String?): ObjectId? = when {
        revStr != null && revStr.isNotBlank() -> jgit.repository.resolve(revStr)
        else -> null
    }

    /**
     * Resolves a JGit {@code RevObject} using the given revision string.
     * @param revStr the revision string to use
     * @param peel whether or not to peel the resolved object
     * @return the resolved object
     */
    fun resolveRevObject(revStr: String?, peel: Boolean = false): RevObject? =
        resolveObject(revStr)?.let {
            val walk = RevWalk(jgit.repository)
            val rev = walk.parseAny(it)
            if (peel) walk.peel(rev) else rev
        }

    /**
     * Resolves the parents of an object.
     * @param id the object to get the parents of
     * @return the parents of the commit
     */
    infix fun resolveParents(id: ObjectId?): Set<ObjectId> {
        val walk = RevWalk(jgit.repository)
        val rev = walk.parseCommit(id)
        return rev.parents.map { walk.parseCommit(it) }.toSet()
    }

    /**
     * Resolves a Grgit {@code Commit} using the given revision string.
     * @param revStr the revision string to use
     * @return the resolved commit
     */
    infix fun resolveCommit(revStr: String?): Commit? = when {
        revStr != null && revStr.isNotBlank() -> resolveCommit(resolveObject(revStr))
        else -> null
    }

    /**
     * Resolves a Grgit {@code Commit} using the given object.
     * @param id the object id of the commit to resolve
     * @return the resolved commit
     */
    infix fun resolveCommit(id: ObjectId?): Commit? =
        id?.let {
            val walk = RevWalk(jgit.repository)
            Commit(this, walk.parseCommit(it))
        }

    /**
     * Resolves a Grgit tag from a name.
     * @param name the name of the tag to resolve
     * @return the resolved tag
     */
    infix fun resolveTag(name: String): Tag = resolveTag(jgit.repository.findRef(name))

    /**
     * Resolves a Grgit Tag from a JGit ref.
     * @param ref the JGit ref to resolve
     * @return the resolved tag
     */
    infix fun resolveTag(ref: Ref): Tag {
        val props = mutableMapOf<String, Any>()
        val fullName = ref.name
        var commit: Commit
        try {
            val walk = RevWalk(jgit.repository)
            val rev = walk.parseTag(ref.objectId)
            val target = walk.peel(rev)
            walk.parseBody(rev.`object`)
            commit = Commit(this, target as RevCommit)
            val tagger = rev.taggerIdent
            props["tagger"] = Person(tagger.name, tagger.emailAddress)
            props["fullMessage"] = rev.fullMessage
            props["shortMessage"] = rev.shortMessage

            val instant = rev.taggerIdent.`when`.toInstant()
            val zone = rev.taggerIdent.timeZone?.toZoneId() ?: ZoneOffset.UTC
            props["dateTime"] = ZonedDateTime.ofInstant(instant, zone)
        } catch (e: IncorrectObjectTypeException) {
            commit = resolveCommit(ref.objectId)!!
        }
        return Tag(commit, props["tagger"] as? Person, fullName, props["fullMessage"] as? String,
                   props["shortMessage"] as? String, props["dateTime"] as? ZonedDateTime)
    }

    /**
     * Resolves a Grgit branch from a name.
     * @param name the name of the branch to resolve
     * @return the resolved branch
     */
    infix fun resolveBranch(name: String): Branch? = resolveBranch(jgit.repository.findRef(name))

    /**
     * Resolves a Grgit branch from a JGit ref.
     * @param ref the JGit ref to resolve
     * @return the resolved branch or {@code null} if the {@code ref} is
     * {@code null}
     */
    infix fun resolveBranch(ref: Ref?): Branch? {
        if (ref == null)
            return null
        //        Map props = [:]
        val fullName = ref.name
        val shortName = Repository.shortenRefName(fullName)
        val config = jgit.repository.config
        val branchConfig = BranchConfig(config, shortName)
        return Branch(fullName, when (branchConfig.trackingBranch?.isNotBlank()) {
            true -> resolveBranch(branchConfig.trackingBranch)
            else -> null
        })
    }
}