package com.example.apestoque.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.apestoque.R
import com.example.apestoque.adapter.InspecaoAdapter
import com.example.apestoque.data.NetworkModule
import kotlinx.coroutines.launch

class InspecionarFragment : Fragment() {
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
        val adapter = InspecaoAdapter()
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            val api = NetworkModule.api(requireContext())
            runCatching { api.listarInspecoes() }
                .onSuccess { lista ->
                    adapter.submitList(lista.flatMap { it.itens })
                }
        }
    }
}
