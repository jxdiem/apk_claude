package com.jxdiem.diemgeo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jxdiem.diemgeo.security.RootStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        lifecycleScope.launch {
            val rootCheck = async(Dispatchers.IO) { RootStatus.refresh(this@SplashActivity); RootStatus.isRooted }
            delay(SPLASH_DURATION_MS)

            if (rootCheck.await()) {
                showRootBlockedDialog()
            } else {
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                finish()
            }
        }
    }

    private fun showRootBlockedDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.root_blocked_title)
            .setMessage(R.string.root_blocked_message)
            .setCancelable(false)
            .setPositiveButton(R.string.root_blocked_exit) { _, _ -> finishAffinity() }
            .show()
    }

    companion object {
        private const val SPLASH_DURATION_MS = 1800L
    }
}
