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
import com.example.apestoque.adapter.RevisaoAdapter
import com.example.apestoque.data.JsonNetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RevisaoFragment : Fragment() {

    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var rv: RecyclerView
    private lateinit var tvMsg: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_revisao, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        swipe = view.findViewById(R.id.swipeRevisao)
        rv = view.findViewById(R.id.rvRevisao)
        tvMsg = view.findViewById(R.id.tvMensagemRevisao)
        swipe.setOnRefreshListener { carregar() }
        carregar()
    }

    private fun carregar() {
        swipe.isRefreshing = true
        lifecycleScope.launch {
            try {
                val lista = withContext(Dispatchers.IO) { JsonNetworkModule.api.listarRevisao() }
                if (lista.isEmpty()) {
                    tvMsg.text = "Nenhuma divergência."
                    tvMsg.visibility = View.VISIBLE
                    rv.visibility = View.GONE
                } else {
                    tvMsg.visibility = View.GONE
                    rv.visibility = View.VISIBLE
                    rv.adapter = RevisaoAdapter(lista)
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
