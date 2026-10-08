package com.gameloader.app

import android.app.Application
import android.content.Context
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackbox.app.configuration.ClientConfiguration

class LoaderApp : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        try {
            BlackBoxCore.get().doAttachBaseContext(base, object : ClientConfiguration() {
                override fun getHostPackageName(): String = base.packageName

                override fun isEnableDaemonService(): Boolean = true

                // false = root apps can see real root
                override fun isHideRoot(): Boolean = false
            })
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    override fun onCreate() {
        super.onCreate()
        BlackBoxCore.get().doCreate()
    }
}