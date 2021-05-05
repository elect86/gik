package gik.service

import gik.Person
import gik.Repo
import gik.Tag
import org.eclipse.jgit.lib.PersonIdent

class TagService(val repo: Repo) {

    /**
     * Lists tags in the repository. Returns a list of {@link Tag}.
     * @see [git-tag Manual Page](http://git-scm.com/docs/git-tag)
     */
    fun list(): List<Tag> {
        val cmd = repo.jgit.tagList()
        return cmd.call().map {
            repo resolveTag it
        }
    }

    /**
     * Adds a tag to the repository. Returns the newly created [Tag].
     * @see [git-tag Manual Page](http://git-scm.com/docs/git-tag)
     * @param[name] The name of the tag to create
     * @param[message] The message to put on the tag
     * @param[tagger] The person who created the tag
     * @param[annotate] if an annotated tag should be created
     * @param[force] to overwrite an existing tag
     * @param[pointsTo] The commit the tag should point to
     */
    fun add(name: String,
            message: String? = null,
            tagger: Person? = null,
            annotate: Boolean = true,
            force: Boolean = false,
            pointsTo: Any? = null): Tag {

        val cmd = repo.jgit.tag()
        cmd.name = name
        cmd.message = message
        if (tagger != null)
            cmd.tagger = PersonIdent(tagger.name, tagger.email)
        cmd.isAnnotated = annotate
        cmd.isForceUpdate = force
        if (pointsTo != null) {
            val revStr = ResolveService(repo) toRevisionString pointsTo
            cmd.objectId = repo.resolveRevObject(revStr)
        }

        val ref = cmd.call()
        return repo resolveTag ref
    }

    /**
     * Removes one or more tags from the repository. Returns a list of the fully qualified tag names that were removed.
     * @see [git-tag Manual Page](http://git-scm.com/docs/git-tag)
     * @param[names] Names of tags to remove
     */
    fun remove(names: List<String> = emptyList()): List<String> =
        repo.jgit.tagDelete()
            .setTags(*names.map { ResolveService(repo).toTagName(it) }.toTypedArray())
            .call()
}