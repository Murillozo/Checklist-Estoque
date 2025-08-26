package com.example.apestoque.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import com.example.apestoque.R
import com.example.apestoque.data.InspecaoItem

class InspecaoAdapter : RecyclerView.Adapter<InspecaoAdapter.VH>() {
    private val itens = mutableListOf<InspecaoItem>()

    fun submitList(lista: List<InspecaoItem>) {
        itens.clear()
        itens.addAll(lista)
        notifyDataSetChanged()
    }

    fun getItens(): List<InspecaoItem> = itens

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_inspecao, parent, false)
        return VH(view)
    }

    override fun getItemCount(): Int = itens.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = itens[position]
        holder.cb.text = "${item.referencia} (${item.quantidade})"
        holder.cb.isChecked = item.verificado
        holder.et.visibility = if (item.verificado) View.GONE else View.VISIBLE
        holder.et.setText(if (item.qtdEstoque > 0) item.qtdEstoque.toString() else "")

        holder.cb.setOnCheckedChangeListener { _, checked ->
            item.verificado = checked
            if (checked) {
                item.qtdEstoque = item.quantidade
                holder.et.setText("")
                holder.et.visibility = View.GONE
            } else {
                item.qtdEstoque = 0
                holder.et.setText("")
                holder.et.visibility = View.VISIBLE
            }
        }

        holder.et.removeTextChangedListener(holder.watcher)
        holder.watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                item.qtdEstoque = s?.toString()?.toIntOrNull() ?: 0
            }
        }
        holder.et.addTextChangedListener(holder.watcher)
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val cb: CheckBox = view.findViewById(R.id.cbItem)
        val et: EditText = view.findViewById(R.id.etQtdEstoque)
        var watcher: TextWatcher? = null
    }
}