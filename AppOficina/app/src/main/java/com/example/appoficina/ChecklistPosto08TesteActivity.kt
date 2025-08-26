package com.example.appoficina

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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

        // 8.3 - TESTE - DADOS
        val dadosPerguntas = listOf(
            "Responsável",
            "Altitude em relação ao nivel do mar",
            "Tipo de ambiente",
            "Temperatura Ambiente",
            "Humidade relátiva",
            "Tensão de comando",
            "Tensão circuito auxiliar",
            "Tensão circuito de força",
        )
        val dadosInputs = mutableListOf<EditText>()
        dadosPerguntas.forEach { pergunta ->
            val tv = TextView(this)
            tv.text = pergunta
            container.addView(tv)
            val et = EditText(this)
            container.addView(et)
            dadosInputs.add(et)
        }

        // 8.4 - TESTE - TENSÃO APLICADA
        data class TensaoRefs(
            val unidade: EditText,
            val valor: EditText,
            val resUnidade: EditText,
            val resValor: EditText,
            val c: CheckBox,
            val nc: CheckBox,
            val na: CheckBox,
        )

        val tensaoPerguntas = listOf(
            "4.2 - Comando x Terra",
            "4.3 - Força - Fase A x BC Terra",
            "4.4 - Força - Fase B x AC Terra",
            "4.5 - Força - Fase C x AB Terra",
            "4.6 - Força - Fase ABC x Terra",
        )
        val tensaoRefs = mutableListOf<TensaoRefs>()
        tensaoPerguntas.forEach { pergunta ->
            val tv = TextView(this)
            tv.text = pergunta
            container.addView(tv)
            val row1 = LinearLayout(this)
            row1.orientation = LinearLayout.HORIZONTAL
            val unidade = EditText(this)
            unidade.hint = "Unidade"
            val valor = EditText(this)
            valor.hint = "Valor"
            row1.addView(unidade)
            row1.addView(valor)
            container.addView(row1)
            val row2 = LinearLayout(this)
            row2.orientation = LinearLayout.HORIZONTAL
            val resUni = EditText(this)
            resUni.hint = "Resultado: Unidade"
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

        // 8.5 - CONFIGURAÇÃO DE DISPOSITIVOS
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
            val tv = TextView(this)
            tv.text = pergunta
            container.addView(tv)
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

        // 8.5 - FUNCIONAIS
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
            val tv = TextView(this)
            tv.text = pergunta
            container.addView(tv)
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
                dadosPerguntas.forEachIndexed { idx, pergunta ->
                    val obj = JSONObject()
                    obj.put("numero", numero++)
                    obj.put("pergunta", pergunta)
                    val arr = JSONArray()
                    arr.put(dadosInputs[idx].text.toString())
                    obj.put("resposta", arr)
                    itens.put(obj)
                }
                tensaoPerguntas.forEachIndexed { idx, pergunta ->
                    val refs = tensaoRefs[idx]
                    val obj = JSONObject()
                    obj.put("numero", numero++)
                    obj.put("pergunta", pergunta)
                    val arr = JSONArray()
                    arr.put(refs.unidade.text.toString())
                    arr.put(refs.valor.text.toString())
                    arr.put(refs.resUnidade.text.toString())
                    arr.put(refs.resValor.text.toString())
                    arr.put(
                        when {
                            refs.c.isChecked -> "C"
                            refs.nc.isChecked -> "NC"
                            else -> "NA"
                        },
                    )
                    obj.put("resposta", arr)
                    itens.put(obj)
                }
                configPerguntas.forEachIndexed { idx, pergunta ->
                    val (c, na) = configPairs[idx]
                    val obj = JSONObject()
                    obj.put("numero", numero++)
                    obj.put("pergunta", pergunta)
                    val arr = JSONArray()
                    arr.put(if (c.isChecked) "C" else "NA")
                    obj.put("resposta", arr)
                    itens.put(obj)
                }
                funcPerguntas.forEachIndexed { idx, pergunta ->
                    val (c, na) = funcPairs[idx]
                    val obj = JSONObject()
                    obj.put("numero", numero++)
                    obj.put("pergunta", pergunta)
                    val arr = JSONArray()
                    arr.put(if (c.isChecked) "C" else "NA")
                    obj.put("resposta", arr)
                    itens.put(obj)
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

