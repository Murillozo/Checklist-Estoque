package com.example.appoficina

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class AdminConfigActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_config)

        prefs = getSharedPreferences("config", MODE_PRIVATE)

        val adminUser = findViewById<EditText>(R.id.edit_admin_user)
        val adminPass = findViewById<EditText>(R.id.edit_admin_pass)
        val apiIp = findViewById<EditText>(R.id.edit_api_ip)
        val inspetorPass = findViewById<EditText>(R.id.edit_pass_inspetor)
        val pass02 = findViewById<EditText>(R.id.edit_pass_tab02)
        val pass03 = findViewById<EditText>(R.id.edit_pass_tab03)
        val pass04 = findViewById<EditText>(R.id.edit_pass_tab04)
        val pass05 = findViewById<EditText>(R.id.edit_pass_tab05)
        val pass06 = findViewById<EditText>(R.id.edit_pass_tab06)
        val pass061 = findViewById<EditText>(R.id.edit_pass_tab06_1)
        val pass07 = findViewById<EditText>(R.id.edit_pass_tab07)
        val pass08 = findViewById<EditText>(R.id.edit_pass_tab08)
        val pass08Teste = findViewById<EditText>(R.id.edit_pass_tab08_teste)
        val pass09 = findViewById<EditText>(R.id.edit_pass_tab09)

        adminUser.setText(prefs.getString("admin_user", "admin"))
        adminPass.setText(prefs.getString("admin_pass", "admin"))
        apiIp.setText(prefs.getString("api_ip", "192.168.0.135"))
        inspetorPass.setText(prefs.getString("pass_inspetor", "inspetor"))
        pass02.setText(prefs.getString("pass_02", ""))
        pass03.setText(prefs.getString("pass_03", ""))
        pass04.setText(prefs.getString("pass_04", ""))
        pass05.setText(prefs.getString("pass_05", ""))
        pass06.setText(prefs.getString("pass_06", ""))
        pass061.setText(prefs.getString("pass_06_1", ""))
        pass07.setText(prefs.getString("pass_07", ""))
        pass08.setText(prefs.getString("pass_08", ""))
        pass08Teste.setText(prefs.getString("pass_08_teste", ""))
        pass09.setText(prefs.getString("pass_09", ""))

        findViewById<Button>(R.id.button_save).setOnClickListener {
            prefs.edit().apply {
                putString("admin_user", adminUser.text.toString())
                putString("admin_pass", adminPass.text.toString())
                putString("api_ip", apiIp.text.toString())
                putString("pass_inspetor", inspetorPass.text.toString())
                putString("pass_02", pass02.text.toString())
                putString("pass_03", pass03.text.toString())
                putString("pass_04", pass04.text.toString())
                putString("pass_05", pass05.text.toString())
                putString("pass_06", pass06.text.toString())
                putString("pass_06_1", pass061.text.toString())
                putString("pass_07", pass07.text.toString())
                putString("pass_08", pass08.text.toString())
                putString("pass_08_teste", pass08Teste.text.toString())
                putString("pass_09", pass09.text.toString())
                apply()
            }
            finish()
        }
    }
}