package com.example.apestoque.checklist

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.apestoque.R
import com.example.apestoque.data.NetworkModule
import com.example.apestoque.data.Solicitacao
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChecklistActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checklist)

        val json = intent.getStringExtra("solicitacao")
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(Solicitacao::class.java)
        val solicitacao = adapter.fromJson(json ?: "") ?: return finish()

        val container = findViewById<LinearLayout>(R.id.containerChecklist)
        val checks = solicitacao.itens.map { item ->
            CheckBox(this).apply {
                text = "${item.referencia} × ${item.quantidade}"
            }
        }
        checks.forEach { container.addView(it) }

        val btn = findViewById<Button>(R.id.btnAprovar)
        btn.setOnClickListener {
            if (checks.all { it.isChecked }) {
                lifecycleScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            NetworkModule.api.aprovarSolicitacao(solicitacao.id)
                        }
                        setResult(Activity.RESULT_OK)
                        finish()
                    } catch (e: Exception) {
                        Toast.makeText(this@ChecklistActivity, "Erro ao aprovar", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Marque todos os itens", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
