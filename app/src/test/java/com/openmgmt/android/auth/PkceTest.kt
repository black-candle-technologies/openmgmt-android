package com.openmgmt.android.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the PKCE helpers and OAuth defaults.
 *
 * These run on the JVM (no Android framework), which is why [Pkce] avoids
 * android.util.Base64 in favor of java.util.Base64.
 */
class PkceTest {

    private val urlSafe = Regex("^[A-Za-z0-9_-]+$")

    @Test
    fun `codeChallenge matches RFC 7636 Appendix B test vector`() {
        // Official test vector from RFC 7636, Appendix B.
        val verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", Pkce.codeChallenge(verifier))
    }

    @Test
    fun `codeChallenge is deterministic`() {
        val verifier = Pkce.codeVerifier()
        assertEquals(Pkce.codeChallenge(verifier), Pkce.codeChallenge(verifier))
    }

    @Test
    fun `codeVerifier is 43 url-safe chars`() {
        // 32 random bytes -> base64url without padding = 43 chars (RFC 7636 §4.1).
        val verifier = Pkce.codeVerifier()
        assertEquals(43, verifier.length)
        assertTrue(urlSafe.matches(verifier))
    }

    @Test
    fun `codeVerifier is unique per call`() {
        assertNotEquals(Pkce.codeVerifier(), Pkce.codeVerifier())
    }

    @Test
    fun `codeChallenge output is 43 url-safe chars`() {
        // SHA-256 output is 32 bytes -> same 43-char encoding as the verifier.
        val challenge = Pkce.codeChallenge(Pkce.codeVerifier())
        assertEquals(43, challenge.length)
        assertTrue(urlSafe.matches(challenge))
    }

    @Test
    fun `state is 22 url-safe chars and unique`() {
        // 16 random bytes -> base64url without padding = 22 chars.
        val first = Pkce.state()
        assertEquals(22, first.length)
        assertTrue(urlSafe.matches(first))
        assertNotEquals(first, Pkce.state())
    }

    @Test
    fun `OAuthConfig defaults match the deployed auth service`() {
        val config = OAuthConfig()
        assertEquals("https://auth.blackcandletech.com", config.issuer)
        assertEquals("com.openmgmt.android:/oauth2/callback", config.redirectUri)
        assertEquals("identity", config.scope)
    }
}
