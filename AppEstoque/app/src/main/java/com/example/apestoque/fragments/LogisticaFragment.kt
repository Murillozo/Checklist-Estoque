package com.example.apestoque.fragments

import android.content.Intent
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
import com.example.apestoque.adapter.ProjetoAdapter
import com.example.apestoque.checklist.ChecklistExpedicaoActivity
import com.example.apestoque.data.JsonNetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LogisticaFragment : Fragment() {

    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var rv: RecyclerView
    private lateinit var tvMsg: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_logistica, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        swipe = view.findViewById(R.id.swipeLogistica)
        rv = view.findViewById(R.id.rvLogistica)
        tvMsg = view.findViewById(R.id.tvMensagemLogistica)
        swipe.setOnRefreshListener { carregar() }
        carregar()
    }

    private fun carregar() {
        swipe.isRefreshing = true
        lifecycleScope.launch {
            try {
                val projetos = withContext(Dispatchers.IO) {
                    JsonNetworkModule.api(requireContext()).listarExpedicaoProjetos().projetos
                }
                if (projetos.isEmpty()) {
                    tvMsg.text = "Nenhum projeto."
                    tvMsg.visibility = View.VISIBLE
                    rv.visibility = View.GONE
                } else {
                    tvMsg.visibility = View.GONE
                    rv.visibility = View.VISIBLE
                    rv.adapter = ProjetoAdapter(projetos) { projeto ->
                        val intent = Intent(requireContext(), ChecklistExpedicaoActivity::class.java)
                        intent.putExtra("obra", projeto.obra)
                        startActivity(intent)
                    }
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
