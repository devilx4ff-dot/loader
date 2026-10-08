package com.gameloader.app

import android.app.Application
import android.content.Context
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackbox.app.configuration.ClientConfiguration
import com.gameloader.app.core.CloneManager

class LoaderApp : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        try {
            BlackBoxCore.get().doAttachBaseContext(base, object : ClientConfiguration() {
                override fun getHostPackageName(): String = base.packageName

                override fun isEnableDaemonService(): Boolean = true

                // false = root apps can see real root
                override fun isHideRoot(): Boolean = true
            })
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    override fun onCreate() {
        super.onCreate()
        BlackBoxCore.get().doCreate()
        try {
            if (!BlackBoxCore.get().isInstallGms(CloneManager.SPACE_ID)) {
                BlackBoxCore.get().installGms(CloneManager.SPACE_ID)
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }
}