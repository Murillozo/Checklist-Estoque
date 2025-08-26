package com.example.apestoque.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
package com.example.apestoque.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.apestoque.R
import com.example.apestoque.adapter.InspecaoAdapter
import com.example.apestoque.adapter.InspecaoSolicitacaoAdapter
import com.example.apestoque.data.InspecaoResultadoItem
import com.example.apestoque.data.InspecaoResultadoRequest
import com.example.apestoque.data.NetworkModule
import com.example.apestoque.data.SolicitacaoRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

class InspecionarFragment : Fragment() {
    private var solicitacaoId: Int? = null
    private lateinit var listaAdapter: InspecaoSolicitacaoAdapter
    private lateinit var itensAdapter: InspecaoAdapter
    private val repo by lazy { SolicitacaoRepository(NetworkModule.api(requireContext())) }
    private var refreshJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_inspecionar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerSolic = view.findViewById<RecyclerView>(R.id.recyclerSolicitacoes)
        val recyclerItens = view.findViewById<RecyclerView>(R.id.recyclerInspecao)
        val btn = view.findViewById<Button>(R.id.btnEnviar)

        listaAdapter = InspecaoSolicitacaoAdapter { sol ->
            solicitacaoId = sol.id
            itensAdapter.submitList(sol.itens.map { it.copy() })
            recyclerSolic.visibility = View.GONE
            recyclerItens.visibility = View.VISIBLE
            btn.visibility = View.VISIBLE
        }
        recyclerSolic.layoutManager = LinearLayoutManager(requireContext())
        recyclerSolic.adapter = listaAdapter

        itensAdapter = InspecaoAdapter()
        recyclerItens.layoutManager = LinearLayoutManager(requireContext())
        recyclerItens.adapter = itensAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            repo.fetchInspecoes()
                .onSuccess { lista -> listaAdapter.submitList(lista) }
        }
        refreshJob = viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                delay(5000)
                if (solicitacaoId == null) {
                    repo.fetchInspecoes()
                        .onSuccess { lista -> listaAdapter.submitList(lista) }
                }
            }
        }

        btn.setOnClickListener {
            val id = solicitacaoId ?: return@setOnClickListener
            val itens = itensAdapter.getItens().map {
                val faltante = if (it.verificado) 0 else max(0, it.quantidade - it.qtdEstoque)
                InspecaoResultadoItem(
                    id = it.id,
                    verificado = it.verificado,
                    faltante = faltante
                )
            }
            viewLifecycleOwner.lifecycleScope.launch {
                repo.enviarResultadoInspecao(id, InspecaoResultadoRequest(itens))
                    .onSuccess {
                        recyclerSolic.visibility = View.VISIBLE
                        recyclerItens.visibility = View.GONE
                        btn.visibility = View.GONE
                        solicitacaoId = null
                        repo.fetchInspecoes()
                            .onSuccess { lista -> listaAdapter.submitList(lista) }
                    }
            }
        }
    }

    override fun onDestroyView() {
        refreshJob?.cancel()
        super.onDestroyView()
    }
}
