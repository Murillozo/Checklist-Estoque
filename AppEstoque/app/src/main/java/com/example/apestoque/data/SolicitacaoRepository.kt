package com.example.apestoque.data

class SolicitacaoRepository(private val api: ApiService) {
    suspend fun fetchSolicitacoes(): List<Solicitacao> =
        api.listarSolicitacoes()

    suspend fun aprovarSolicitacao(id: Int) =
        api.aprovarSolicitacao(id)

    suspend fun marcarCompras(id: Int, body: ComprasRequest) =
        api.marcarCompras(id, body)
}
