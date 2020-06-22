package com.handydev.financier.activity

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.handydev.financier.R
import com.handydev.financier.fragments.BlotterFragment

class BlotterActivity2 : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.blotter2)
        if (savedInstanceState == null) {
            val blotter = BlotterFragment()
            if(intent?.extras != null) {
                blotter.arguments = intent.extras
            }
            supportFragmentManager
                    .beginTransaction()
                    .add(R.id.blotter_root_layout, BlotterFragment(), "blotter")
                    .commit()
        }
    }
}