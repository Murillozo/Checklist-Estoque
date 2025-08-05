package com.example.apestoque.checklist

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.apestoque.R
import com.example.apestoque.data.ComprasRequest
import com.example.apestoque.data.Item
import com.example.apestoque.data.NetworkModule
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChecklistPosto01Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checklist_posto01)

        val id = intent.getIntExtra("id", -1)
        if (id == -1) return finish()
        val jsonPend = intent.getStringExtra("pendentes")
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val pendentes = jsonPend?.let {
            val type = Types.newParameterizedType(List::class.java, Item::class.java)
            moshi.adapter<List<Item>>(type).fromJson(it)
        }

        val cbC = findViewById<CheckBox>(R.id.cbC)
        val cbNC = findViewById<CheckBox>(R.id.cbNC)

        cbC.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) cbNC.isChecked = false
        }
        cbNC.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) cbC.isChecked = false
        }

        findViewById<Button>(R.id.btnConcluirPosto01).setOnClickListener {
            val isC = cbC.isChecked
            val isNC = cbNC.isChecked
            if (!isC && !isNC) {
                Toast.makeText(this, "Selecione uma opção", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        if (pendentes == null) {
                            NetworkModule.api.aprovarSolicitacao(id)
                        } else {
                            NetworkModule.api.marcarCompras(id, ComprasRequest(pendentes))
                        }
                    }
                    Toast.makeText(
                        this@ChecklistPosto01Activity,
                        "Checklist concluído",
                        Toast.LENGTH_LONG
                    ).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this@ChecklistPosto01Activity, "Erro ao concluir", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

}
