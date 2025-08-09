package com.example.appoficina

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import org.json.JSONObject
import java.io.File

class Posto01MateriaisFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_posto01_materiais, container, false)
        val listContainer: LinearLayout = view.findViewById(R.id.projetos_container)

        val baseDir = Environment.getExternalStorageDirectory()
        val jsonDir = File(baseDir, "site/json_api")
        if (jsonDir.exists()) {
            jsonDir.listFiles { file -> file.extension == "json" }?.sorted()?.forEach { file ->
                try {
                    val obj = JSONObject(file.readText())
                    val obra = obj.optString("obra", file.nameWithoutExtension)
                    val ano = obj.optString("ano", "")
                    val tv = TextView(requireContext())
                    tv.text = "$obra - $ano"
                    tv.setPadding(0, 0, 0, 16)
                    tv.setOnClickListener {
                        val intent = Intent(requireContext(), ChecklistPosto01Parte2Activity::class.java)
                        intent.putExtra("obra", obra)
                        intent.putExtra("ano", ano)
                        startActivity(intent)
                    }
                    listContainer.addView(tv)
                } catch (_: Exception) {
                }
            }
        }
        return view
    }
}

