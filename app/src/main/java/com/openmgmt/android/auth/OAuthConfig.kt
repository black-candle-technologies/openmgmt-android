package com.openmgmt.android.auth

/**
 * OAuth configuration for Black Candle account sign-in.
 *
 * The Android app is a native OAuth client (RFC 8252): system browser via
 * Custom Tabs, PKCE S256, and a custom-scheme redirect handled by
 * [MainActivity]. The CIMD client metadata document must list this
 * redirect URI for the token exchange to succeed.
 */
data class OAuthConfig(
    val issuer: String = "https://auth.blackcandletech.com",
    val clientId: String = "https://blackcandletech.com/oauth/openmgmt-android.json",
    /** Custom-scheme redirect registered in AndroidManifest.xml. */
    val redirectUri: String = "com.openmgmt.android:/oauth2/callback",
    val scope: String = "identity",
)
