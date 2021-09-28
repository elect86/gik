package main

import org.eclipse.jgit.api.GitCommand
import org.eclipse.jgit.api.TransportCommand
import org.eclipse.jgit.api.errors.TransportException
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path

val Any.asFile: File
    get() = when (this) {
        is File -> this
        is Path -> toFile()
        else -> File(toString())
    }


/**
 * Utility class that allows a JGit {@code TransportCommand} to be configured
 * to use additional authentication options.
 */
object TransportOpUtil {

    private val logger = LoggerFactory.getLogger(TransportOpUtil::class.java)

    /**
     * Configures the given transport command with the given credentials.
     * @param cmd the command to configure
     * @param credentials the hardcoded credentials to use, if not {@code null}
     */
    fun <G : GitCommand<*>, T>configure(cmd: TransportCommand<G, T>, credentials: Credentials?) {
        val config = AuthConfig.fromSystem()
        cmd.setCredentialsProvider(determineCredentialsProvider(config, credentials))
    }

    private fun determineCredentialsProvider(config: AuthConfig?, credentials: Credentials?): CredentialsProvider? {
        val systemCreds = config?.hardcodedCreds
        return when {
            credentials?.isPopulated == true -> {
                logger.info("using hardcoded credentials provided programmatically")
                UsernamePasswordCredentialsProvider(credentials.username, credentials.password)
            }
            systemCreds?.isPopulated == true -> {
                logger.info("using hardcoded credentials from system properties")
                UsernamePasswordCredentialsProvider(systemCreds.username, systemCreds.password)
            }
            else -> null
        }
    }
}

interface FullName {
    val fullName: String
}

class PushException : TransportException {
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
}

operator fun File.plusAssign(text: String) = appendText(text)
val File.text: String
    get() = readText()