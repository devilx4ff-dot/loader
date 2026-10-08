package com.gameloader.app.core

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackbox.entity.pm.InstallResult
import java.io.File

object CloneManager {

    const val SPACE_ID = 0

    fun listHostGames(ctx: Context): List<AppItem> {
        val pm = ctx.packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            .filter { isLikelyGame(it) }
            .map {
                AppItem(
                    packageName = it.packageName,
                    name = it.loadLabel(pm).toString(),
                    icon = it.loadIcon(pm),
                    isCloned = isCloned(it.packageName)
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    private fun isLikelyGame(ai: ApplicationInfo): Boolean {
        if (ai.category == ApplicationInfo.CATEGORY_GAME) return true
        val pkg = ai.packageName.lowercase()
        return pkg.contains("game") ||
                pkg.startsWith("com.tencent") ||
                pkg.startsWith("com.mihoyo") ||
                pkg.startsWith("com.hoyoverse") ||
                pkg.startsWith("com.garena") ||
                pkg.startsWith("com.pubg") ||
                pkg.startsWith("com.riotgames") ||
                pkg.startsWith("com.netease") ||
                pkg.startsWith("com.kurogame") ||
                pkg.startsWith("com.yo-star") ||
                pkg.startsWith("com.mobile.legends") ||
                pkg.startsWith("com.vng") ||
                pkg.startsWith("com.roblox") ||
                pkg.startsWith("com.epicgames")
    }

    fun cloneFromHost(packageName: String): InstallResult {
        return BlackBoxCore.get().installPackageAsUser(packageName, SPACE_ID)
    }

    fun cloneFromApk(apkPath: String): InstallResult {
        return BlackBoxCore.get().installPackageAsUser(File(apkPath), SPACE_ID)
    }

    fun launch(packageName: String) {
        BlackBoxCore.get().launchApk(packageName, SPACE_ID)
    }

    fun isCloned(packageName: String): Boolean {
        return BlackBoxCore.get().isInstalled(packageName, SPACE_ID)
    }

    fun uninstall(packageName: String) {
        BlackBoxCore.get().uninstallPackageAsUser(packageName, SPACE_ID)
    }

    fun listCloned(ctx: Context): List<AppItem> {
        val pm = ctx.packageManager
        return BlackBoxCore.get().getInstalledPackages(0, SPACE_ID)
            .mapNotNull { pkgInfo ->
                try {
                    val ai = pm.getApplicationInfo(pkgInfo.packageName, 0)
                    AppItem(
                        packageName = pkgInfo.packageName,
                        name = ai.loadLabel(pm).toString(),
                        icon = ai.loadIcon(pm),
                        isCloned = true
                    )
                } catch (e: Exception) {
                    null
                }
            }
            .sortedBy { it.name.lowercase() }
    }
}

data class AppItem(
    val packageName: String,
    val name: String,
    val icon: android.graphics.drawable.Drawable?,
    var isCloned: Boolean
)