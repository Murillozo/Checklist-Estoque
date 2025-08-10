package com.example.appoficina

import android.os.Bundle
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

        val viewPager: ViewPager2 = findViewById(R.id.view_pager)
        val tabLayout: TabLayout = findViewById(R.id.tab_layout)

        val fragments: List<Fragment> = listOf(
            Posto02OficinaFragment(),
            SimpleTextFragment.newInstance("03 - POSTO - 03 PRÉ-MONTAGEM - 01"),
            SimpleTextFragment.newInstance("04 - POSTO - 04 BARRAMENTO"),
            SimpleTextFragment.newInstance("05 - POSTO - 05 CABLAGEM - 01"),
            SimpleTextFragment.newInstance("06 - POSTO - 06 PRÉ-MONTAGEM - 02"),
            SimpleTextFragment.newInstance("06.1 - POSTO - 06 CABLAGEM - 02"),
            SimpleTextFragment.newInstance("07 - POSTO - 08 IQM"),
            SimpleTextFragment.newInstance("08 - POSTO - 08 IQE"),
            SimpleTextFragment.newInstance("POSTO - 08 TESTE")
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
