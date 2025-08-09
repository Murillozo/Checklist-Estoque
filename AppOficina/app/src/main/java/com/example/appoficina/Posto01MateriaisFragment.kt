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
            try {
                val url = URL("http://192.168.0.135:5000/json_api/projects")
                val conn = url.openConnection() as HttpURLConnection
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()

                val projetos = JSONObject(response).optJSONArray("projetos") ?: JSONArray()
                requireActivity().runOnUiThread {
                    for (i in 0 until projetos.length()) {
                        val obj = projetos.getJSONObject(i)
                        val obra = obj.optString("obra")
                        val ano = obj.optString("ano")
                        val tv = TextView(requireContext())
                        tv.text = String.format("%02d - %s - %s", i + 1, obra, ano)
                        tv.setPadding(0, 0, 0, 16)
                        tv.setOnClickListener {
                            val intent = Intent(requireContext(), ChecklistPosto01Parte2Activity::class.java)
                            intent.putExtra("obra", obra)
                            intent.putExtra("ano", ano)
                            startActivity(intent)
                        }
                        listContainer.addView(tv)
                    }
                }
            } catch (_: Exception) {
            }
        }.start()

        return view
    }
}
