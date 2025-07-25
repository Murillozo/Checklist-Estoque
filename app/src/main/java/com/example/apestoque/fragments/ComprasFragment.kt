package com.example.apestoque.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.apestoque.R
import com.example.apestoque.adapter.SolicitacaoAdapter
import com.example.apestoque.checklist.ChecklistActivity
import com.example.apestoque.data.NetworkModule
import com.example.apestoque.data.Solicitacao
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ComprasFragment : Fragment() {

    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var rv: RecyclerView
    private lateinit var tvMsg: TextView
    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { carregar() }

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
                val lista = withContext(Dispatchers.IO) { NetworkModule.api.listarSolicitacoes() }
                val pendentes = lista.filter { it.status != "aprovado" }
                if (pendentes.isEmpty()) {
                    tvMsg.text = "Nenhuma solicitação."
                    tvMsg.visibility = View.VISIBLE
                    rv.visibility = View.GONE
                } else {
                    tvMsg.visibility = View.GONE
                    rv.visibility = View.VISIBLE
                    rv.adapter = SolicitacaoAdapter(pendentes) { sol -> abrirChecklist(sol) }
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

    private fun abrirChecklist(sol: Solicitacao) {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val json = moshi.adapter(Solicitacao::class.java).toJson(sol)
        val intent = Intent(requireContext(), ChecklistActivity::class.java)
        intent.putExtra("solicitacao", json)
        launcher.launch(intent)
    }
}
