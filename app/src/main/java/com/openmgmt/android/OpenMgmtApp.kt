package com.openmgmt.android

import android.app.Application
import com.openmgmt.android.auth.AuthManager
import com.openmgmt.android.data.AppDatabase
import com.openmgmt.android.data.MainRepository
import com.openmgmt.android.sync.SyncCredentials
import com.openmgmt.android.sync.SyncManager

class OpenMgmtApp : Application() {

    lateinit var repository: MainRepository
        private set

    lateinit var authManager: AuthManager
        private set

    lateinit var syncManager: SyncManager
        private set

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.get(this)
        repository = MainRepository(database)
        authManager = AuthManager(this)
        syncManager = SyncManager(
            repository = repository,
            authManager = authManager,
            credentials = SyncCredentials(this),
        )
    }
}
