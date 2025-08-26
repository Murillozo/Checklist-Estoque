package com.example.apestoque.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.apestoque.R
import com.example.apestoque.data.InspecaoSolicitacao

class InspecaoSolicitacaoAdapter(
    private val onClick: (InspecaoSolicitacao) -> Unit,
) : RecyclerView.Adapter<InspecaoSolicitacaoAdapter.VH>() {
    private val itens = mutableListOf<InspecaoSolicitacao>()

    fun submitList(lista: List<InspecaoSolicitacao>) {
        itens.clear()
        itens.addAll(lista)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_inspecao_solicitacao, parent, false)
        return VH(view)
    }

    override fun getItemCount(): Int = itens.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val sol = itens[position]
        holder.titulo.text = "Solicitação ${sol.id}"
        holder.qtd.text = "${sol.itens.size} itens"
        holder.itemView.setOnClickListener { onClick(sol) }
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val titulo: TextView = view.findViewById(R.id.tvTitulo)
        val qtd: TextView = view.findViewById(R.id.tvQtd)
    }
}

