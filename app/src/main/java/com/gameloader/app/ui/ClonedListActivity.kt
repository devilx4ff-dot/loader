package com.gameloader.app.ui

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gameloader.app.R
import com.gameloader.app.core.CloneManager
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClonedListActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var progress: ProgressBar
    private lateinit var adapter: GameAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cloned)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        recycler = findViewById(R.id.recycler)
        progress = findViewById(R.id.progress)

        adapter = GameAdapter(
            onClone = { },
            onLaunch = { item -> CloneManager.launch(item.packageName) },
            onUninstall = { item ->
                CloneManager.uninstall(item.packageName)
                Toast.makeText(this, "Removed ${item.name}", Toast.LENGTH_SHORT).show()
                loadCloned()
            },
            showUninstall = true
        )
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        loadCloned()
    }

    private fun loadCloned() {
        progress.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            val list = CloneManager.listCloned(this@ClonedListActivity)
            withContext(Dispatchers.Main) {
                adapter.submit(list)
                progress.visibility = View.GONE
            }
        }
    }
}