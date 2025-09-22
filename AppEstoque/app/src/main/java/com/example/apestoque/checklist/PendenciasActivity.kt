package com.example.apestoque.checklist

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.apestoque.R
import com.example.apestoque.data.ComprasRequest
import com.squareup.moshi.Types
import com.example.apestoque.data.Item
import com.example.apestoque.data.NetworkModule
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PendenciasActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pendencias)

        val id = intent.getIntExtra("id", -1)
        if (id == -1) return finish()

        val json = intent.getStringExtra("pendencias")
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter<List<Item>>(Types.newParameterizedType(List::class.java, Item::class.java))
        val itens = adapter.fromJson(json ?: "[]") ?: emptyList()

        val container = findViewById<LinearLayout>(R.id.containerPendencias)
        val edits = itens.map { item ->
            val editText = EditText(this).apply {
                inputType = InputType.TYPE_CLASS_NUMBER
                hint = "0"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL

                addView(TextView(context).apply {
                    text = "${item.referencia} - solicitado ${item.quantidade}"
                })

                addView(TextView(context).apply {
                    text = "Quantidade em estoque"
                })

                addView(editText)
            }.also { container.addView(it) }

            editText
        }

        findViewById<Button>(R.id.btnEnviarPendencias).setOnClickListener {
            val atualizados = mutableListOf<Item>()

            for (i in itens.indices) {
                val item = itens[i]
                val estoque = edits[i].text.toString().toIntOrNull() ?: 0
                if (estoque > item.quantidade) {
                    Toast.makeText(
                        this,
                        "Quantidade para ${item.referencia} maior que o solicitado",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                atualizados.add(Item(item.referencia, item.quantidade - estoque))
            }

            lifecycleScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        NetworkModule.api(this@PendenciasActivity).marcarCompras(id, ComprasRequest(atualizados))
                    }
                    setResult(Activity.RESULT_OK)
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this@PendenciasActivity, "Erro ao enviar", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
