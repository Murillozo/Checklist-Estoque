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
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL

                addView(TextView(context).apply {
                    text = item.referencia
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })

                addView(EditText(context).apply {
                    inputType = InputType.TYPE_CLASS_NUMBER
                    setText(item.quantidade.toString())
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                })
            }.also { container.addView(it) }
        }

        findViewById<Button>(R.id.btnEnviarPendencias).setOnClickListener {
            val atualizados = itens.mapIndexed { index, it ->
                val edit = (edits[index].getChildAt(1) as EditText)
                val qt = edit.text.toString().toIntOrNull() ?: it.quantidade
                Item(it.referencia, qt)
            }

            lifecycleScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        NetworkModule.api.marcarCompras(id, ComprasRequest(atualizados))
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
