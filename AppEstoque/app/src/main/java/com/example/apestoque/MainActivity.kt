package com.example.apestoque

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager2.widget.ViewPager2
import com.example.apestoque.fragments.EstoqueFragment
import com.example.apestoque.fragments.HistoricoFragment
import com.example.apestoque.fragments.ComprasFragment
import com.example.apestoque.fragments.AprovadoFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import androidx.fragment.app.Fragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Toolbar
        findViewById<Toolbar>(R.id.toolbar).also { setSupportActionBar(it) }

        // Views
        val swipe = findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        val pager = findViewById<ViewPager2>(R.id.viewPager)
        val tabs  = findViewById<TabLayout>(R.id.tabLayout)

        // Fragments + Titles
        val frags: List<Fragment> = listOf(
            EstoqueFragment(),
            ComprasFragment(),
            AprovadoFragment(),
            HistoricoFragment()
        )
        val titles = listOf("Solicitaçãoes", "Compras", "Aprovado")

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
