package com.example.appproducao

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.appproducao.data.NetworkModule
import com.example.appproducao.data.Solicitacao
import com.example.appproducao.data.Checklist
import com.example.appproducao.ui.theme.AppProducaoTheme
import com.squareup.moshi.Moshi
import kotlinx.coroutines.launch
import java.io.File

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
fun ChecklistScreen() {
    val context = LocalContext.current
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var selectedFile by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(Unit) {
        val dir = File(context.filesDir, "../../site/json_api")
        if (dir.exists()) {
            files = dir.listFiles { f -> f.extension == "json" }?.sortedByDescending { it.lastModified() }?.toList()
                ?: emptyList()
        }
    }

    if (selectedFile == null) {
        LazyColumn {
            items(files) { file ->
                Text(
                    text = file.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedFile = file }
                        .padding(16.dp)
                )
                Divider()
            }
        }
    } else {
        ChecklistDetailScreen(selectedFile!!) { selectedFile = null }
    }
}

@Composable
fun ChecklistDetailScreen(file: File, onDone: () -> Unit) {
    val moshi = remember { Moshi.Builder().build() }
    val adapter = remember { moshi.adapter(Checklist::class.java) }
    var checklist by remember { mutableStateOf<Checklist?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var nome by remember { mutableStateOf("") }

    LaunchedEffect(file) {
        checklist = runCatching { adapter.fromJson(file.readText()) }.getOrNull()
    }

    checklist?.let { ck ->
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(ck.itens) { item ->
                    var checked by remember { mutableStateOf(item.status == "C") }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(checked = checked, onCheckedChange = {
                            checked = it
                            item.status = if (it) "C" else "N.C"
                        })
                        Text(item.descricao, Modifier.padding(start = 8.dp))
                    }
                }
            }
            Button(
                onClick = { showDialog = true },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Concluir")
            }
        }
    } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    checklist?.responsavel = nome
                    val newName = file.nameWithoutExtension + "_preenchido.json"
                    val newFile = File(file.parentFile, newName)
                    newFile.writeText(adapter.indent("  ").toJson(checklist))
                    showDialog = false
                    onDone()
                }) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancelar") }
            },
            text = {
                Column {
                    Text("Seu nome Produção")
                    TextField(value = nome, onValueChange = { nome = it })
                }
            }
        )
    }
}

@Composable
fun MainScreen() {
    var selected by remember { mutableStateOf(0) }
    val tabs = listOf("Aprovadas", "Em produção", "Checklist")
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
            0 -> AprovadasScreen()
            1 -> EmProducaoScreen()
            else -> ChecklistScreen()
        }
    }
}

@Composable
fun AprovadasScreen() {
    var solicitacoes by remember { mutableStateOf<List<Solicitacao>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val all = NetworkModule.api.listarSolicitacoes()
                solicitacoes = all.filter { it.status == "aprovado" }
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

        }
        else -> {
            LazyColumn {
                items(solicitacoes) { sol ->
                    Text(
                        text = "${sol.obra} - id ${sol.id}",
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
    var lista80 by remember { mutableStateOf<List<Solicitacao>>(emptyList()) }
    var listaCompleta by remember { mutableStateOf<List<Solicitacao>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val all = NetworkModule.api.listarSolicitacoes()
                val aprovados = all.filter { it.status == "aprovado" }
                lista80 = aprovados.filter { it.pendencias != null && it.pendencias != "[]" }
                listaCompleta = aprovados.filter { it.pendencias == null || it.pendencias == "[]" }
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

        }
        else -> {
            LazyColumn {
                if (lista80.isNotEmpty()) {
                    item {
                        Text(
                            text = "Material Separado com 80% da lista",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    items(lista80) { sol ->
                        Text(
                            text = "${sol.obra} - id ${sol.id}",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        Divider()
                    }
                }
                if (listaCompleta.isNotEmpty()) {
                    item {
                        Text(
                            text = "Material separado completo",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    items(listaCompleta) { sol ->
                        Text(
                            text = "${sol.obra} - id ${sol.id}",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        Divider()
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AppProducaoTheme {
        MainScreen()
    }
}
