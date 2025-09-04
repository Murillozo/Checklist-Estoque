package com.example.appoficina

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class ChecklistPosto08TesteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checklist_posto08_teste)

        val obra = intent.getStringExtra("obra") ?: ""
        val ano = intent.getStringExtra("ano") ?: ""
        val inspetor = intent.getStringExtra("inspetor") ?: ""

        val container = findViewById<LinearLayout>(R.id.questions_container)

        fun addLabel(text: String) {
            val tv = TextView(this)
            tv.text = text
            container.addView(tv)
        }

        // 8.3 - TESTE - DADOS
        addLabel("8.3 - TESTE - DADOS")

        addLabel("Responsável")
        val responsavelInput = EditText(this)
        container.addView(responsavelInput)

        addLabel("Altitude em relação ao nivel do mar")
        val altitudeInput = EditText(this)
        altitudeInput.isEnabled = false
        container.addView(altitudeInput)

        addLabel("Grau de Poluição")
        val poluicaoSpinner = Spinner(this)
        val poluicaoOptions = listOf(
            "Grau de poluição 1: Não ocorre poluição ou somente uma poluição seca não-condutiva. A poluição não tem nenhuma influência.",
            "Grau de poluição 2: Presença somente de uma poluição não-condutiva, exceto que, ocasionalmente, uma condutividade temporária causada por condensação pode ocorrer.",
            "Grau de poluição 3: Presença de uma poluição condutiva ou de uma poluição seca não-condutiva, que pode se tornar condutiva devido à condensação.",
            "Grau de poluição 4: Ocorre uma condutividade contínua devido a presença de pó condutivo, chuva ou outras condições úmidas."
        )
        poluicaoSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, poluicaoOptions)
        container.addView(poluicaoSpinner)

        addLabel("Grau de proteção (IP)")
        val ipInput = EditText(this)
        container.addView(ipInput)

        addLabel("Instalação")
        val instalSpinner = Spinner(this)
        instalSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Abrigada", "Ao tempo")
        )
        container.addView(instalSpinner)

        addLabel("Aplicação")
        val aplicSpinner = Spinner(this)
        aplicSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Industrial", "Comercial", "Agro", "Residencial", "Data center", "Fotovoltaico")
        )
        container.addView(aplicSpinner)

        addLabel("Temperatura Ambiente")
        val tempInput = EditText(this)
        tempInput.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        tempInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                AlertDialog.Builder(this)
                    .setMessage("preencher com a temperatura do medidor do relogio da produção")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
        container.addView(tempInput)

        addLabel("Humidade relativa")
        val humidadeInput = EditText(this)
        humidadeInput.isEnabled = false
        container.addView(humidadeInput)

        addLabel("Tensão de comando (V)")
        val tensaoComandoInput = EditText(this)
        container.addView(tensaoComandoInput)

        addLabel("Tensão circuito auxiliar (V)")
        val tensaoAuxInput = EditText(this)
        container.addView(tensaoAuxInput)

        addLabel("Tensão circuito de força (V)")
        val tensaoForcaInput = EditText(this)
        container.addView(tensaoForcaInput)

        fetchAmbientData(altitudeInput, humidadeInput)

        // 8.4 - TESTE - TENSÃO APLICADA
        addLabel("8.4 - TESTE - TENSÃO APLICADA")
        data class TensaoRefs(
            val unidade: Spinner,
            val valor: EditText,
            val resUnidade: Spinner,
            val resValor: EditText,
            val c: CheckBox,
            val nc: CheckBox,
            val na: CheckBox,
        )

        val unidadeOptions = listOf("V", "kV")
        val resUnidadeOptions = listOf("Ω", "MΩ", "GΩ", "TΩ", "mA")
        val tensaoPerguntas = listOf(
            "4.2 - Comando x Terra",
            "4.3 - Força - Fase A x BC Terra",
            "4.4 - Força - Fase B x AC Terra",
            "4.5 - Força - Fase C x AB Terra",
            "4.6 - Força - Fase ABC x Terra",
        )
        val tensaoRefs = mutableListOf<TensaoRefs>()
        tensaoPerguntas.forEach { pergunta ->
            addLabel(pergunta)
            val row1 = LinearLayout(this)
            row1.orientation = LinearLayout.HORIZONTAL
            val unidade = Spinner(this)
            unidade.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                unidadeOptions
            )
            val valor = EditText(this)
            valor.hint = "Valor"
            row1.addView(unidade)
            row1.addView(valor)
            container.addView(row1)

            val row2 = LinearLayout(this)
            row2.orientation = LinearLayout.HORIZONTAL
            val resUni = Spinner(this)
            resUni.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                resUnidadeOptions

            )
            val resVal = EditText(this)
            resVal.hint = "Resultado: Valor"
            row2.addView(resUni)
            row2.addView(resVal)
            container.addView(row2)

            val row3 = LinearLayout(this)
            row3.orientation = LinearLayout.HORIZONTAL
            val c = CheckBox(this)
            c.text = "C"
            val nc = CheckBox(this)
            nc.text = "N.C."
            nc.setPadding(24, 0, 0, 0)
            val na = CheckBox(this)
            na.text = "N.A."
            na.setPadding(24, 0, 0, 0)
            row3.addView(c)
            row3.addView(nc)
            row3.addView(na)
            container.addView(row3)

            tensaoRefs.add(TensaoRefs(unidade, valor, resUni, resVal, c, nc, na))

            c.setOnCheckedChangeListener { _, isChecked -> if (isChecked) { nc.isChecked = false; na.isChecked = false } }
            nc.setOnCheckedChangeListener { _, isChecked -> if (isChecked) { c.isChecked = false; na.isChecked = false } }
            na.setOnCheckedChangeListener { _, isChecked -> if (isChecked) { c.isChecked = false; nc.isChecked = false } }
        }

        // 8.5 - TESTE - CONFIGURAÇÃO DE DISPOSITIVOS
        addLabel("8.5 - TESTE - CONFIGURAÇÃO DE DISPOSITIVOS")
        val configPerguntas = listOf(
            "Multimedidores",
            "Controladores",
            "Drives",
            "Monitores",
            "Sensores",
            "IHMs",
            "Timers",
            "Dispositivos de proteção ajustável",
        )
        val configPairs = mutableListOf<Pair<CheckBox, CheckBox>>()
        configPerguntas.forEach { pergunta ->
            addLabel(pergunta)
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            val c = CheckBox(this)
            c.text = "C"
            val na = CheckBox(this)
            na.text = "NA"
            na.setPadding(24, 0, 0, 0)
            row.addView(c)
            row.addView(na)
            container.addView(row)
            configPairs.add(Pair(c, na))
            c.setOnCheckedChangeListener { _, isChecked -> if (isChecked) na.isChecked = false }
            na.setOnCheckedChangeListener { _, isChecked -> if (isChecked) c.isChecked = false }
        }

        // 8.5 - TESTE - FUNCIONAIS
        addLabel("8.5 - TESTE - FUNCIONAIS")
        val funcPerguntas = listOf(
            "Sinalizadores",
            "Status à campo",
            "Leituras",
            "Intertravamentos",
            "Lógica de acionamento",
            "Programa",
            "Tensão das saídas à campo",
        )
        val funcPairs = mutableListOf<Pair<CheckBox, CheckBox>>()
        funcPerguntas.forEach { pergunta ->
            addLabel(pergunta)
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            val c = CheckBox(this)
            c.text = "C"
            val na = CheckBox(this)
            na.text = "NA"
            na.setPadding(24, 0, 0, 0)
            row.addView(c)
            row.addView(na)
            container.addView(row)
            funcPairs.add(Pair(c, na))
            c.setOnCheckedChangeListener { _, isChecked -> if (isChecked) na.isChecked = false }
            na.setOnCheckedChangeListener { _, isChecked -> if (isChecked) c.isChecked = false }
        }

        val concluirButton = findViewById<Button>(R.id.btnConcluirPosto08Teste)

        concluirButton.setOnClickListener {
            Thread {
                val itens = JSONArray()
                var numero = 8301

                fun addItem(pergunta: String, respostas: List<String>) {
                    val obj = JSONObject()
                    obj.put("numero", numero++)
                    obj.put("pergunta", pergunta)
                    val arr = JSONArray()
                    respostas.forEach { arr.put(it) }
                    obj.put("resposta", arr)
                    itens.put(obj)
                }

                addItem("Responsável", listOf(responsavelInput.text.toString()))
                addItem("Altitude em relação ao nivel do mar", listOf(altitudeInput.text.toString()))
                addItem("Grau de Poluição", listOf(poluicaoSpinner.selectedItem.toString()))
                addItem("Grau de proteção (IP)", listOf(ipInput.text.toString()))
                addItem("Instalação", listOf(instalSpinner.selectedItem.toString()))
                addItem("Aplicação", listOf(aplicSpinner.selectedItem.toString()))
                addItem("Temperatura Ambiente", listOf(tempInput.text.toString()))
                addItem("Humidade relativa", listOf(humidadeInput.text.toString()))
                addItem("Tensão de comando", listOf(tensaoComandoInput.text.toString()))
                addItem("Tensão circuito auxiliar", listOf(tensaoAuxInput.text.toString()))
                addItem("Tensão circuito de força", listOf(tensaoForcaInput.text.toString()))

                tensaoPerguntas.forEachIndexed { idx, pergunta ->
                    val refs = tensaoRefs[idx]
                    addItem(
                        pergunta,
                        listOf(
                            refs.unidade.selectedItem.toString(),
                            refs.valor.text.toString(),
                            refs.resUnidade.selectedItem.toString(),
                            refs.resValor.text.toString(),
                            when {
                                refs.c.isChecked -> "C"
                                refs.nc.isChecked -> "NC"
                                else -> "NA"
                            }
                        )
                    )
                }

                configPerguntas.forEachIndexed { idx, pergunta ->
                    val (c, na) = configPairs[idx]
                    addItem(pergunta, listOf(if (c.isChecked) "C" else "NA"))
                }

                funcPerguntas.forEachIndexed { idx, pergunta ->
                    val (c, na) = funcPairs[idx]
                    addItem(pergunta, listOf(if (c.isChecked) "C" else "NA"))
                }

                val payload = JSONObject()
                payload.put("obra", obra)
                payload.put("ano", ano)
                payload.put("inspetor", inspetor)
                payload.put("itens", itens)
                enviarChecklist(payload, "/json_api/posto08_teste/update")
            }.start()
            finish()
        }
    }

    private fun fetchAmbientData(altitudeView: EditText, humidadeView: EditText) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                100
            )
            return
        }
        val lm = getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        val loc = lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
            ?: lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
        loc?.let {
            val lat = it.latitude
            val lon = it.longitude
            Thread {
                try {
                    val altUrl = URL("https://api.open-meteo.com/v1/elevation?latitude=$lat&longitude=$lon")
                    val altConn = altUrl.openConnection() as HttpURLConnection
                    val altRes = altConn.inputStream.bufferedReader().use { r -> r.readText() }
                    val alt = JSONObject(altRes).getJSONArray("elevation").optDouble(0)
                    altConn.disconnect()

                    val humUrl = URL("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=relative_humidity_2m")
                    val humConn = humUrl.openConnection() as HttpURLConnection
                    val humRes = humConn.inputStream.bufferedReader().use { r -> r.readText() }
                    val hum = JSONObject(humRes).getJSONObject("current").optDouble("relative_humidity_2m")
                    humConn.disconnect()

                    runOnUiThread {
                        altitudeView.setText(alt.toString())
                        humidadeView.setText(hum.toString())
                    }
                } catch (_: Exception) {
                }
            }.start()
        }
    }

    private fun enviarChecklist(json: JSONObject, path: String) {
        val ip = getSharedPreferences("config", Context.MODE_PRIVATE)
            .getString("api_ip", "192.168.0.135")
        val address = "http://$ip:5000$path"
        try {
            val url = URL(address)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
            conn.responseCode
            conn.disconnect()
        } catch (_: Exception) {
        }
    }
}

