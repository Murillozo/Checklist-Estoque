package com.example.appproducao

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.appproducao.ui.theme.APPProducaoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL

data class Solicitacao(
    val id: Int,
    val obra: String,
    val status: String
)

class MainActivity : ComponentActivity() {
    private val solicitacoes = mutableStateListOf<Solicitacao>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        buscarSolicitacoes()
        setContent {
            APPProducaoTheme {
                TelaPrincipal(solicitacoes)
            }
        }
    }

    private fun buscarSolicitacoes() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val texto = URL("http://10.0.2.2:5000/projetista/api/solicitacoes").readText()
                val arr = JSONArray(texto)
                val novos = mutableListOf<Solicitacao>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    novos.add(
                        Solicitacao(
                            id = obj.getInt("id"),
                            obra = obj.getString("obra"),
                            status = obj.getString("status")
                        )
                    )
                }
                withContext(Dispatchers.Main) {
                    solicitacoes.clear()
                    solicitacoes.addAll(novos)
                }
            } catch (e: Exception) {
                // Em caso de erro apenas imprime no log
                e.printStackTrace()
            }
        }
    }
}

@Composable
fun TelaPrincipal(solicitacoes: List<Solicitacao>) {
    var tabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Iniciar", "Em produção")

    Scaffold(
        topBar = {
            TabRow(selectedTabIndex = tabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = tabIndex == index,
                        onClick = { tabIndex = index },
                        text = { Text(title) }
                    )
                }
            }
        }
    ) { inner ->
        when (tabIndex) {
            0 -> AbaIniciar(
                solicitacoes.filter { it.status == "aprovado" },
                Modifier.padding(inner)
            )
            else -> AbaEmProducao(Modifier.padding(inner))
        }
    }
}

@Composable
fun AbaIniciar(lista: List<Solicitacao>, modifier: Modifier = Modifier) {
    if (lista.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nenhuma solicitação")
        }
    } else {
        LazyColumn(modifier = modifier.fillMaxSize()) {
            items(lista) { sol ->
                Text(
                    text = "${sol.id} - ${sol.obra}",
                    modifier = Modifier.padding(16.dp)
                )
                Divider()
            }
        }
    }
}

@Composable
fun AbaEmProducao(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Em produção")
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    APPProducaoTheme {
        TelaPrincipal(
            listOf(
                Solicitacao(1, "Obra X", "aprovado"),
                Solicitacao(2, "Obra Y", "analise")
            )
        )
    }
}