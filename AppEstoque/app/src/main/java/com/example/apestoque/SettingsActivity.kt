package com.example.apestoque

import android.content.Context
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val prefs = getSharedPreferences("app", Context.MODE_PRIVATE)
        val etOperador = findViewById<EditText>(R.id.etOperadorSuprimentos)
        val etApiIp = findViewById<EditText>(R.id.etApiIp)

        etOperador.setText(prefs.getString("operador_suprimentos", ""))
        etOperador.doAfterTextChanged {
            prefs.edit().putString("operador_suprimentos", it.toString()).apply()
        }

        etApiIp.setText(prefs.getString("api_ip", "192.168.0.135"))
        etApiIp.doAfterTextChanged {
            prefs.edit().putString("api_ip", it.toString()).apply()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
