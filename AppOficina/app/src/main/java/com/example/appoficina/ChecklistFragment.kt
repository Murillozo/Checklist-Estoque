package com.example.appoficina

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import org.json.JSONObject

class ChecklistFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_checklist, container, false)
        val checklistContainer: LinearLayout = view.findViewById(R.id.checklist_container)
        val items = loadItemsFromAssets()
        for (item in items) {
            val checkBox = CheckBox(requireContext())
            checkBox.text = item
            checklistContainer.addView(checkBox)
        }
        return view
    }

    private fun loadItemsFromAssets(): List<String> {
        val result = mutableListOf<String>()
        val assetManager = requireContext().assets
        val files = assetManager.list("")?.filter { it.endsWith(".json") } ?: emptyList()
        for (file in files) {
            val jsonStr = assetManager.open(file).bufferedReader().use { it.readText() }
            val obj = JSONObject(jsonStr)
            val array = obj.optJSONArray("items")
            if (array != null) {
                for (i in 0 until array.length()) {
                    result.add(array.optString(i))
                }
            }
        }
        return result
    }
}
