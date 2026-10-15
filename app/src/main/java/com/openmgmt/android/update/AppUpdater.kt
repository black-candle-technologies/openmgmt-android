package com.openmgmt.android.update

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.ProgressBar
import androidx.activity.ComponentActivity
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/** Release assets are untrusted until both manifest hashes and APK identity validate. */
class AppUpdater(private val activity: ComponentActivity) {
    private val prefs = activity.getSharedPreferences("updates", 0)
    private val mutex = Mutex()
    private var downloading = false
    private val http = OkHttpClient.Builder().callTimeout(5, TimeUnit.MINUTES).build()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private data class Candidate(val version: String, val url: String, val hash: String, val digest: String?, val code: Long)

    private fun fetch(url: String): String {
        require(url.startsWith("https://"))
        return http.newCall(Request.Builder().url(url).header("Accept", "application/vnd.github+json").build())
            .execute().use { response ->
                check(response.isSuccessful) { "HTTP ${response.code}" }
                requireNotNull(response.body).string()
            }
    }
    private fun JsonObject.text(key: String) = this[key]?.jsonPrimitive?.contentOrNull

    fun check(manual: Boolean = false) {
        activity.lifecycleScope.launch {
            if (!mutex.tryLock()) return@launch
            try {
                if (!manual && System.currentTimeMillis() - prefs.getLong("lastScan", 0) < 3_600_000) return@launch
                val installed = PackageInfoCompat.getLongVersionCode(activity.packageManager.getPackageInfo(activity.packageName, 0))
                val candidate = withContext(Dispatchers.IO) {
                    val releases = json.parseToJsonElement(fetch("https://api.github.com/repos/black-candle-technologies/openmgmt-android/releases?per_page=10")).jsonArray
                    val candidates = releases.mapNotNull { element ->
                        val release = element.jsonObject
                        if (release["draft"]?.jsonPrimitive?.booleanOrNull == true) return@mapNotNull null
                        val assets = release["assets"]?.jsonArray.orEmpty().map { it.jsonObject }
                        val manifestAsset = assets.find { it.text("name") == "release.json" } ?: return@mapNotNull null
                        val manifest = json.parseToJsonElement(fetch(requireNotNull(manifestAsset.text("browser_download_url")))).jsonObject
                        if (manifest["schema"]?.jsonPrimitive?.intOrNull != 1) return@mapNotNull null
                        val files = manifest["files"]?.jsonArray.orEmpty().map { it.jsonObject }
                        val file = files.find { it.text("abi") == "universal" } ?: files.singleOrNull() ?: return@mapNotNull null
                        val asset = assets.find { it.text("name") == file.text("name") } ?: return@mapNotNull null
                        val code = file["version_code"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
                        val hash = file.text("sha256") ?: return@mapNotNull null
                        require(hash.matches(Regex("[a-fA-F0-9]{64}"))) { "Invalid release checksum" }
                        Candidate(release.text("tag_name").orEmpty().removePrefix("v"), requireNotNull(asset.text("browser_download_url")), hash, asset.text("digest"), code)
                    }
                    prefs.edit().putLong("lastScan", System.currentTimeMillis()).apply()
                    candidates.filter { isNewerVersion(installed, it.code) }.maxByOrNull { it.code }
                }
                if (candidate != null) {
                    AlertDialog.Builder(activity).setTitle("Update available")
                        .setMessage("OpenMGMT ${candidate.version} is ready. Install the update now?")
                        .setPositiveButton("Update") { _, _ -> download(candidate) }
                        .setNegativeButton("Later", null).show()
                } else if (manual) message("No updates available")
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { if (manual) message("Unable to check for updates. Try again later.")
            } finally { mutex.unlock() }
        }
    }

    private fun download(candidate: Candidate) {
        if (downloading) return
        downloading = true
        activity.lifecycleScope.launch {
            val progress = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply { isIndeterminate = true }
            val dialog = AlertDialog.Builder(activity).setTitle("Downloading update…").setView(progress).setCancelable(false).show()
            val file = File(activity.cacheDir, "updates/openmgmt.apk")
            try {
                withContext(Dispatchers.IO) {
                    file.parentFile?.mkdirs()
                    require(candidate.url.startsWith("https://"))
                    http.newCall(Request.Builder().url(candidate.url).build()).execute().use { response ->
                        check(response.isSuccessful)
                        val body = requireNotNull(response.body)
                        val length = body.contentLength()
                        var total = 0L
                        var previous = -1
                        body.byteStream().use { input -> file.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                output.write(buffer, 0, count)
                                total += count
                                val percent = if (length > 0) ((total * 100) / length).toInt().coerceIn(0, 100) else -1
                                if (percent != previous) {
                                    previous = percent
                                    withContext(Dispatchers.Main) { progress.isIndeterminate = false; progress.progress = percent }
                                }
                            }
                        } }
                    }
                    val sha = MessageDigest.getInstance("SHA-256")
                    file.inputStream().use { input ->
                        val buffer = ByteArray(8192)
                        while (true) { val n = input.read(buffer); if (n < 0) break; sha.update(buffer, 0, n) }
                    }
                    val actual = sha.digest().hex()
                    check(actual.equals(candidate.hash, true)) { "Manifest checksum mismatch" }
                    candidate.digest?.let { check(it.startsWith("sha256:") && actual.equals(it.removePrefix("sha256:"), true)) { "Asset digest mismatch" } }
                    @Suppress("DEPRECATION")
                    val info = activity.packageManager.getPackageArchiveInfo(file.path,
                        if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES)
                    check(info != null && info.packageName == activity.packageName && PackageInfoCompat.getLongVersionCode(info) == candidate.code) { "APK identity mismatch" }
                    if (UPDATE_SIGNER_SHA256.isEmpty()) {
                        Log.w("OpenMGMTUpdater", "TODO: production signer pin not configured; certificate pin check skipped")
                    } else {
                        @Suppress("DEPRECATION")
                        val signatures = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures
                        check(!signatures.isNullOrEmpty() && signatures.all {
                            MessageDigest.getInstance("SHA-256").digest(it.toByteArray()).hex().equals(UPDATE_SIGNER_SHA256, true)
                        }) { "APK signer mismatch" }
                    }
                }
                dialog.dismiss()
                install(file)
            } catch (e: CancellationException) { file.delete(); throw e
            } catch (e: Exception) { file.delete(); message("Update could not be verified or installed. Please try again.")
            } finally { downloading = false; dialog.dismiss() }
        }
    }
    private fun install(file: File) {
        if (!activity.packageManager.canRequestPackageInstalls()) {
            AlertDialog.Builder(activity).setTitle("Allow app updates")
                .setMessage("Allow OpenMGMT to install updates, then check for updates again.")
                .setPositiveButton("Settings") { _, _ ->
                    activity.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${activity.packageName}")))
                }.setNegativeButton("Later", null).show()
        } else {
            val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.updates", file)
            activity.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        }
    }
    private fun message(text: String) { AlertDialog.Builder(activity).setMessage(text).setPositiveButton("OK", null).show() }
    private fun ByteArray.hex() = joinToString("") { "%02x".format(it) }
}
