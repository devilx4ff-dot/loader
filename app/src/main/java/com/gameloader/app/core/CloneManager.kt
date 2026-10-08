package com.gameloader.app.core

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackbox.core.env.BEnvironment
import top.niunaijun.blackbox.entity.pm.InstallResult
import java.io.File

object CloneManager {

    const val SPACE_ID = 0

    private val ALLOWED_PACKAGES = setOf(
        "com.pubg.imobile",       // BGMI
        "com.facebook.katana",    // Facebook
        "com.twitter.android",    // Twitter
        "com.android.chrome",     // Chrome
        "com.android.vending"
    )
    /**
     * Returns all whitelisted apps that are installed on the HOST, with the
     * isCloned flag reflecting whether they're already cloned in the container.
     */
    fun listAllWhitelist(ctx: Context): List<AppItem> {
        val pm = ctx.packageManager
        val out = mutableListOf<AppItem>()
        for (pkg in ALLOWED_PACKAGES) {
            try {
                val ai = pm.getApplicationInfo(pkg, 0)
                out.add(
                    AppItem(
                        packageName = pkg,
                        name = ai.loadLabel(pm).toString(),
                        icon = ai.loadIcon(pm),
                        isCloned = isCloned(pkg)
                    )
                )
            } catch (_: Exception) {
                // app not installed on host — skip
            }
        }
        return out.sortedBy { it.name.lowercase() }
    }
    fun listHostGames(ctx: Context): List<AppItem> {
        val pm = ctx.packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.packageName != ctx.packageName }
            .filter { isSupportedApp(it) }
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

    private fun isSupportedApp(ai: ApplicationInfo): Boolean {
        return ai.packageName in ALLOWED_PACKAGES
    }

    fun cloneWithObb(
        packageName: String,
        onProgress: (stage: CloneStage, percent: Int, detail: String) -> Unit
    ): InstallResult {
        onProgress(CloneStage.InstallingApk, 0, "Installing APK…")

        val result = BlackBoxCore.get().installPackageAsUser(packageName, SPACE_ID)
        if (!result.success) {
            onProgress(CloneStage.Failed, 0, result.msg ?: "Install failed")
            return result
        }

        onProgress(CloneStage.CopyingObb, 0, "Scanning OBB…")

        BlackBoxCore.get().copyObbToVirtual(packageName, SPACE_ID,
            object : BEnvironment.ObbCopyListener {
                override fun onStart(totalFiles: Int, totalBytes: Long) {
                    onProgress(CloneStage.CopyingObb, 0,
                        "Copying ${totalFiles} file(s), ${humanBytes(totalBytes)}")
                }

                override fun onProgress(filesDone: Int, bytesDone: Long,
                                        totalBytes: Long, currentFile: String) {
                    val pct = if (totalBytes > 0)
                        ((bytesDone * 100) / totalBytes).toInt() else 0
                    onProgress(CloneStage.CopyingObb, pct,
                        "$pct%  •  ${humanBytes(bytesDone)} / ${humanBytes(totalBytes)}")
                }

                override fun onDone(success: Boolean, filesDone: Int, bytesDone: Long) {
                    if (success) {
                        onProgress(CloneStage.Done, 100, "OBB ready")
                    } else {
                        onProgress(CloneStage.Failed, 0, "OBB copy failed")
                    }
                }
            })

        return result
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
            .filter { it.packageName in ALLOWED_PACKAGES }   // <-- only your 4 apps
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

    private fun humanBytes(bytes: Long): String {
        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024
        return when {
            bytes >= gb -> String.format("%.2f GB", bytes / gb)
            bytes >= mb -> String.format("%.1f MB", bytes / mb)
            bytes >= kb -> String.format("%.1f KB", bytes / kb)
            else -> "$bytes B"
        }
    }
}

enum class CloneStage { InstallingApk, CopyingObb, Done, Failed }

data class AppItem(
    val packageName: String,
    val name: String,
    val icon: android.graphics.drawable.Drawable?,
    var isCloned: Boolean
)