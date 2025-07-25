package com.example.apestoque.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.apestoque.R
import com.example.apestoque.data.Solicitacao

class SolicitacaoAdapter(
    private val lista: List<Solicitacao>
) : RecyclerView.Adapter<SolicitacaoAdapter.ViewHolder>() {

    inner class ViewHolder(item: View) : RecyclerView.ViewHolder(item) {
        val tvId    = item.findViewById<TextView>(R.id.tvId)
        val tvObra  = item.findViewById<TextView>(R.id.tvObra)
        val tvData  = item.findViewById<TextView>(R.id.tvData)
        val tvItens = item.findViewById<TextView>(R.id.tvItens)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_solicitacao, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = lista.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sol = lista[position]
        holder.tvId.text   = "#${sol.id}"
        holder.tvObra.text = sol.obra
        holder.tvData.text = sol.data.replace("T","  ")  // formata legível
        val itensTexto = sol.itens.joinToString("\n") {
            "• ${it.referencia} × ${it.quantidade}"
        }
        holder.tvItens.text = itensTexto
    }
}
