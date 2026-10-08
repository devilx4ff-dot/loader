package com.gameloader.app.ui

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.gameloader.app.R
import com.gameloader.app.core.CloneManager

class HomeActivity : AppCompatActivity() {

    private lateinit var brandLogo: ImageView
    private lateinit var heroCard: LinearLayout
    private lateinit var statusRow: LinearLayout
    private lateinit var heroIcon: ImageView
    private lateinit var heroName: TextView
    private lateinit var heroPackage: TextView
    private lateinit var btnStart: AppCompatButton
    private lateinit var statusDot: View
    private lateinit var chipTwitter: LinearLayout
    private lateinit var chipFacebook: LinearLayout

    private val targetPackage = "com.pubg.imobile"

    companion object {
        private const val TAG = "HomeActivity"
        private const val REQ_NOTIFICATIONS = 1001
        private const val REQ_STORAGE = 1002
        private const val REQ_MANAGE_FILES = 1003
        private const val REQ_INSTALL_PACKAGES = 1004
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        brandLogo = findViewById(R.id.brandLogo)
        heroCard = findViewById(R.id.heroCard)
        statusRow = findViewById(R.id.statusRow)
        heroIcon = findViewById(R.id.heroIcon)
        heroName = findViewById(R.id.heroName)
        heroPackage = findViewById(R.id.heroPackage)
        btnStart = findViewById(R.id.btnStart)
        statusDot = findViewById(R.id.statusDot)
        chipTwitter = findViewById(R.id.chipTwitter)
        chipFacebook = findViewById(R.id.chipFacebook)

        runEntranceAnimations()
        startIconBob()
        loadHeroAppInfo()
        wireButtons()

        heroCard.postDelayed({
            requestAllPermissions()
            refreshCloneState()
        }, 600)
    }

    override fun onResume() {
        super.onResume()
        refreshCloneState()
    }

    private fun refreshCloneState() {
        try {
            val cloned = CloneManager.isCloned(targetPackage)
            btnStart.text = if (cloned) "START" else "CLONE"
            Log.d(TAG, "bgmi cloned = $cloned")
        } catch (t: Throwable) {
            Log.w(TAG, "refreshCloneState failed", t)
            btnStart.text = "START"
        }
    }

    // ==================== ANIMATIONS ====================

    private fun runEntranceAnimations() {
        brandLogo.alpha = 0f
        brandLogo.scaleX = 0.85f
        brandLogo.scaleY = 0.85f
        brandLogo.animate().alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(600).setInterpolator(DecelerateInterpolator()).start()

        heroCard.alpha = 0f
        heroCard.translationY = 40f
        heroCard.animate().alpha(1f).translationY(0f)
            .setStartDelay(200).setDuration(550)
            .setInterpolator(DecelerateInterpolator()).start()

        statusRow.alpha = 0f
        statusRow.translationY = 40f
        statusRow.animate().alpha(1f).translationY(0f)
            .setStartDelay(350).setDuration(550)
            .setInterpolator(DecelerateInterpolator()).start()

        chipTwitter.alpha = 0f
        chipTwitter.translationY = 30f
        chipTwitter.animate().alpha(1f).translationY(0f)
            .setStartDelay(500).setDuration(450).start()

        chipFacebook.alpha = 0f
        chipFacebook.translationY = 30f
        chipFacebook.animate().alpha(1f).translationY(0f)
            .setStartDelay(620).setDuration(450).start()
    }

    private fun startIconBob() {
        val anim = ObjectAnimator.ofFloat(heroIcon, "translationY", 0f, -6f, 0f)
        anim.duration = 2200
        anim.repeatCount = ObjectAnimator.INFINITE
        anim.interpolator = DecelerateInterpolator()
        anim.start()
    }

    // ==================== APP INFO ====================

    private fun loadHeroAppInfo() {
        try {
            val pm = packageManager
            val ai = pm.getApplicationInfo(targetPackage, 0)
            heroIcon.setImageDrawable(ai.loadIcon(pm))
            heroName.text = ai.loadLabel(pm).toString().uppercase()
            heroPackage.text = targetPackage
        } catch (_: Exception) {
            // not installed on host — leave placeholders
        }
    }

    // ==================== BUTTONS ====================

    private fun wireButtons() {
        btnStart.setOnClickListener {
            btnStart.animate().scaleX(0.95f).scaleY(0.95f).setDuration(80)
                .withEndAction {
                    btnStart.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                }.start()

            if (!CloneManager.isCloned(targetPackage)) {
                startActivity(Intent(this, ClonedListActivity::class.java))
            } else {
                CloneManager.launch(targetPackage)
            }
        }

        chipTwitter.setOnClickListener {
            val pkg = "com.twitter.android"
            if (CloneManager.isCloned(pkg)) CloneManager.launch(pkg)
            else startActivity(Intent(this, ClonedListActivity::class.java))
        }

        chipFacebook.setOnClickListener {
            val pkg = "com.facebook.katana"
            if (CloneManager.isCloned(pkg)) CloneManager.launch(pkg)
            else startActivity(Intent(this, ClonedListActivity::class.java))
        }
    }

    // ==================== PERMISSIONS ====================

    private fun requestAllPermissions() {
        // 1. notifications (Android 13+)
        if (Build.VERSION.SDK_INT >= 33) {
            val p = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(p), REQ_NOTIFICATIONS)
            }
        }

        // 2. legacy storage permissions (Android 12 and below) — explicitly requested
        //    on Android 11+ these are ignored in favour of MANAGE_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            val perms = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            val need = perms.any {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (need) {
                ActivityCompat.requestPermissions(this, perms, REQ_STORAGE)
            }
        } else {
            // on Android 11+ we still request them so the system sees them — but
            // the real gate is MANAGE_EXTERNAL_STORAGE, handled below
            val perms = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            val need = perms.any {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (need) {
                ActivityCompat.requestPermissions(this, perms, REQ_STORAGE)
            }
        }

        // 3. MANAGE_EXTERNAL_STORAGE (Android 11+) — must be granted via settings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                askForAllFilesAccess()
            }
        }

        // 4. install unknown apps (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                askForInstallPackages()
            }
        }
    }

    private fun askForAllFilesAccess() {
        AlertDialog.Builder(this)
            .setTitle("All Files Access Required")
            .setMessage(
                "Game Loader needs \"All files access\" to copy game OBB files " +
                        "and run cloned apps.\n\n" +
                        "Tap Allow, then turn on the switch on the next screen."
            )
            .setCancelable(true)
            .setPositiveButton("Allow") { _, _ ->
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivityForResult(intent, REQ_MANAGE_FILES)
            }
            .setNegativeButton("Later") { _, _ -> }
            .show()
    }

    private fun askForInstallPackages() {
        AlertDialog.Builder(this)
            .setTitle("Install Unknown Apps Required")
            .setMessage(
                "Game Loader needs permission to install apps so it can clone " +
                        "and install games inside the container.\n\n" +
                        "Tap Allow, then turn on \"Allow from this source\" on the next screen."
            )
            .setCancelable(true)
            .setPositiveButton("Allow") { _, _ ->
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                intent.data = Uri.parse("package:$packageName")
                startActivityForResult(intent, REQ_INSTALL_PACKAGES)
            }
            .setNegativeButton("Later") { _, _ -> }
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQ_MANAGE_FILES -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (Environment.isExternalStorageManager()) {
                        Toast.makeText(this, "All files access granted", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            REQ_INSTALL_PACKAGES -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (packageManager.canRequestPackageInstalls()) {
                        Toast.makeText(this, "Install permission granted", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}