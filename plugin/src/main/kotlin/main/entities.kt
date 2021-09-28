package main

import org.eclipse.jgit.lib.PersonIdent
import java.time.ZonedDateTime
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.transport.RemoteConfig
import java.time.Instant
import java.time.ZoneOffset
import org.eclipse.jgit.api.Status as JgitStatus


val RevCommit.dateTime: ZonedDateTime
    get() = ZonedDateTime.ofInstant(Instant.ofEpochSecond(commitTime.toLong()), committerIdent.timeZone?.toZoneId() ?: ZoneOffset.UTC)

data class Credentials(
    val username: String? = null,
    val password: String? = null) {

    //    String getUsername() {
    //        return username ?: ''
    //    }
    //
    //    String getPassword() {
    //        return password ?: ''
    //    }

    val isPopulated: Boolean
        get() = username != null
}

data class DiffEntry(

    /** General type of change indicated by the patch. */
    val changeType: ChangeType,

    /**
     * Get the old name associated with this file.
     * <p>
     * The meaning of the old name can differ depending on the semantic meaning
     * of this patch:
     * <ul>
     * <li><i>file add</i>: always <code>/dev/null</code></li>
     * <li><i>file modify</i>: always {@link #newPath}</li>
     * <li><i>file delete</i>: always the file being deleted</li>
     * <li><i>file copy</i>: source file the copy originates from</li>
     * <li><i>file rename</i>: source file the rename originates from</li>
     * </ul>
     *
     */
    val oldPath: String,

    /**
     * Get the new name associated with this file.
     * <p>
     * The meaning of the new name can differ depending on the semantic meaning
     * of this patch:
     * <ul>
     * <li><i>file add</i>: always the file being created</li>
     * <li><i>file modify</i>: always {@link #oldPath}</li>
     * <li><i>file delete</i>: always <code>/dev/null</code></li>
     * <li><i>file copy</i>: destination file the copy ends up at</li>
     * <li><i>file rename</i>: destination file the rename ends up at</li>
     * </ul>
     *
     */
    val newPath: String) {

    /** General type of change a single file-level patch describes. */
    enum class ChangeType { add, modify, delete, rename, copy }
}

data class Person(
    /** Name of person. */
    val name: String,

    /** Email address of person. */
    val email: String) {

    constructor(indent: PersonIdent) : this(indent.name, indent.emailAddress)
}

data class Ref(
    /** The fully qualified name of this ref. */
    override val fullName: String) : FullName {

    /** The simple name of the ref. */
    val name: String
        get() = Repository.shortenRefName(fullName)
}

data class Remote(

    /** Name of the remote. */
    val name: String,

    /** URL to fetch from. */
    val url: String?,

    /** URL to push to. */
    val pushUrl: String? = null,

    /** Specs to fetch from the remote. */
    val fetchRefSpecs: List<String>,

    /** Specs to push to the remote. */
    val pushRefSpecs: List<String> = emptyList(),

    /** Whether or not pushes will mirror the repository. */
    val mirror: Boolean = false) {

    /**
     * Converts a JGit remote to a Grgit remote.
     * @param rc the remote config to convert
     * @return the converted remote
     */
    constructor(rc: RemoteConfig) : this(
        rc.name,
        rc.urIs.getOrNull(0)?.toString(),
        rc.pushURIs.getOrNull(0)?.toString(),
        rc.fetchRefSpecs.map { it.toString() },
        rc.pushRefSpecs.map { it.toString() },
        rc.isMirror)
}

typealias ChangesMap = Map<String, Set<String>>

data class Status(
    val staged: Changes = Changes(),
    val unstaged: Changes = Changes(),
    val conflicts: Set<String> = emptySet()) {

    /**
     * Converts a JGit status to a Grgit status.
     * @param jgitStatus the status to convert
     * @return the converted status
     */
    constructor(jgitStatus: JgitStatus) : this(
        Changes(jgitStatus.added, jgitStatus.changed, jgitStatus.removed),
        Changes(jgitStatus.untracked, jgitStatus.modified, jgitStatus.missing),
        jgitStatus.conflicting)

    data class Changes(
        val added: Set<String> = emptySet(),
        val modified: Set<String> = emptySet(),
        val removed: Set<String> = emptySet()) {

        /** Gets all changed files. */
        val allChanges: Set<String>
            get() = added + modified + removed

        companion object {
            operator fun invoke(args: ChangesMap = emptyMap()): Changes {
                val invalidArgs = args.keys - listOf("added", "modified", "removed")
                if (invalidArgs.isEmpty())
                    throw IllegalArgumentException("Following keys are not supported: $invalidArgs")
                return Changes(args["added"] ?: emptySet(),
                               args["modified"] ?: emptySet(),
                               args["removed"] ?: emptySet())
            }
        }
    }

    /**
     * Whether the repository has any changes or conflicts.
     * @return {@code true} if there are no changes either staged or unstaged or
     * any conflicts, {@code false} otherwise
     */
    val isClean: Boolean
        get() = (staged.allChanges + unstaged.allChanges + conflicts).isEmpty()

    companion object {
        operator fun invoke(args: Map<String, Any> = emptyMap()): Status {
            val invalidArgs = args.keys - listOf("staged", "unstaged", "conflicts")
            if (invalidArgs.isEmpty())
                throw IllegalArgumentException("Following keys are not supported: $invalidArgs")
            return Status(Changes(args["staged"] as? ChangesMap ?: emptyMap()),
                          Changes(args["unstaged"] as? ChangesMap ?: emptyMap()),
                          conflicts = args["conflicts"] as? Set<String> ?: emptySet())
        }
    }
}

data class Tag(
    /** The commit this tag points to. */
    var commit: Commit,

    /** The person who created the tag. */
    var tagger: Person? = null,

    /** The full name of this tag. */
    override val fullName: String,

    /** The full tag message. */
    var fullMessage: String? = null,

    /** The shortened tag message. */
    var shortMessage: String? = null,

    /** The time the commit was created with the time zone of the committer, if available. */
    var dateTime: ZonedDateTime? = null) : FullName {

    /** The simple name of this tag. */
    val name: String
        get() = Repository.shortenRefName(fullName)
}