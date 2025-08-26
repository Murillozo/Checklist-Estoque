package com.example.appoficina

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class SimpleTextFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_simple_text, container, false)
        val textView: TextView = view.findViewById(R.id.text_simple)
        textView.text = arguments?.getString("text") ?: ""
        return view
    }

    companion object {
        fun newInstance(text: String): SimpleTextFragment {
            val fragment = SimpleTextFragment()
            fragment.arguments = Bundle().apply {
                putString("text", text)
            }
            return fragment
        }
    }
}
