package com.openmgmt.android.update

import java.io.File
import java.util.concurrent.TimeUnit

/** Never reuse a path already handed to the package installer, even across activity instances. */
internal fun newUpdateFile(cacheDir: File, now: Long = System.currentTimeMillis()): File {
    val directory = File(cacheDir, "updates")
    check(directory.isDirectory || directory.mkdirs()) { "Cannot create update directory" }
    // Keep recent downloads: the installer may still be reading one of them.
    directory.listFiles()?.filter {
        it.isFile && it.extension == "apk" && now - it.lastModified() > TimeUnit.DAYS.toMillis(7)
    }?.forEach { it.delete() }
    return File.createTempFile("openmgmt-", ".apk", directory)
}
