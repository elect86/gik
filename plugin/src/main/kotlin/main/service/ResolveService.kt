package main.service

import main.*
import org.eclipse.jgit.lib.ObjectId

/**
 * Convenience methods to resolve various objects.
 */
class ResolveService(val repo: Repo) {

    /**
     * Resolves an object ID from the given object. Can handle any of the following
     * types:
     *
     * <ul>
     *   <li>{@link org.ajoberstar.grgit.Commit}</li>
     *   <li>{@link org.ajoberstar.grgit.Tag}</li>
     *   <li>{@link org.ajoberstar.grgit.Branch}</li>
     *   <li>{@link org.ajoberstar.grgit.Ref}</li>
     * </ul>
     *
     * @param object the object to resolve
     * @return the corresponding object id
     */
    fun toObjectId(obj: Any?): String? = when (obj) {
        null -> null
        is Commit -> obj.id
        is FullName -> ObjectId.toString(repo.jgit.repository.exactRef(obj.fullName).objectId)
        else -> throwIllegalArgument(obj)
    }

    /**
     * Resolves a commit from the given object. Can handle any of the following
     * types:
     *
     * <ul>
     *   <li>{@link org.ajoberstar.grgit.Commit}</li>
     *   <li>{@link org.ajoberstar.grgit.Tag}</li>
     *   <li>{@link org.ajoberstar.grgit.Branch}</li>
     *   <li>{@link String}</li>
     *   <li>{@link GString}</li>
     * </ul>
     *
     * <p>
     * String arguments can be in the format of any
     * <a href="http://git-scm.com/docs/gitrevisions.html">Git revision string</a>.
     * </p>
     * @param object the object to resolve
     * @return the corresponding commit
     */
    fun toCommit(obj: Any?): Commit? = when (obj) {
        null -> null
        is Commit -> obj
        is Tag -> obj.commit
        is Branch -> repo resolveCommit obj.fullName
        is String -> repo resolveCommit obj
        else -> throwIllegalArgument(obj)
    }

    /**
     * Resolves a branch from the given object. Can handle any of the following
     * types:
     * <ul>
     *   <li>{@link Branch}</li>
     *   <li>{@link String}</li>
     *   <li>{@link GString}</li>
     * </ul>
     * @param object the object to resolve
     * @return the corresponding commit
     */
    fun toBranch(obj: Any?): Branch? = when (obj) {
        null -> null
        is Branch -> obj
        is String -> repo resolveBranch obj
        else -> throwIllegalArgument(obj)
    }

    /**
     * Resolves a branch name from the given object. Can handle any of the following
     * types:
     * <ul>
     *   <li>{@link String}</li>
     *   <li>{@link GString}</li>
     *   <li>{@link Branch}</li>
     * </ul>
     * @param object the object to resolve
     * @return the corresponding branch name
     */
    fun toBranchName(obj: Any?): String? = when (obj) {
        null -> null
        is String -> obj
        is Branch -> obj.fullName
        else -> throwIllegalArgument(obj)
    }

    /**
     * Resolves a tag from the given object. Can handle any of the following
     * types:
     * <ul>
     *   <li>{@link Tag}</li>
     *   <li>{@link String}</li>
     *   <li>{@link GString}</li>
     * </ul>
     * @param object the object to resolve
     * @return the corresponding commit
     */
    fun toTag(obj: Any?): Tag? = when (obj) {
        null -> null
        is Tag -> obj
        is String -> repo resolveTag obj
        else -> throwIllegalArgument(obj)
    }

    /**
     * Resolves a tag name from the given object. Can handle any of the following
     * types:
     * <ul>
     *   <li>{@link String}</li>
     *   <li>{@link GString}</li>
     *   <li>{@link Tag}</li>
     * </ul>
     * @param object the object to resolve
     * @return the corresponding tag name
     */
    fun toTagName(obj: Any?): String? = when (obj) {
        null -> null
        is String -> obj
        is Tag -> obj.fullName
        else -> throwIllegalArgument(obj)
    }

    /**
     * Resolves a revision string that corresponds to the given object. Can
     * handle any of the following types:
     * <ul>
     *   <li>{@link org.ajoberstar.grgit.Commit}</li>
     *   <li>{@link org.ajoberstar.grgit.Tag}</li>
     *   <li>{@link org.ajoberstar.grgit.Branch}</li>
     *   <li>{@link String}</li>
     *   <li>{@link GString}</li>
     * </ul>
     * @param object the object to resolve
     * @return the corresponding commit
     */
    infix fun toRevisionString(obj: Any?): String? = when (obj) {
        null -> null
        is Commit -> obj.id
        is Tag -> obj.fullName
        is Branch -> obj.fullName
        is String -> obj
        else -> throwIllegalArgument(obj)
    }

    private fun throwIllegalArgument(obj: Any): Nothing = throw IllegalArgumentException("Can't handle the following object ($obj) of class (${obj::class})")
}