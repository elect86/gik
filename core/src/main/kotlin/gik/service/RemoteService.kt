package gik.service

import gik.Remote
import gik.Repo
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.RemoteConfig
import org.eclipse.jgit.transport.URIish

/**
 * Provides support for remote-related operations on a Git repository.
 *
 * <p>
 *   Details of each operation's properties and methods are available on the
 *   doc page for the class. The following operations are supported directly on
 *   this service instance.
 * </p>
 *
 * <ul>
 *   <li>{@link org.ajoberstar.grgit.operation.RemoteAddOp add}</li>
 *   <li>{@link org.ajoberstar.grgit.operation.RemoteListOp list}</li>
 * </ul>
 */
class RemoteService(val repo: Repo) {

    /**
     * Lists remotes in the repository. Returns a list of [grgit.Remote].
     * @see [git-remote Manual Page](http://git-scm.com/docs/git-remote)
     */
    fun list(): List<Remote> =
        RemoteConfig.getAllRemoteConfigs(repo.jgit.repository.config).map { rc ->
            if (rc.urIs.size > 1 || rc.pushURIs.size > 1)
                throw IllegalArgumentException("Grgit does not currently support multiple URLs in remote: [uris: ${rc.urIs}, pushURIs:${rc.pushURIs}]")
            Remote(rc)
        }

    /**
     * Adds a remote to the repository. Returns the newly created [grgit.Remote].
     * If remote with given name already exists, this command will fail.
     * @see [git-remote Manual Page](http://git-scm.com/docs/git-remote)
     * @param[name] Name of the remote
     * @param[url] URL to fetch from
     * @param[pushUrl] URL to push to
     * @param[fetchRefSpecs] Specs to fetch from the remote
     * @param[pushRefSpecs] Specs to push to the remote
     * @param[mirror] Whether or not pushes will mirror the repository
     */
    fun add(name: String,
            url: String?,
            pushUrl: String? = null,
            fetchRefSpecs: List<String> = emptyList(),
            pushRefSpecs: List<String> = emptyList(),
            mirror: Boolean = false): Remote {

        val config = repo.jgit.repository.config
        if (RemoteConfig.getAllRemoteConfigs(config).any { it.name == name })
            throw IllegalStateException("Remote $name already exists.")
        fun toUri(url: String) = URIish(url)
        fun toRefSpec(spec: String) = RefSpec(spec)
        val remote = RemoteConfig(config, name)
        if (url != null && url.isNotBlank())
            remote.addURI(toUri(url))
        if (pushUrl != null)
            remote.addPushURI(toUri(pushUrl))
        remote.fetchRefSpecs = when {
            fetchRefSpecs.isEmpty() -> listOf("+refs/heads/*:refs/remotes/$name/*")
            else -> fetchRefSpecs
        }.map(::toRefSpec)
        remote.pushRefSpecs = pushRefSpecs.map(::toRefSpec)
        remote.isMirror = mirror
        remote.update(config)
        config.save()
        return Remote(remote)
    }
}