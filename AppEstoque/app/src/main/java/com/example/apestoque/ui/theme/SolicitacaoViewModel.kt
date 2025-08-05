package com.example.apestoque.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.apestoque.data.NetworkModule
import com.example.apestoque.data.Solicitacao
import com.example.apestoque.data.SolicitacaoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SolicitacaoViewModel : ViewModel() {
    private val repo = SolicitacaoRepository(NetworkModule.api)

    private val _lista = MutableStateFlow<List<Solicitacao>>(emptyList())
    val lista: StateFlow<List<Solicitacao>> = _lista

    init {
        viewModelScope.launch {
            _lista.value = repo.fetchSolicitacoes().getOrElse { emptyList() }
        }
    }
}
