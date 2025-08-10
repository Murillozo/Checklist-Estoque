package com.example.apestoque.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.apestoque.R
import com.example.apestoque.data.RevisaoChecklist

class RevisaoAdapter(private val itens: List<RevisaoChecklist>) :
    RecyclerView.Adapter<RevisaoAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tv: TextView = view.findViewById(R.id.tvRevisao)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_revisao, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = itens.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = itens[position]
        val lines = buildString {
            append("${item.obra} (${item.ano})")
            for (div in item.divergencias) {
                append("\nItem ${div.numero}: S=${div.suprimento?.joinToString() ?: "-"} P=${div.producao?.joinToString() ?: "-"}")
            }
        }
        holder.tv.text = lines
    }
}
