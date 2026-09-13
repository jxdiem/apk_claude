package com.jxdiem.diemgeo.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.jxdiem.diemgeo.databinding.ActivityManualBinding

class ManualActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityManualBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // targetSdk 35+ forces edge-to-edge: without this, the toolbar and
        // the last lines of the manual would sit under the status/gesture bars.
        val toolbarInitialTopPadding = binding.toolbar.paddingTop
        val scrollInitialBottomPadding = binding.scrollContent.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(view.paddingLeft, toolbarInitialTopPadding + bars.top, view.paddingRight, view.paddingBottom)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.scrollContent) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, scrollInitialBottomPadding + bars.bottom)
            insets
        }
    }
}
