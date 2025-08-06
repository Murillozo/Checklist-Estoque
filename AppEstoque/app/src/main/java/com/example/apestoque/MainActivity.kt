package com.example.apestoque

import android.content.Context
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager2.widget.ViewPager2
import com.example.apestoque.fragments.AprovadoFragment
import com.example.apestoque.fragments.ComprasFragment
import com.example.apestoque.fragments.SolicitacoesFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Toolbar
        findViewById<Toolbar>(R.id.toolbar).also { setSupportActionBar(it) }

        // Operador de Suprimentos
        val prefs = getSharedPreferences("app", Context.MODE_PRIVATE)
        val etOperador = findViewById<EditText>(R.id.etOperadorSuprimentos)
        etOperador.setText(prefs.getString("operador_suprimentos", ""))
        etOperador.doAfterTextChanged {
            prefs.edit().putString("operador_suprimentos", it.toString()).apply()
        }

        // Views
        val swipe = findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        val pager = findViewById<ViewPager2>(R.id.viewPager)
        val tabs  = findViewById<TabLayout>(R.id.tabLayout)

        // Fragments + Titles
        val frags: List<Fragment> = listOf(
            SolicitacoesFragment(),
            ComprasFragment(),
            AprovadoFragment()
        )
        val titles = listOf("Solicitações", "Compras", "Aprovadas")

        // Adapter
        pager.adapter = ViewPagerAdapter(this, frags)

        // Conecta abas ao pager
        TabLayoutMediator(tabs, pager) { tab, i ->
            tab.text = titles[i]
        }.attach()

        // Pull to refresh
        swipe.setOnRefreshListener {
            pager.adapter?.notifyItemChanged(pager.currentItem)
            swipe.isRefreshing = false
        }
    }
}
