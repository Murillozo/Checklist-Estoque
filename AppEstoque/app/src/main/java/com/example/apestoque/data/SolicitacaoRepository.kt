package com.example.apestoque.data

class SolicitacaoRepository(private val api: ApiService) {
    suspend fun fetchInspecoes(): Result<List<InspecaoSolicitacao>> =
        runCatching { api.listarInspecoes() }

    suspend fun fetchSolicitacoes(): Result<List<Solicitacao>> =
        runCatching { api.listarSolicitacoes() }

    suspend fun aprovarSolicitacao(id: Int): Result<Unit> =
        runCatching { api.aprovarSolicitacao(id) }

    suspend fun marcarCompras(id: Int, body: ComprasRequest): Result<Unit> =
        runCatching { api.marcarCompras(id, body) }

    suspend fun enviarResultadoInspecao(id: Int, body: InspecaoResultadoRequest): Result<Unit> =
        runCatching { api.enviarResultadoInspecao(id, body) }
}
