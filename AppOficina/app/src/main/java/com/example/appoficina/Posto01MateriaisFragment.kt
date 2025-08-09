package com.example.appoficina

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class Posto01MateriaisFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_posto01_materiais, container, false)
        val listContainer: LinearLayout = view.findViewById(R.id.projetos_container)

        Thread {
            val urls = listOf(
                "http://10.0.2.2:5000/json_api/projects",
                "http://192.168.0.151:5000/json_api/projects",
                "http://192.168.0.135:5000/json_api/projects"
            )
            var loaded = false
            for (address in urls) {
                try {
                    val url = URL(address)
                    val conn = url.openConnection() as HttpURLConnection
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    conn.disconnect()

                    val projetos = JSONObject(response).optJSONArray("projetos") ?: JSONArray()
                    requireActivity().runOnUiThread {
                        listContainer.removeAllViews()
                        for (i in 0 until projetos.length()) {
                            val obj = projetos.getJSONObject(i)
                            val obra = obj.optString("obra")
                            val ano = obj.optString("ano")
                            val tv = TextView(requireContext())
                            tv.text = String.format("%02d - %s - %s", i + 1, obra, ano)
                            tv.setPadding(0, 0, 0, 16)
                            tv.setOnClickListener { _: View ->
                                val intent: Intent = Intent(requireContext(), ChecklistPosto01Parte2Activity::class.java)
                                intent.putExtra("obra", obra)
                                intent.putExtra("ano", ano)
                                startActivity(intent)
                            }
                            listContainer.addView(tv)
                        }
                    }
                    loaded = true
                    break
                } catch (_: Exception) {
                    // tenta proximo endereco
                }
            }
            if (!loaded) {
                requireActivity().runOnUiThread {
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