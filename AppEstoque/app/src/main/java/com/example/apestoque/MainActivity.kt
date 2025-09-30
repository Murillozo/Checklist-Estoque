package com.example.apestoque

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager2.widget.ViewPager2
import com.example.apestoque.fragments.AprovadoFragment
import com.example.apestoque.fragments.CameraFragment
import com.example.apestoque.fragments.ComprasFragment
import com.example.apestoque.fragments.InspecionarFragment
import com.example.apestoque.fragments.LogisticaFragment
import com.example.apestoque.fragments.RevisaoFragment
import com.example.apestoque.fragments.SolicitacoesFragment
import com.example.apestoque.util.InspecaoPollingWorker
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(
                    this,
                    R.string.notification_permission_denied,
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        InspecaoPollingWorker.schedule(applicationContext)
        maybeRequestNotificationPermission()

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

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val permission = Manifest.permission.POST_NOTIFICATIONS
        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                // Already granted
            }

            shouldShowRequestPermissionRationale(permission) -> {
                Toast.makeText(
                    this,
                    R.string.notification_permission_rationale,
                    Toast.LENGTH_LONG
                ).show()
                requestNotificationPermission.launch(permission)
            }

            else -> requestNotificationPermission.launch(permission)
        }
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
