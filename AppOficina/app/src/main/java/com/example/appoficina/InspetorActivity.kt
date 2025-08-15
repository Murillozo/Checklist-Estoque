package com.example.appoficina

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class InspetorActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inspetor)
        
        val refreshButton: Button = findViewById(R.id.btnRefresh)
        val viewPager: ViewPager2 = findViewById(R.id.view_pager)
        val tabLayout: TabLayout = findViewById(R.id.tab_layout)

        refreshButton.setOnClickListener { recreate() }

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

        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = fragments.size
            override fun createFragment(position: Int): Fragment = fragments[position]
        }

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = titles[position]
        }.attach()
    }
}
