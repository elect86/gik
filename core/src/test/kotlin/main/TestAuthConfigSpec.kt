package main

import kotlin.test.Test

class TestAuthConfigSpec {

    @Test
    fun `getHardcodedCreds returns creds if username and password are set with properties`() {
        val props = mapOf(AuthConfig.USERNAME_OPTION to "myuser", AuthConfig.PASSWORD_OPTION to "mypass")
        assert(AuthConfig.fromMap(props).hardcodedCreds == Credentials("myuser", "mypass"))
    }

    @Test
    fun `getHardcodedCreds returns creds if username and password are set with env`() {
        val env = mapOf(AuthConfig.USERNAME_ENV_VAR to "myuser", AuthConfig.PASSWORD_ENV_VAR to "mypass")
        assert(AuthConfig.fromMap(emptyMap(), env).hardcodedCreds == Credentials("myuser", "mypass"))
    }

    @Test
    fun `getHardcodedCreds returns creds if username is set and password is not`() {
        val props = mapOf(AuthConfig.USERNAME_OPTION to "myuser")
        assert(AuthConfig.fromMap(props).hardcodedCreds == Credentials("myuser"))
    }

    @Test
    fun `getHardcodedCreds are not populated if username is not set`() =
        assert(!AuthConfig.fromMap(emptyMap()).hardcodedCreds.isPopulated)
}