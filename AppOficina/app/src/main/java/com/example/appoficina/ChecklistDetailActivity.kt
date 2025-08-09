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
        val itemsJson = intent.getStringExtra("items_json")
        val itensArray = if (itemsJson != null) JSONArray(itemsJson) else JSONArray()
        val checkBoxes = mutableListOf<CheckBox>()
        for (i in 0 until itensArray.length()) {
            val pergunta = itensArray.getString(i)
            val checkBox = CheckBox(this)
            checkBox.text = pergunta
            container.addView(checkBox)
            checkBoxes.add(checkBox)
        }

        val saveButton: Button = findViewById(R.id.save_button)
        saveButton.setOnClickListener {
            val resultArray = JSONArray()
            for (i in 0 until itensArray.length()) {
                val cb = checkBoxes[i]
                val itemObj = JSONObject()
                itemObj.put("item", itensArray.getString(i))
                itemObj.put("checked", cb.isChecked)
                resultArray.put(itemObj)
            }
            val obj = JSONObject()
            obj.put("items", resultArray)
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

