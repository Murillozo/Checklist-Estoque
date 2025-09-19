package com.example.apestoque.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.apestoque.R
import com.example.apestoque.adapter.SolicitacaoAdapter
import com.example.apestoque.checklist.ChecklistActivity
import com.example.apestoque.data.NetworkModule
import com.example.apestoque.data.Solicitacao
import com.google.android.material.snackbar.Snackbar
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.media.AudioManager
import android.media.ToneGenerator

class SolicitacoesFragment : Fragment() {

    private var refreshJob: Job? = null
    private val refreshIntervalMs = 30_000L  // 30 segundos

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvSolicitacoes: RecyclerView
    private lateinit var tvMensagem: TextView
    private lateinit var searchView: SearchView
    private var todasSolicitacoes: List<Solicitacao> = emptyList()
    private var knownIds: Set<Int> = emptySet()
    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        carregarDados(showSnackbar = false)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Infla o layout que contém swipeRefresh, rvSolicitacoes e tvMensagem
        return inflater.inflate(R.layout.fragment_solicitacoes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Busca as views
        swipeRefresh    = view.findViewById(R.id.swipeRefresh)
        rvSolicitacoes  = view.findViewById(R.id.rvSolicitacoes)
        tvMensagem      = view.findViewById(R.id.tvMensagem)
        searchView      = view.findViewById(R.id.searchView)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filtrar(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filtrar(newText)
                return true
            }
        })

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
                    NetworkModule.api(requireContext()).listarSolicitacoes()
                }

                val pendentes = lista.filter { it.status != "aprovado" && it.pendencias == null }

                if (pendentes.isEmpty()) {
                    tvMensagem.text = "Nenhuma solicitação encontrada."
                    tvMensagem.visibility = View.VISIBLE
                    rvSolicitacoes.visibility = View.GONE
                    todasSolicitacoes = emptyList()
                    knownIds = emptySet()
                } else {
                    // Agrupa por obra e pega a última solicitação de cada obra
                    val ultimasPorObra = pendentes
                        .groupBy { it.obra }
                        .mapNotNull { (_, group) -> group.maxByOrNull { it.id } }
                        .sortedByDescending { it.id }

                    val currentIds = ultimasPorObra.map { it.id }.toSet()
                    if (knownIds.isNotEmpty() && (currentIds - knownIds).isNotEmpty()) {
                        playTone()
                    }
                    knownIds = currentIds

                    todasSolicitacoes = ultimasPorObra
                    filtrar(searchView.query.toString())

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

    private fun filtrar(query: String?) {
        val texto = query.orEmpty()
        val filtrada = if (texto.isBlank()) {
            todasSolicitacoes
        } else {
            todasSolicitacoes.filter { it.obra.contains(texto, ignoreCase = true) }
        }

        if (filtrada.isEmpty()) {
            tvMensagem.text = "Nenhum projeto encontrado."
            tvMensagem.visibility = View.VISIBLE
            rvSolicitacoes.visibility = View.GONE
        } else {
            rvSolicitacoes.adapter = SolicitacaoAdapter(filtrada) { sol -> abrirChecklist(sol) }
            rvSolicitacoes.visibility = View.VISIBLE
            tvMensagem.visibility = View.GONE
        }
    }

    private fun abrirChecklist(sol: Solicitacao) {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val json = moshi.adapter(Solicitacao::class.java).toJson(sol)
        val intent = Intent(requireContext(), ChecklistActivity::class.java)
        intent.putExtra("solicitacao", json)
        launcher.launch(intent)
    }

    private fun playTone() {
        ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100).startTone(
            ToneGenerator.TONE_PROP_BEEP,
            150
        )
    }
}
