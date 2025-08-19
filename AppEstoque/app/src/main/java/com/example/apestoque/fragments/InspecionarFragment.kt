package com.example.apestoque.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.Button
import com.example.apestoque.R
import com.example.apestoque.adapter.InspecaoAdapter
import com.example.apestoque.data.NetworkModule
import com.example.apestoque.data.SolicitacaoRepository
import com.example.apestoque.data.InspecaoResultadoItem
import com.example.apestoque.data.InspecaoResultadoRequest
import kotlinx.coroutines.launch

class InspecionarFragment : Fragment() {
    private var solicitacaoId: Int? = null
    private lateinit var adapter: InspecaoAdapter
    private val repo by lazy { SolicitacaoRepository(NetworkModule.api(requireContext())) }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_inspecionar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerInspecao)
        adapter = InspecaoAdapter()
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        val btn = view.findViewById<Button>(R.id.btnEnviar)

        viewLifecycleOwner.lifecycleScope.launch {
            repo.fetchInspecoes()
                .onSuccess { lista ->
                    val sol = lista.firstOrNull()
                    if (sol != null) {
                        solicitacaoId = sol.id
                        adapter.submitList(sol.itens)
                    }
                }
        }

        btn.setOnClickListener {
            val id = solicitacaoId ?: return@setOnClickListener
            val itens = adapter.getItens().map {
                InspecaoResultadoItem(
                    id = it.id,
                    verificado = it.verificado,
                    faltante = if (it.verificado) 0 else it.faltante
                )
            }
            viewLifecycleOwner.lifecycleScope.launch {
                repo.enviarResultadoInspecao(id, InspecaoResultadoRequest(itens))
            }
        }
    }
}
