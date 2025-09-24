package com.example.apestoque.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.apestoque.R
import com.example.apestoque.adapter.SolicitacaoAdapter
import com.example.apestoque.data.NetworkModule
import com.example.apestoque.data.Solicitacao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ComprasFragment : Fragment() {

    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var rv: RecyclerView
    private lateinit var tvMsg: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_compras, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipe = view.findViewById(R.id.swipeCompras)
        rv = view.findViewById(R.id.rvCompras)
        tvMsg = view.findViewById(R.id.tvMensagemCompras)

        swipe.setOnRefreshListener { carregar() }
        carregar()
    }

    private fun carregar() {
        swipe.isRefreshing = true
        lifecycleScope.launch {
            try {
                val lista = withContext(Dispatchers.IO) { NetworkModule.api(requireContext()).listarSolicitacoes() }
                val statusCompras = setOf("compras", "pendente_compras")
                val pendentes = lista.filter { sol ->
                    sol.status?.lowercase() in statusCompras
                }
                if (pendentes.isEmpty()) {
                    tvMsg.text = "Nenhuma solicitação."
                    tvMsg.visibility = View.VISIBLE
                    rv.visibility = View.GONE
                } else {
                    tvMsg.visibility = View.GONE
                    rv.visibility = View.VISIBLE
                    rv.adapter = SolicitacaoAdapter(pendentes)
                }
            } catch (e: Exception) {
                tvMsg.text = "Erro ao carregar"
                tvMsg.visibility = View.VISIBLE
                rv.visibility = View.GONE
            } finally {
                swipe.isRefreshing = false
            }
        }
    }

}
