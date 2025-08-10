package com.example.apestoque.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.apestoque.R
import com.example.apestoque.data.Divergencia

class DivergenciaAdapter(private val itens: List<Divergencia>) :
    RecyclerView.Adapter<DivergenciaAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val pergunta: TextView = view.findViewById(R.id.tvPergunta)
        val sup: TextView = view.findViewById(R.id.tvSup)
        val prod: TextView = view.findViewById(R.id.tvProd)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_divergencia, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = itens.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val d = itens[position]
        holder.pergunta.text = "Item ${d.numero}: ${d.pergunta}"
        holder.sup.text = "Suprimento: ${d.suprimento?.joinToString() ?: "-"}"
        holder.prod.text = "Produção: ${d.producao?.joinToString() ?: "-"}"
    }
}
