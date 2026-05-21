package com.liongate.budget

import android.app.Application
import com.liongate.budget.api.OpenClawApiServer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class BudgetApplication : Application() {

    @Inject
    lateinit var openClawApiServer: OpenClawApiServer

    override fun onCreate() {
        super.onCreate()
        openClawApiServer.startServer()
    }

    override fun onTerminate() {
        super.onTerminate()
        openClawApiServer.stopServer()
    }
}
