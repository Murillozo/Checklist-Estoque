package com.example.appoficina

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ChecklistFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_checklist, container, false)
        val checklistContainer: LinearLayout = view.findViewById(R.id.checklist_container)

        Thread {
            try {
                val url = URL("http://192.168.0.135:5000/json_api/projects")
                val conn = url.openConnection() as HttpURLConnection
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val json = JSONObject(response)
                val projects = json.getJSONObject("projects")
                requireActivity().runOnUiThread {
                    for (key in projects.keys()) {
                        val textView = TextView(requireContext())
                        textView.text = key
                        textView.setPadding(0, 0, 0, 16)
                        val itemsArray = projects.getJSONArray(key)
                        textView.setOnClickListener {
                            val intent = Intent(requireContext(), ChecklistDetailActivity::class.java)
                            intent.putExtra("file_name", key)
                            intent.putExtra("items_json", itemsArray.toString())
                            startActivity(intent)
                        }
                        checklistContainer.addView(textView)
                    }
                }
            } catch (_: Exception) {
            }
        }.start()

        return view
    }
}

