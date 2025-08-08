package com.example.appoficina

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import android.graphics.Typeface
import org.json.JSONObject

class ChecklistFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_checklist, container, false)
        val checklistContainer: LinearLayout = view.findViewById(R.id.checklist_container)
        val projects = loadProjectsFromAssets()
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
        return view
    }

    private fun loadProjectsFromAssets(): Map<String, List<String>> {
        val assetManager = requireContext().assets
        val files = assetManager.list("")?.filter { it.endsWith(".json") } ?: emptyList()
        val result = mutableMapOf<String, List<String>>()
        for (file in files) {
            val jsonStr = assetManager.open(file).bufferedReader().use { it.readText() }
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
