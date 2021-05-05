package gik

import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import java.time.ZonedDateTime

/**
 * A commit.
 * @since 0.1.0
 */
data class Commit(

    /** The full hash of the commit. */
    val id: String,

    /** The abbreviated hash of the commit. */
    val abbreviatedId: String,

    /** Hashes of any parent commits. */
    val parentIds: List<String>,

    /** The author of the changes in the commit. */
    val author: Person,

    /** The committer of the changes in the commit. */
    val committer: Person,

    /** The time the commit was created with the time zone of the committer, if available. */
    val dateTime: ZonedDateTime,

    /** The full commit message. */
    val fullMessage: String,

    /** The shortened commit message. */
    val shortMessage: String) {

    constructor(repo: Repo, rev: RevCommit) : this(
        id = ObjectId.toString(rev),
        abbreviatedId = repo.jgit.repository.newObjectReader().use { it.abbreviate(rev).name() },
        committer = Person(rev.committerIdent),
        author = Person(rev.authorIdent),
        dateTime = rev.dateTime,
        parentIds = rev.parents.map { ObjectId.toString(it) },
        fullMessage = rev.fullMessage,
        shortMessage = rev.shortMessage)

    data class Diff(
        val commit: Commit?,
        val added: Set<String> = emptySet(),
        val copied: Set<String> = emptySet(),
        val modified: Set<String> = emptySet(),
        val removed: Set<String> = emptySet(),
        val renamed: Set<String> = emptySet(),
        val renamings: Map<String, String> = emptyMap()) {

        /** Gets all changed files. */
        val allChanges: Set<String>
            get() = added + copied + modified + removed + renamed
    }

    /**
     * Checks if this is an ancestor of {@code tip}.
     * @param repo the repository to look in
     * @param tip the tip version
     * @since 0.2.2
     */
    fun isAncestorOf(tip: Commit, repo: Repo): Boolean {
        val jgit = repo.jgit.repository
        val revWalk = RevWalk(jgit)
        val baseCommit = revWalk.lookupCommit(jgit.resolve(id))
        val tipCommit = revWalk.lookupCommit(jgit.resolve(tip.id))
        return revWalk.isMergedInto(baseCommit, tipCommit)
    }
}