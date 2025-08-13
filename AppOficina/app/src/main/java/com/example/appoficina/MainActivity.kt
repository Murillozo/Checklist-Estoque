package com.example.appoficina

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    private var previousTab = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("config", MODE_PRIVATE)

        val adminButton: TextView = findViewById(R.id.admin_button)
        val inspetorButton: Button = findViewById(R.id.inspetor_button)
        val refreshButton: Button = findViewById(R.id.btnRefresh)
        val viewPager: ViewPager2 = findViewById(R.id.view_pager)
        val tabLayout: TabLayout = findViewById(R.id.tab_layout)

        adminButton.setOnClickListener {
            val userInput = EditText(this)
            val passInput = EditText(this).apply {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                addView(userInput)
                addView(passInput)
            }
            AlertDialog.Builder(this)
                .setTitle("Login Admin")
                .setView(layout)
                .setPositiveButton("OK") { _, _ ->
                    val savedUser = prefs.getString("admin_user", "admin")
                    val savedPass = prefs.getString("admin_pass", "admin")
                    if (userInput.text.toString() == savedUser && passInput.text.toString() == savedPass) {
                        startActivity(Intent(this, AdminConfigActivity::class.java))
                    } else {
                        Toast.makeText(this, "Credenciais inválidas", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        inspetorButton.setOnClickListener {
            val requiredPass = prefs.getString("pass_inspetor", "inspetor")
            val input = EditText(this).apply {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            AlertDialog.Builder(this)
                .setTitle("Senha Inspetor")
                .setView(input)
                .setPositiveButton("OK") { _, _ ->
                    if (input.text.toString() == requiredPass) {
                        startActivity(Intent(this, InspetorActivity::class.java))
                    } else {
                        Toast.makeText(this, "Senha incorreta", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        refreshButton.setOnClickListener {
            recreate()
        }

        val fragments: List<Fragment> = listOf(
            Posto01MateriaisFragment(),
            Posto02OficinaFragment(),
            Posto03PreMontagemFragment(),
            Posto04BarramentoFragment(),
            Posto05CablagemFragment(),
            Posto06PreMontagemFragment(),
            Posto06Cablagem02Fragment(),
            SimpleTextFragment.newInstance("POSTO - 09 EXPEDIÇÃO")
        )
        val titles = listOf(
            "01 - Posto 01 - Materiais",
            "02 - POSTO - 02 OFICINA",
            "03 - POSTO - 03 PRÉ-MONTAGEM - 01",
            "04 - POSTO - 04 BARRAMENTO",
            "05 - POSTO - 05 CABLAGEM - 01",
            "06 - POSTO - 06 PRÉ-MONTAGEM - 02",
            "06.1 - POSTO - 06 CABLAGEM - 02",
            "POSTO - 09 EXPEDIÇÃO"
        )
        val tabKeys = listOf<String?>(
            null,
            "pass_02",
            "pass_03",
            "pass_04",
            "pass_05",
            "pass_06",
            "pass_06_1",
            "pass_09"
        )

        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = fragments.size
            override fun createFragment(position: Int): Fragment = fragments[position]
        }

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = titles[position]
        }.attach()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val key = tabKeys[position]
                if (key != null) {
                    val requiredPass = prefs.getString(key, "")
                    if (!requiredPass.isNullOrEmpty()) {
                        val input = EditText(this@MainActivity).apply {
                            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                        }
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("Senha necessária")
                            .setView(input)
                            .setPositiveButton("OK") { _, _ ->
                                if (input.text.toString() == requiredPass) {
                                    previousTab = position
                                } else {
                                    Toast.makeText(this@MainActivity, "Senha incorreta", Toast.LENGTH_SHORT).show()
                                    viewPager.post { viewPager.currentItem = previousTab }
                                }
                            }
                            .setNegativeButton("Cancelar") { _, _ ->
                                viewPager.post { viewPager.currentItem = previousTab }
                            }
                            .setOnCancelListener {
                                viewPager.post { viewPager.currentItem = previousTab }
                            }
                            .show()
                    } else {
                        previousTab = position
                    }
                } else {
                    previousTab = position
                }
            }
        })
    }
}