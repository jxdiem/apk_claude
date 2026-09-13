package com.jxdiem.diemgeo.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.jxdiem.diemgeo.databinding.ActivityManualBinding

class ManualActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityManualBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }
}
