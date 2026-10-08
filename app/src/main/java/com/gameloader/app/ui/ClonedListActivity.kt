package com.gameloader.app.ui

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.gameloader.app.R
import com.gameloader.app.core.AppItem
import com.gameloader.app.core.CloneManager
import com.gameloader.app.core.CloneStage
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClonedListActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var progress: ProgressBar
    private lateinit var emptyText: TextView
    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var adapter: GameAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cloned)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Clone Apps"
        toolbar.setNavigationOnClickListener { finish() }

        recycler = findViewById(R.id.recycler)
        progress = findViewById(R.id.progress)
        emptyText = findViewById(R.id.empty_text)
        swipe = findViewById(R.id.swipe)

        adapter = GameAdapter(
            onClone = { item -> doClone(item) },
            onLaunch = { item -> CloneManager.launch(item.packageName) },
            onUninstall = { item ->
                CloneManager.uninstall(item.packageName)
                Toast.makeText(this, "Removed ${item.name}", Toast.LENGTH_SHORT).show()
                load()
            },
            showUninstall = true
        )
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        swipe.setOnRefreshListener { load() }

        load()
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_cloned, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                load()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun load() {
        progress.visibility = View.VISIBLE
        emptyText.visibility = View.GONE
        swipe.isRefreshing = false

        CoroutineScope(Dispatchers.IO).launch {
            val list = try {
                CloneManager.listAllWhitelist(this@ClonedListActivity)
            } catch (e: Exception) {
                emptyList()
            }
            withContext(Dispatchers.Main) {
                adapter.submit(list)
                progress.visibility = View.GONE
                swipe.isRefreshing = false
                if (list.isEmpty()) {
                    emptyText.visibility = View.VISIBLE
                    emptyText.text =
                        "No supported apps installed on this device.\n" +
                        "Install BGMI, Twitter, Facebook or Chrome first."
                } else {
                    emptyText.visibility = View.GONE
                }
            }
        }
    }

    // ---------- clone with bottom sheet progress ----------

    private fun doClone(item: AppItem) {
        val dialog = BottomSheetDialog(this, R.style.Theme_GameLoader_BottomSheet)
        val view = layoutInflater.inflate(R.layout.dialog_clone_progress, null)
        dialog.setContentView(view)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.behavior.peekHeight = 400
        dialog.show()

        val icon = view.findViewById<ImageView>(R.id.sheetIcon)
        val title = view.findViewById<TextView>(R.id.sheetTitle)
        val subtitle = view.findViewById<TextView>(R.id.sheetSubtitle)
        val percent = view.findViewById<TextView>(R.id.sheetPercent)
        val bar = view.findViewById<ProgressBar>(R.id.sheetBar)
        val bytes = view.findViewById<TextView>(R.id.sheetBytes)
        val sheetRoot = view.findViewById<View>(R.id.sheetRoot)

        icon.setImageDrawable(item.icon)
        title.text = item.name

        // entrance animation
        sheetRoot.alpha = 0f
        sheetRoot.translationY = 80f
        sheetRoot.animate().alpha(1f).translationY(0f)
            .setDuration(280).setInterpolator(DecelerateInterpolator()).start()

        CoroutineScope(Dispatchers.IO).launch {
            val result = CloneManager.cloneWithObb(item.packageName) { stage, pct, detail ->
                runOnUiThread {
                    when (stage) {
                        CloneStage.InstallingApk -> {
                            title.text = "Installing ${item.name}"
                            subtitle.text = "Preparing package…"
                            bar.isIndeterminate = true
                            percent.text = ""
                            bytes.text = ""
                        }
                        CloneStage.CopyingObb -> {
                            title.text = "Downloading assets"
                            subtitle.text = detail
                            bar.isIndeterminate = false
                            bar.progress = pct
                            percent.text = "$pct%"

                            // pull out the "X MB / Y MB" portion from detail
                            val slash = detail.indexOf("/")
                            if (slash > 0) {
                                bytes.text = detail.substring(slash + 1).trim()
                            } else {
                                bytes.text = detail
                            }
                        }
                        CloneStage.Done -> {
                            bar.isIndeterminate = false
                            bar.progress = 100
                            percent.text = "100%"
                            title.text = "Ready"
                            subtitle.text = "Launching available"
                            icon.setImageResource(android.R.drawable.checkbox_on_background)
                        }
                        CloneStage.Failed -> {
                            bar.isIndeterminate = false
                            title.text = "Failed"
                            subtitle.text = detail
                            icon.setImageResource(android.R.drawable.stat_notify_error)
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                // exit animation
                sheetRoot.animate().alpha(0f).translationY(80f)
                    .setDuration(280)
                    .withEndAction {
                        if (dialog.isShowing) dialog.dismiss()
                    }.start()

                if (result.success) {
                    Toast.makeText(this@ClonedListActivity,
                        "Cloned: ${item.name}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@ClonedListActivity,
                        "Failed: ${result.msg}", Toast.LENGTH_LONG).show()
                }
                load()
            }
        }
    }
}