package com.gameloader.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.gameloader.app.R
import com.gameloader.app.core.AppItem
import com.gameloader.app.core.CloneManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var progress: ProgressBar
    private lateinit var adapter: GameAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        recycler = findViewById(R.id.recycler)
        swipe = findViewById(R.id.swipe)
        progress = findViewById(R.id.progress)

        adapter = GameAdapter(
            onClone = { item -> doClone(item) },
            onLaunch = { item -> CloneManager.launch(item.packageName) }
        )
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        swipe.setOnRefreshListener { loadGames() }

        findViewById<FloatingActionButton>(R.id.fab_cloned).setOnClickListener {
            startActivity(Intent(this, ClonedListActivity::class.java))
        }

        loadGames()
    }

    private fun loadGames() {
        progress.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            val list = CloneManager.listHostGames(this@MainActivity)
            withContext(Dispatchers.Main) {
                adapter.submit(list)
                progress.visibility = View.GONE
                swipe.isRefreshing = false
            }
        }
    }

    private fun doClone(item: AppItem) {
        progress.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            val result = CloneManager.cloneFromHost(item.packageName)
            withContext(Dispatchers.Main) {
                progress.visibility = View.GONE
                if (result.success) {
                    Toast.makeText(this@MainActivity, "Cloned: ${item.name}", Toast.LENGTH_SHORT).show()
                    item.isCloned = true
                    adapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this@MainActivity, "Fail: ${result.msg}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}