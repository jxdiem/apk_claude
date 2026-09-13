package com.jxdiem.diemgeo.security

import android.content.Context
import android.os.Build
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Heuristic root/tamper detection (spec point 2): there is no Android API
 * that certifies a device is "clean", so this combines several independent
 * signals used by well-known root checkers (RootBeer-style). A device is
 * rooted or engineering-signed on ANY of these signals — false positives are
 * possible on custom ROMs or emulators (which normally ship test-keys too),
 * but that trade-off is intentional here: a fake-GPS app needs root or a
 * dev-signed build to work reliably, so treating those environments as
 * untrusted directly serves the anti-spoofing goal in point 1.
 */
object RootDetector {

    private val SU_PATHS = arrayOf(
        "/system/bin/su", "/system/xbin/su", "/sbin/su", "/system/su",
        "/su/bin/su", "/data/local/xbin/su", "/data/local/bin/su",
        "/system/sd/xbin/su", "/system/bin/failsafe/su", "/vendor/bin/su",
        "/system/app/Superuser.apk", "/system/app/SuperSU.apk"
    )

    private val ROOT_PACKAGES = arrayOf(
        "com.topjohnwu.magisk",
        "eu.chainfire.supersu",
        "com.noshufou.android.su",
        "com.noshufou.android.su.elite",
        "com.koushikdutta.superuser",
        "com.thirdparty.superuser",
        "com.yellowes.su",
        "com.kingroot.kinguser",
        "com.kingo.root",
        "com.smedialink.oneclickroot",
        "com.zhiqupk.root.global",
        "com.alephzain.framaroot"
    )

    fun isDeviceRooted(context: Context): Boolean =
        hasTestKeys() || hasSuBinary() || hasRootPackage(context) || canExecuteSu()

    private fun hasTestKeys(): Boolean = Build.TAGS?.contains("test-keys") == true

    private fun hasSuBinary(): Boolean = SU_PATHS.any { runCatching { File(it).exists() }.getOrDefault(false) }

    private fun hasRootPackage(context: Context): Boolean = ROOT_PACKAGES.any { pkg ->
        runCatching {
            context.packageManager.getPackageInfo(pkg, 0)
            true
        }.getOrDefault(false)
    }

    private fun canExecuteSu(): Boolean = runCatching {
        val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
        val output = BufferedReader(InputStreamReader(process.inputStream)).readLine()
        process.destroy()
        !output.isNullOrBlank()
    }.getOrDefault(false)
}

/** Cached result so the (mildly expensive) checks only ever run once per process. */
object RootStatus {
    @Volatile
    var isRooted: Boolean = false
        private set

    fun refresh(context: Context) {
        isRooted = RootDetector.isDeviceRooted(context.applicationContext)
    }
}
