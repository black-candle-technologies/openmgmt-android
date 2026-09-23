package com.openmgmt.android.auth

/**
 * OAuth configuration for Black Candle account sign-in.
 *
 * The Android app is a native OAuth client (RFC 8252): system browser via
 * Custom Tabs, PKCE S256, and a custom-scheme redirect handled by
 * [MainActivity]. The client_id is not configured — the app self-registers
 * via Dynamic Client Registration (RFC 7591) on first sign-in and persists
 * the issued id (see AuthManager).
 */
data class OAuthConfig(
    val issuer: String = "https://auth.blackcandletech.com",
    /** Custom-scheme redirect registered in AndroidManifest.xml. */
    val redirectUri: String = "com.openmgmt.android:/oauth2/callback",
    val scope: String = "identity",
)
