package com.example.appoficina

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment

class ChecklistFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_checklist, container, false)
        val checklistContainer: LinearLayout = view.findViewById(R.id.checklist_container)

        val assetManager = requireContext().assets
        val files = assetManager.list("")?.filter { it.endsWith(".json") } ?: emptyList()
        for (fileName in files) {
            val textView = TextView(requireContext())
            textView.text = fileName
            textView.setPadding(0, 0, 0, 16)
            textView.setOnClickListener {
                val intent = Intent(requireContext(), ChecklistDetailActivity::class.java)
                intent.putExtra("file_name", fileName)
                startActivity(intent)
            }
            checklistContainer.addView(textView)
        }

        return view
    }
}

