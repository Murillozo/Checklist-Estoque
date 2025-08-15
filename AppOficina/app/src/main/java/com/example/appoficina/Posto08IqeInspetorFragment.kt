package com.example.appoficina

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class Posto08IqeInspetorFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_posto02_oficina, container, false)
        val listContainer: LinearLayout = view.findViewById(R.id.projetos_container)

        Thread {
            val ip = requireContext().getSharedPreferences("config", Context.MODE_PRIVATE)
                .getString("api_ip", "192.168.0.135")
            val address = "http://$ip:5000/json_api/posto08_iqe/projects"
            var loaded = false
            try {
                val url = URL(address)
                val conn = url.openConnection() as HttpURLConnection
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val projetos = JSONObject(response).optJSONArray("projetos") ?: JSONArray()
                if (!isAdded) return@Thread
                activity?.runOnUiThread {
                    listContainer.removeAllViews()
                    for (i in 0 until projetos.length()) {
                        val obj = projetos.getJSONObject(i)
                        val obra = obj.optString("obra")
                        val ano = obj.optString("ano")
                        val tv = TextView(requireContext())
                        tv.text = String.format("%02d - %s - %s", i + 1, obra, ano)
                        tv.setPadding(0, 0, 0, 16)
                        tv.setOnClickListener {
                            val input = EditText(requireContext())
                            AlertDialog.Builder(requireContext())
                                .setTitle("Nome do inspetor")
                                .setView(input)
                                .setPositiveButton("OK") { _, _ ->
                                    val nome = input.text.toString()
                                    val intent = Intent(requireContext(), ChecklistPosto08IqeActivity::class.java)
                                    intent.putExtra("obra", obra)
                                    intent.putExtra("ano", ano)
                                    intent.putExtra("inspetor", nome)
                                    startActivity(intent)
                                }
                                .setNegativeButton("Cancelar", null)
                                .show()
                        }
                        listContainer.addView(tv)
                    }
                }
                loaded = true
            } catch (_: Exception) {
            }
            if (!loaded && isAdded) {
                activity?.runOnUiThread {
                    listContainer.removeAllViews()
                    val tv = TextView(requireContext())
                    tv.text = "Não foi possível carregar os projetos"
                    listContainer.addView(tv)
                }
            }
        }.start()

        return view
    }
}
