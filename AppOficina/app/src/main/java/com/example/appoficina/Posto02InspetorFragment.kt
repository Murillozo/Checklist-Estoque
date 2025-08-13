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
import java.net.URLEncoder

class Posto02InspetorFragment : Fragment() {
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
            val urls = listOf(
                "http://$ip:5000/json_api/posto02/insp/projects",
            )
            var loaded = false
            for (address in urls) {
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
                                Thread {
                                    val urlsChecklist = listOf(
                                        "http://$ip:5000/json_api/posto02/checklist?obra=" +
                                            URLEncoder.encode(obra, "UTF-8") +
                                            "&ano=" + URLEncoder.encode(ano, "UTF-8"),
                                    )
                                    var divergencias: JSONArray? = null
                                    var found = false
                                    for (addr in urlsChecklist) {
                                        try {
                                            val url = URL(addr)
                                            val conn = url.openConnection() as HttpURLConnection
                                            val response = conn.inputStream.bufferedReader().use { it.readText() }
                                            conn.disconnect()
                                            val json = JSONObject(response)
                                            divergencias = json.optJSONObject("posto02")?.optJSONArray("divergencias")
                                            found = true
                                            break
                                        } catch (_: Exception) {
                                        }
                                    }
                                    if (!isAdded) return@Thread
                                    activity?.runOnUiThread {
                                        if (found && divergencias != null && divergencias!!.length() > 0) {
                                            val intent = Intent(requireContext(), PreviewDivergenciasActivity::class.java)
                                            intent.putExtra("obra", obra)
                                            intent.putExtra("ano", ano)
                                            intent.putExtra("divergencias", divergencias.toString())
                                            intent.putExtra("tipo", "insp_posto02")
                                            startActivity(intent)
                                        } else {
                                            val input = EditText(requireContext())
                                            AlertDialog.Builder(requireContext())
                                                .setTitle("Nome do inspetor")
                                                .setView(input)
                                                .setPositiveButton("OK") { _, _ ->
                                                    val nome = input.text.toString()
                                                    val intent = Intent(requireContext(), ChecklistPosto02InspActivity::class.java)
                                                    intent.putExtra("obra", obra)
                                                    intent.putExtra("ano", ano)
                                                    intent.putExtra("inspetor", nome)
                                                    startActivity(intent)
                                                }
                                                .setNegativeButton("Cancelar", null)
                                                .show()
                                        }
                                    }
                                }.start()
                            }
                            listContainer.addView(tv)
                        }
                    }
                    loaded = true
                    break
                } catch (_: Exception) {
                    // tenta proximo endereço
                }
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
