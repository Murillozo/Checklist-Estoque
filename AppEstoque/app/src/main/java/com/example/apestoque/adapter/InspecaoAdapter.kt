package com.example.apestoque.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.apestoque.R
import com.example.apestoque.data.Item

class InspecaoAdapter : ListAdapter<Item, InspecaoAdapter.VH>(DIFF) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_inspecao, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.cb.text = "${item.referencia} (${item.quantidade})"
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val cb: CheckBox = view.findViewById(R.id.cbItem)
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Item>() {
            override fun areItemsTheSame(oldItem: Item, newItem: Item) =
                oldItem.referencia == newItem.referencia

            override fun areContentsTheSame(oldItem: Item, newItem: Item) =
                oldItem == newItem
        }
    }
}
