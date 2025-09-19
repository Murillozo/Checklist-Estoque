package com.example.apestoque

import com.example.apestoque.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class FakeApiService : ApiService {
    var shouldThrow = false
    override suspend fun listarSolicitacoes(): List<Solicitacao> {
        if (shouldThrow) throw IOException("network")
        return listOf(
            Solicitacao(
                id = 1,
                obra = "Obra 1",
                data = "2024-01-01",
                itens = listOf(Item("Ref", 2))
            )
        )
    }

    override suspend fun aprovarSolicitacao(id: Int) {
        if (shouldThrow) throw IOException("network")
    }

    override suspend fun marcarCompras(id: Int, body: ComprasRequest) {
        if (shouldThrow) throw IOException("network")
    }
}

class SolicitacaoRepositoryTest {
    private val api = FakeApiService()
    private val repo = SolicitacaoRepository(api)

    @Test
    fun fetchSolicitacoes_returnsDataOnSuccess() = runBlocking {
        val result = repo.fetchSolicitacoes()
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().size)
    }

    @Test
    fun fetchSolicitacoes_returnsFailureOnException() = runBlocking {
        api.shouldThrow = true
        val result = repo.fetchSolicitacoes()
        assertTrue(result.isFailure)
    }
}
