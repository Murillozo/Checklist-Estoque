package com.example.apestoque

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.apestoque.adapter.DivergenciaAdapter
import com.example.apestoque.data.JsonNetworkModule
import com.example.apestoque.data.ReenvioRequest
import com.example.apestoque.data.RevisaoChecklist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RevisaoDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_revisao_detail)

        val checklist = intent.getSerializableExtra("revisao") as? RevisaoChecklist
        if (checklist == null) {
            finish()
            return
        }

        findViewById<TextView>(R.id.tvTitulo).text = "${checklist.obra} (${checklist.ano})"
        val rv = findViewById<RecyclerView>(R.id.rvDivs)
        rv.adapter = DivergenciaAdapter(checklist.divergencias)

        val btn = findViewById<Button>(R.id.btnReenviar)
        btn.setOnClickListener {
            lifecycleScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        JsonNetworkModule.api.reenviarChecklist(
                            ReenvioRequest(checklist.obra, checklist.ano)
                        )
                    }
                    Toast.makeText(this@RevisaoDetailActivity, "Enviado", Toast.LENGTH_SHORT).show()
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this@RevisaoDetailActivity, "Erro ao enviar", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
