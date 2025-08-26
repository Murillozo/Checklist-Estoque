package com.example.apestoque

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager2.widget.ViewPager2
import com.example.apestoque.fragments.AprovadoFragment
import com.example.apestoque.fragments.ComprasFragment
import com.example.apestoque.fragments.SolicitacoesFragment
import com.example.apestoque.fragments.RevisaoFragment
import com.example.apestoque.fragments.LogisticaFragment
import com.example.apestoque.fragments.InspecionarFragment
import com.example.apestoque.fragments.CameraFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import android.view.Menu
import android.view.MenuItem

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
            SolicitacoesFragment(),
            ComprasFragment(),
            AprovadoFragment(),
            RevisaoFragment(),
            LogisticaFragment(),
            InspecionarFragment(),
            CameraFragment()
        )
        val titles = listOf(
            "Solicitações",
            "Compras",
            "Aprovadas",
            "Revisão",
            "Logística",
            "Inspecionar",
            ""
        )
        val icons = listOf(
            null,
            null,
            null,
            null,
            null,
            null,
            android.R.drawable.ic_menu_camera
        )

        // Adapter
        pager.adapter = ViewPagerAdapter(this, frags)

        // Conecta abas ao pager
        TabLayoutMediator(tabs, pager) { tab, i ->
            tab.text = titles[i]
            icons[i]?.let { tab.setIcon(it) }
        }.attach()

        // Pull to refresh
        swipe.setOnRefreshListener {
            if (pager.currentItem != 6) {
                pager.adapter?.notifyItemChanged(pager.currentItem)
            }
            swipe.isRefreshing = false
        }

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                swipe.isEnabled = position != 6
            }
        })

        swipe.isEnabled = pager.currentItem != 6
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
