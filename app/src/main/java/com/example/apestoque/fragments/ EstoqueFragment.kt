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
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EstoqueFragment : Fragment() {

    private var refreshJob: Job? = null
    private val refreshIntervalMs = 30_000L  // 30 segundos

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvSolicitacoes: RecyclerView
    private lateinit var tvMensagem: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Infla o layout que contém swipeRefresh, rvSolicitacoes e tvMensagem
        return inflater.inflate(R.layout.fragment_estoque, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Busca as views
        swipeRefresh    = view.findViewById(R.id.swipeRefresh)
        rvSolicitacoes  = view.findViewById(R.id.rvSolicitacoes)
        tvMensagem      = view.findViewById(R.id.tvMensagem)

        // Pull-to-refresh manual
        swipeRefresh.setOnRefreshListener {
            carregarDados(showSnackbar = true)
        }

        // Carrega pela primeira vez
        carregarDados(showSnackbar = false)

        // Agenda atualizações automáticas
        refreshJob = lifecycleScope.launch {
            while (isActive) {
                delay(refreshIntervalMs)
                carregarDados(showSnackbar = false)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Cancela o job ao sair da view
        refreshJob?.cancel()
    }

    private fun carregarDados(showSnackbar: Boolean) {
        // Habilita indicador de loading
        swipeRefresh.isRefreshing = true

        lifecycleScope.launch {
            try {
                // Requisição em IO
                val lista: List<Solicitacao> = withContext(Dispatchers.IO) {
                    NetworkModule.api.listarSolicitacoes()
                }

                if (lista.isEmpty()) {
                    tvMensagem.text = "Nenhuma solicitação encontrada."
                    tvMensagem.visibility = View.VISIBLE
                    rvSolicitacoes.visibility = View.GONE
                } else {
                    // Agrupa por obra e pega a última solicitação de cada obra
                    val ultimasPorObra = lista
                        .groupBy { it.obra }
                        .mapNotNull { (_, group) -> group.maxByOrNull { it.id } }
                        .sortedByDescending { it.id }

                    // Atualiza RecyclerView
                    rvSolicitacoes.adapter = SolicitacaoAdapter(ultimasPorObra)
                    rvSolicitacoes.visibility = View.VISIBLE
                    tvMensagem.visibility = View.GONE

                    if (showSnackbar) {
                        Snackbar.make(
                            requireView(),
                            "Carregadas ${ultimasPorObra.size} solicitações",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                tvMensagem.text = "Erro ao carregar: ${e.localizedMessage}"
                tvMensagem.visibility = View.VISIBLE
                rvSolicitacoes.visibility = View.GONE
            } finally {
                // Sempre desliga o loading
                swipeRefresh.isRefreshing = false
            }
        }
    }
}
