package com.gameloader.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gameloader.app.R
import com.gameloader.app.core.AppItem

class GameAdapter(
    private val onClone: (AppItem) -> Unit,
    private val onLaunch: (AppItem) -> Unit,
    private val onUninstall: ((AppItem) -> Unit)? = null,
    private val showUninstall: Boolean = false
) : RecyclerView.Adapter<GameAdapter.VH>() {

    private val items = mutableListOf<AppItem>()

    fun submit(list: List<AppItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_game, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.name.text = item.name
        holder.pkg.text = item.packageName
        holder.icon.setImageDrawable(item.icon)

        if (item.isCloned) {
            holder.btnClone.visibility = View.GONE
            holder.btnLaunch.visibility = View.VISIBLE
            holder.btnUninstall.visibility = if (showUninstall) View.VISIBLE else View.GONE
        } else {
            holder.btnClone.visibility = View.VISIBLE
            holder.btnLaunch.visibility = View.GONE
            holder.btnUninstall.visibility = View.GONE
        }

        holder.btnClone.setOnClickListener { onClone(item) }
        holder.btnLaunch.setOnClickListener { onLaunch(item) }
        holder.btnUninstall.setOnClickListener { onUninstall?.invoke(item) }
    }

    override fun getItemCount() = items.size

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val icon: ImageView = v.findViewById(R.id.icon)
        val name: TextView = v.findViewById(R.id.name)
        val pkg: TextView = v.findViewById(R.id.pkg)
        val btnClone: Button = v.findViewById(R.id.btn_clone)
        val btnLaunch: Button = v.findViewById(R.id.btn_launch)
        val btnUninstall: Button = v.findViewById(R.id.btn_uninstall)
    }
}