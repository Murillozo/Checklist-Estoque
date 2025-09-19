package com.example.apestoque.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.apestoque.R
import com.example.apestoque.data.Projeto

class ProjetoAdapter(
    private val lista: List<Projeto>,
    private val onClick: (Projeto) -> Unit
) : RecyclerView.Adapter<ProjetoAdapter.ViewHolder>() {

    inner class ViewHolder(item: View) : RecyclerView.ViewHolder(item) {
        val tvObra: TextView = item.findViewById(R.id.tvObraProjeto)
        val tvAno: TextView = item.findViewById(R.id.tvAnoProjeto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_projeto, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = lista.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val proj = lista[position]
        holder.tvObra.text = proj.obra
        holder.tvAno.text = proj.ano
        holder.itemView.setOnClickListener { onClick(proj) }
    }
}
