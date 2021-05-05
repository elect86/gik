package gik

import org.eclipse.jgit.lib.Repository

/**
 * A branch.
 * @since 0.2.0
 */
data class Branch(

    /** The fully qualified name of this branch. */
    override val fullName: String,

    /** This branch's upstream branch. {@code null} if this branch isn't tracking an upstream. */
    val trackingBranch: Branch? = null) : FullName {

    /** The simple name of the branch. */
    val name: String
        get() = Repository.shortenRefName(fullName)

    /**
     * The tracking status of a branch.
     * @since 0.2.0
     */
    data class Status(
        /** The branch this object is for. */
        val branch: Branch,

        /** The number of commits this branch is ahead of its upstream. */
        val aheadCount: Int,

        /** The number of commits this branch is behind its upstream. */
        val behindCount: Int)
}