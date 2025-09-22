package com.example.appoficina

import android.media.MediaPlayer
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class InspetorActivity : AppCompatActivity() {
    private var tabLongPressPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inspetor)

        // Botão de refresh
        val refreshButton: Button = findViewById(R.id.btnRefresh)
        val viewPager: ViewPager2 = findViewById(R.id.view_pager)
        val tabLayout: TabLayout = findViewById(R.id.tab_layout)

        refreshButton.setOnClickListener { recreate() }

        // Prepara o MediaPlayer para tocar o som
        tabLongPressPlayer = MediaPlayer.create(this, R.raw.tab_long_press_sound).apply {
            setOnCompletionListener { it.seekTo(0) } // Volta para o início quando termina
        }

        // Lista de fragments
        val fragments: List<Fragment> = listOf(
            Posto02InspetorFragment(),
            Posto03PreMontagemInspetorFragment(),
            Posto04BarramentoInspetorFragment(),
            Posto05CablagemInspetorFragment(),
            Posto06PreMontagemInspetorFragment(),
            Posto06Cablagem02InspetorFragment(),
            Posto08IqmInspetorFragment(),
            Posto08IqeInspetorFragment(),
            Posto08TesteInspetorFragment()
        )

        // Títulos das abas
        val titles = listOf(
            "02 - POSTO - 02 OFICINA",
            "03 - POSTO - 03 PRÉ-MONTAGEM - 01",
            "04 - POSTO - 04 BARRAMENTO",
            "05 - POSTO - 05 CABLAGEM - 01",
            "06 - POSTO - 06 PRÉ-MONTAGEM - 02",
            "06.1 - POSTO - 06 CABLAGEM - 02",
            "07 - POSTO - 08 IQM",
            "08 - POSTO - 08 IQE",
            "POSTO - 08 TESTE"
        )

        // Adapter do ViewPager
        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = fragments.size
            override fun createFragment(position: Int): Fragment = fragments[position]
        }

        // Liga o TabLayout ao ViewPager
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = titles[position]
        }.attach()

        // Adiciona listener de long press na aba do IQM
        val iqmTabIndex = titles.indexOf("07 - POSTO - 08 IQM")
        if (iqmTabIndex != -1) {
            tabLayout.post {
                val tabStrip = tabLayout.getChildAt(0) as? ViewGroup ?: return@post
                val tabView = tabStrip.getChildAt(iqmTabIndex)
                tabView?.setOnLongClickListener {
                    tabLongPressPlayer?.let { player ->
                        if (player.isPlaying) player.seekTo(0)
                        player.start()
                    }
                    true
                }
            }
        }
    }

    override fun onDestroy() {
        // Libera o MediaPlayer para evitar vazamentos de memória
        tabLongPressPlayer?.release()
        tabLongPressPlayer = null
        super.onDestroy()
    }
}
