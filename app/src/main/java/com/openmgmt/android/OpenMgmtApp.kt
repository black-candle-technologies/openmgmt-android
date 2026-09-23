package com.openmgmt.android

import android.app.Application
import com.openmgmt.android.auth.AuthManager
import com.openmgmt.android.data.AppDatabase
import com.openmgmt.android.sync.SyncManager

class OpenMgmtApp : Application() {

    lateinit var authManager: AuthManager
        private set

    lateinit var syncManager: SyncManager
        private set

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.get(this)
        authManager = AuthManager(this)
        syncManager = SyncManager(
            context = this,
            database = database,
            authManager = authManager,
        )
    }
}
