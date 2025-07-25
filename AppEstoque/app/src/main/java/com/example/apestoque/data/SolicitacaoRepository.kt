package com.example.apestoque.data

class SolicitacaoRepository(private val api: ApiService) {
    suspend fun fetchSolicitacoes(): List<Solicitacao> =
        api.listarSolicitacoes()

    suspend fun aprovarSolicitacao(id: Int) =
        api.aprovarSolicitacao(id)
}
