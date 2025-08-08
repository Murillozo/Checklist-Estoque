package com.example.appoficina

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class ChecklistDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checklist_detail)

        val container: LinearLayout = findViewById(R.id.checklist_detail_container)
        val fileName = intent.getStringExtra("file_name")
        val jsonStr = if (fileName != null) {
            assets.open(fileName).bufferedReader().use { it.readText() }
        } else {
            "{}"
        }
        val obj = JSONObject(jsonStr)
        val itensArray = obj.optJSONArray("itens")
        val checkBoxes = mutableListOf<CheckBox>()
        if (itensArray != null) {
            for (i in 0 until itensArray.length()) {
                val itemObj = itensArray.getJSONObject(i)
                val pergunta = itemObj.optString("pergunta")
                val respostaArray = itemObj.optJSONArray("resposta")
                val checkBox = CheckBox(this)
                checkBox.text = pergunta
                if (respostaArray != null && respostaArray.toString().contains("C")) {
                    checkBox.isChecked = true
                }
                container.addView(checkBox)
                checkBoxes.add(checkBox)
            }
        }

        val saveButton: Button = findViewById(R.id.save_button)
        saveButton.setOnClickListener {
            if (itensArray != null) {
                for (i in 0 until itensArray.length()) {
                    val itemObj = itensArray.getJSONObject(i)
                    val cb = checkBoxes[i]
                    val newRes = if (cb.isChecked) JSONArray(listOf("C")) else JSONArray()
                    itemObj.put("resposta", newRes)
                }
            }
            Thread {
                sendChecklistToServer(obj)
            }.start()
            finish()
        }
    }

    private fun sendChecklistToServer(json: JSONObject) {
        val url = URL("http://192.168.0.135:5000/json_api/upload")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        val writer = OutputStreamWriter(conn.outputStream)
        writer.write(json.toString())
        writer.flush()
        writer.close()
        conn.responseCode
        conn.disconnect()
    }
}

