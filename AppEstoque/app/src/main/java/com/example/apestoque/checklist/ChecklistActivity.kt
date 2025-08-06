package com.example.apestoque.checklist

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.apestoque.R
import com.example.apestoque.data.Solicitacao
import com.squareup.moshi.Types
import com.example.apestoque.data.Item
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.launch

class ChecklistActivity : AppCompatActivity() {
    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

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

        val btn = findViewById<Button>(R.id.btnConcluir)
        btn.setOnClickListener {
            val pendentes = solicitacao.itens.filterIndexed { index, _ -> !checks[index].isChecked }

            lifecycleScope.launch {
                try {
                    if (pendentes.isEmpty()) {
                        val intent = Intent(this@ChecklistActivity, ChecklistPosto01Activity::class.java)
                        intent.putExtra("id", solicitacao.id)
                        intent.putExtra("obra", solicitacao.obra)
                        launcher.launch(intent)
                    } else {
                        val jsonPend = moshi.adapter<List<Item>>(
                            Types.newParameterizedType(List::class.java, Item::class.java)
                        ).toJson(pendentes)
                        val intent = Intent(this@ChecklistActivity, PendenciasActivity::class.java)
                        intent.putExtra("id", solicitacao.id)
                        intent.putExtra("pendencias", jsonPend)
                        launcher.launch(intent)
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@ChecklistActivity, "Erro ao enviar", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
