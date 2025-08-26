package com.example.apestoque.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.apestoque.R
import com.example.apestoque.data.FotoNode

class CameraTreeAdapter(
    private val onChildClick: (String, FotoNode) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<Any>()

    fun setData(data: List<FotoNode>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int =
        if (items[position] is FotoNode) VIEW_TYPE_GROUP else VIEW_TYPE_CHILD

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layout = if (viewType == VIEW_TYPE_GROUP) R.layout.list_group else R.layout.list_child
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return object : RecyclerView.ViewHolder(view) {}
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            VIEW_TYPE_GROUP -> {
                val node = items[position] as FotoNode
                holder.itemView.findViewById<TextView>(android.R.id.text1).text = node.name
                holder.itemView.setOnClickListener {
                    val pos = holder.bindingAdapterPosition
                    val groupNode = items[pos] as FotoNode
                    if (pos + 1 < items.size && items[pos + 1] is Pair<*, *>) {
                        val count = groupNode.children?.size ?: 0
                        repeat(count) { items.removeAt(pos + 1) }
                        notifyItemRangeRemoved(pos + 1, count)
                    } else {
                        val children = groupNode.children ?: emptyList()
                        val insertItems = children.map { groupNode.name to it }
                        items.addAll(pos + 1, insertItems)
                        notifyItemRangeInserted(pos + 1, insertItems.size)
                    }
                }
            }
            VIEW_TYPE_CHILD -> {
                val pair = items[position] as Pair<String, FotoNode>
                holder.itemView.findViewById<TextView>(android.R.id.text1).text = pair.second.name
                holder.itemView.setOnClickListener {
                    onChildClick(pair.first, pair.second)
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size

    companion object {
        private const val VIEW_TYPE_GROUP = 0
        private const val VIEW_TYPE_CHILD = 1
    }
}
