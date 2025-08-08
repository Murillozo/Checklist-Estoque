package com.example.appoficina

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.graphics.Typeface
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import org.json.JSONObject
import java.net.URL
import android.os.Handler
import android.os.Looper

class ChecklistFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_checklist, container, false)
        val checklistContainer: LinearLayout = view.findViewById(R.id.checklist_container)
        Thread {
            val projects = loadProjectsFromServer()
            Handler(Looper.getMainLooper()).post {
                for ((name, items) in projects) {
                    val title = TextView(requireContext())
                    title.text = name
                    title.setTypeface(null, Typeface.BOLD)
                    checklistContainer.addView(title)
                    for (item in items) {
                        val checkBox = CheckBox(requireContext())
                        checkBox.text = item
                        checklistContainer.addView(checkBox)
                    }
                }
            }
        }.start()
        return view
    }

    private fun loadProjectsFromServer(): Map<String, List<String>> {
        val baseUrl = "http://192.168.0.135:5000/site/json_api/"
        val listingHtml = URL(baseUrl).readText()
        val fileRegex = Regex("href=\\\"([^\\\"]+\\.json)\\\")
        val files = fileRegex.findAll(listingHtml).map { it.groupValues[1] }.toList()
        val result = mutableMapOf<String, List<String>>()
        for (file in files) {
            val jsonStr = URL(baseUrl + file).readText()
            val obj = JSONObject(jsonStr)
            val array = obj.optJSONArray("items")
            val items = mutableListOf<String>()
            if (array != null) {
                for (i in 0 until array.length()) {
                    items.add(array.optString(i))
                }
            }
            result[file] = items
        }
        return result
    }
}
