package com.openmgmt.android.update

fun isNewerVersion(installed: Long, candidate: Long): Boolean = candidate > installed

// TODO: pin the production signing certificate once release signing is configured.
// CI currently builds unsigned release APKs; never substitute a debug certificate.
const val UPDATE_SIGNER_SHA256 = ""
