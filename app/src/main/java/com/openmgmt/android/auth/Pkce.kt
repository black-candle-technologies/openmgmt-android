package com.openmgmt.android.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/** PKCE S256 helpers (RFC 7636). */
object Pkce {

    // URL-safe Base64 without padding or line wraps. java.util.Base64 is used
    // instead of android.util.Base64 so this object stays pure-JVM and unit
    // testable (API 26+, and minSdk is 26).
    private val encoder: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()

    fun codeVerifier(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return encoder.encodeToString(bytes)
    }

    fun codeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(verifier.toByteArray(Charsets.US_ASCII))
        return encoder.encodeToString(hash)
    }

    fun state(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return encoder.encodeToString(bytes)
    }
}
