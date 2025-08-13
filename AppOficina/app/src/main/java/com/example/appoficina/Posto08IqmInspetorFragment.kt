package com.example.appoficina

import android.content.Context
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
import java.net.URLEncoder

class Posto08IqmInspetorFragment : Fragment() {
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
            val address = "http://$ip:5000/json_api/posto08_iqm/projects"
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
                            Thread {
                                val addr = "http://$ip:5000/json_api/posto08_iqm/checklist?obra=" +
                                    URLEncoder.encode(obra, "UTF-8")
                                var itens: JSONArray? = null
                                var found = false
                                try {
                                    val u = URL(addr)
                                    val c = u.openConnection() as HttpURLConnection
                                    val resp = c.inputStream.bufferedReader().use { it.readText() }
                                    c.disconnect()
                                    val json = JSONObject(resp)
                                    val root = json.optJSONObject("posto08_iqm") ?: json
                                    itens = root.optJSONArray("itens")
                                    found = true
                                } catch (_: Exception) {
                                }
                                if (!isAdded) return@Thread
                                activity?.runOnUiThread {
                                    if (found && itens != null) {
                                        val divergencias = JSONArray()
                                        for (j in 0 until itens!!.length()) {
                                            val item = itens!!.getJSONObject(j)
                                            val respostas = item.optJSONObject("respostas") ?: JSONObject()
                                            val funcoesNc = JSONArray()
                                            val funcoes = arrayOf("montador", "produção", "inspetor")
                                            for (func in funcoes) {
                                                val arr = respostas.optJSONArray(func) ?: JSONArray()
                                                for (k in 0 until arr.length()) {
                                                    val r = arr.optString(k).replace(".", "").trim().uppercase()
                                                    if (r == "NC") {
                                                        funcoesNc.put(func)
                                                        break
                                                    }
                                                }
                                            }
                                            if (funcoesNc.length() > 0) {
                                                val prev = JSONObject()
                                                prev.put("numero", item.optInt("numero"))
                                                prev.put("pergunta", item.optString("pergunta"))
                                                prev.put("posto", "Posto 08 IQM")
                                                prev.put("funcoes", funcoesNc)
                                                divergencias.put(prev)
                                            }
                                        }
                                        val intent = Intent(requireContext(), PreviewDivergenciasActivity::class.java)
                                        intent.putExtra("obra", obra)
                                        intent.putExtra("ano", ano)
                                        intent.putExtra("divergencias", divergencias.toString())
                                        intent.putExtra("tipo", "insp_posto08_iqm")
                                        startActivity(intent)
                                    }
                                }
                            }.start()
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
