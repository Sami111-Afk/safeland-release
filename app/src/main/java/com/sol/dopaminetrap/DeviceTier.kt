package com.sol.dopaminetrap

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Detectează categoria dispozitivului pentru optimizări adaptive de baterie.
 *
 * FLAGSHIP: RAM ≥ 6GB sau MEDIA_PERFORMANCE_CLASS ≥ 31
 *   → cod actual neschimbat (cachedThreadPool + poll cu timeout)
 *
 * STANDARD: restul
 *   → thread pool mărginit + take()/poison pill + batch flush
 *
 * TODO: șterge forceStandard înainte de release.
 */
object DeviceTier {

    /** Forțează mod STANDARD pentru testare — șterge înainte de release */
    val forceStandard = AtomicBoolean(false)

    fun isStandard(context: Context): Boolean {
        if (forceStandard.get()) return true
        // Flagship: MEDIA_PERFORMANCE_CLASS definit explicit de producător
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            Build.VERSION.MEDIA_PERFORMANCE_CLASS >= 31) return false
        // Flagship: ≥ 6 GB RAM
        val memInfo = ActivityManager.MemoryInfo()
        context.getSystemService(ActivityManager::class.java).getMemoryInfo(memInfo)
        if (memInfo.totalMem >= 6L * 1024 * 1024 * 1024) return false
        return true
    }
}
