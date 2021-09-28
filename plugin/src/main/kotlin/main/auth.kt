package main

import org.eclipse.jgit.lib.Config
import org.eclipse.jgit.storage.file.FileBasedConfig
import org.eclipse.jgit.util.FS
import org.eclipse.jgit.util.SystemReader
import org.eclipse.jgit.util.time.MonotonicClock
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

/**
 * Stores configuration options for how to authenticate with remote
 * repositories.
 * @see <a href="http://ajoberstar.org/grgit/grgit-authentication.html">grgit-authentication</a>
 */
class AuthConfig(
    private val props: Map<String, String>,
    private val env: Map<String, String>) {

    init {
        GrgitSystemReader.install()
        logger.debug("If SSH is used, the following external command (if non-null) will be used instead of JSch: {}", SystemReader.getInstance().getenv("GIT_SSH"))
    }

    /**
     * Constructs and returns a {@link Credentials} instance reflecting the
     * settings in the system properties.
     * @return a credentials instance reflecting the settings in the system
     * properties, or, if the username isn't set, {@code null}
     */
    val hardcodedCreds: Credentials
        get() = Credentials(
            username = props[USERNAME_OPTION] ?: env[USERNAME_ENV_VAR],
            password = props[PASSWORD_OPTION] ?: env[PASSWORD_ENV_VAR])

    companion object {

        private val logger = LoggerFactory.getLogger(AuthConfig::class.java)

        val USERNAME_OPTION = "org.ajoberstar.grgit.auth.username"
        val PASSWORD_OPTION = "org.ajoberstar.grgit.auth.password"

        val USERNAME_ENV_VAR = "GRGIT_USER"
        val PASSWORD_ENV_VAR = "GRGIT_PASS"

        /**
         * Factory method to construct an authentication configuration from the
         * given properties and environment.
         * @param properties the properties to use in this configuration
         * @param env the environment vars to use in this configuration
         * @return the constructed configuration
         * @throws IllegalArgumentException if force is set to an invalid option
         */
        fun fromMap(props: Map<String, String>, env: Map<String, String> = emptyMap()) = AuthConfig(props, env)

        /**
         * Factory method to construct an authentication configuration from the
         * current system properties and environment variables.
         * @return the constructed configuration
         * @throws IllegalArgumentException if force is set to an invalid option
         */
        fun fromSystem(): AuthConfig = fromMap(System.getProperties().toMap() as Map<String, String>, System.getenv())
    }
}

class GrgitSystemReader(
    private val delegate: SystemReader,
    private val gitSsh: String?) : SystemReader() {

    override fun getHostname(): String = delegate.hostname

    override fun getenv(variable: String?): String? {
        val value = delegate.getenv(variable)
        return when {
            "GIT_SSH" == variable && value == null -> gitSsh
            else -> value
        }
    }

    override fun getProperty(key: String?): String = delegate.getProperty(key)
    override fun openJGitConfig(parent: Config?, fs: FS?): FileBasedConfig = delegate.openJGitConfig(parent, fs)
    override fun openUserConfig(parent: Config?, fs: FS?): FileBasedConfig = delegate.openUserConfig(parent, fs)
    override fun openSystemConfig(parent: Config?, fs: FS?): FileBasedConfig = delegate.openSystemConfig(parent, fs)
    override fun getCurrentTime(): Long = delegate.currentTime
    override fun getClock(): MonotonicClock = delegate.clock
    override fun getTimezone(`when`: Long): Int = delegate.getTimezone(`when`)
    override fun getTimeZone(): TimeZone = delegate.timeZone
    override fun getLocale(): Locale = delegate.locale
    override fun getSimpleDateFormat(pattern: String?): SimpleDateFormat = delegate.getSimpleDateFormat(pattern)
    override fun getSimpleDateFormat(pattern: String?, locale: Locale?): SimpleDateFormat = delegate.getSimpleDateFormat(pattern, locale)
    override fun getDateTimeInstance(dateStyle: Int, timeStyle: Int): DateFormat = delegate.getDateTimeInstance(dateStyle, timeStyle)
    override fun isWindows(): Boolean = delegate.isWindows
    override fun isMacOS(): Boolean = delegate.isWindows
    override fun checkPath(path: String?) = delegate.checkPath(path)
    override fun checkPath(path: ByteArray?) = delegate.checkPath(path)


    companion object {

        private val PATH_SPLITTER = Pattern.compile(Pattern.quote(File.pathSeparator))

        fun install() {
            val current = SystemReader.getInstance()

            val gitSsh = findExecutable("ssh") ?: findExecutable("plink")

            val grgit = GrgitSystemReader(current, gitSsh)
            SystemReader.setInstance(grgit)
        }

        private fun findExecutable(exe: String): String? {
            val extensions = System.getenv("PATHEXT")?.split(PATH_SPLITTER) ?: emptyList()
            return PATH_SPLITTER.split(System.getenv("PATH"))
                .map(Paths::get)
                .flatMap { dir ->
                    // assume PATHEXT is only set on Windows
                    if (extensions.isEmpty())
                        listOf(dir.resolve(exe))
                    else
                        extensions.map { dir.resolve(exe + it) }
                }
                .filter(Files::isExecutable)
                .map { it.toAbsolutePath().toString() }
                .firstOrNull()
        }
    }
}
