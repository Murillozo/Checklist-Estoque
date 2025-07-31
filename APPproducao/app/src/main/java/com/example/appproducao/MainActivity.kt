package com.example.appproducao

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.appproducao.data.NetworkModule
import com.example.appproducao.data.Solicitacao
import com.example.appproducao.ui.theme.AppProducaoTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppProducaoTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    var selected by remember { mutableStateOf(0) }
    val tabs = listOf("Iniciar", "Em produção")
    Column {
        TabRow(selectedTabIndex = selected) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    text = { Text(title) },
                    selected = selected == index,
                    onClick = { selected = index }
                )
            }
        }
        when (selected) {
            0 -> IniciarScreen()
            else -> EmProducaoScreen()
        }
    }
}

@Composable
fun IniciarScreen() {
    var solicitacoes by remember { mutableStateOf<List<Solicitacao>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val all = NetworkModule.api.listarSolicitacoes()
                solicitacoes = all.filter { it.status?.lowercase() == "separado" }
            } catch (e: Exception) {
                error = e.localizedMessage
            } finally {
                loading = false
            }
        }
    }

    when {
        loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        error != null -> {
            Text("Erro: $error", modifier = Modifier.padding(16.dp))
        }
        else -> {
            LazyColumn {
                items(solicitacoes) { sol ->
                    Text(
                        text = "${'$'}{sol.obra} - id ${'$'}{sol.id}",
                        modifier = Modifier.padding(16.dp)
                    )
                    Divider()
                }
            }
        }
    }
}

@Composable
fun EmProducaoScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Em produção")
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AppProducaoTheme {
        MainScreen()
    }
}
