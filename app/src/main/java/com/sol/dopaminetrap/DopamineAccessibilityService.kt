package com.sol.dopaminetrap

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.sol.dopaminetrap.analysis.ContentAnalyzer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Două responsabilități:
 * 1. Detectează tab-ul Shorts din YouTube →
 *actualizează flag VPN (comportament existent)
 * 2. Extrage text vizibil din TikTok / Instagram / YouTube → ContentAnalyzer → Room DB
 */
class DopamineAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "DopamineA11y"
        private const val MAX_DEPTH = 8
        private const val SWIPE_DELAY_MS = 4000L
        private const val FOREGROUND_TIMEOUT_MS = 5_000L  // 5s fara events → app e in background

        val trainingModeActive = AtomicBoolean(false)
    }

    private val handler = Handler(Looper.getMainLooper())
    private var swipePending = false

    private val youtubePkg = ProtectedApp.YOUTUBE_SHORTS.packages.first()
    private val tiktokPackages = ProtectedApp.TIKTOK.packages.toSet()
    private val instagramPackages = ProtectedApp.INSTAGRAM.packages.toSet()
    private val allProtectedPackages = ProtectedApp.entries.flatMap { it.packages }.toSet()

    private val packageToAppName = mapOf(
        "com.zhiliaoapp.musically" to "TikTok",
        "com.ss.android.ugc.trill"  to "TikTok",
        "com.ss.android.ugc.aweme"  to "TikTok",
        "com.instagram.android"     to "Instagram",
        "com.google.android.youtube" to "YouTube"
    )

    private var shortsActive = false
    private var tiktokActive = false
    private var instagramActive = false

    private val tiktokTimeoutRunnable = Runnable {
        if (tiktokActive) {
            tiktokActive = false
            DopamineVpnService.isTikTokForeground.set(false)
            DopamineVpnService.instance?.rebuildTunnel()
            Log.d(TAG, "TikTok: BACKGROUND ◼")
        }
    }

    private val instagramTimeoutRunnable = Runnable {
        if (instagramActive) {
            instagramActive = false
            DopamineVpnService.isInstagramForeground.set(false)
            DopamineVpnService.instance?.rebuildTunnel()
            Log.d(TAG, "Instagram: BACKGROUND ◼")
        }
    }

    override fun onServiceConnected() {
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_SELECTED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_SCROLLED
            packageNames = allProtectedPackages.toTypedArray()
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 500
        }
        Log.d(TAG, "Accessibility service conectat — monitorizez: $allProtectedPackages")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return

        if (pkg !in allProtectedPackages) return

        // 1. Detectare Shorts (comportament existent)
        if (pkg == youtubePkg && SettingsManager.isEnabled(this, ProtectedApp.YOUTUBE_SHORTS)) {
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                event.eventType == AccessibilityEvent.TYPE_VIEW_SELECTED ||
                event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED
            ) {
                val root = rootInActiveWindow ?: return
                val isShorts = detectShorts(root)
                root.recycle()
                if (isShorts != shortsActive) {
                    shortsActive = isShorts
                    DopamineVpnService.isYoutubeShortsActive.set(isShorts)
                    DopamineVpnService.instance?.rebuildTunnel()
                    Log.d(TAG, "Shorts: ${if (isShorts) "ACTIV ▶" else "INACTIV ◼"}")
                }
            }
        }

        // 2. Heartbeat foreground TikTok / Instagram → rebuild tunel când intră/iese din app
        if (pkg in tiktokPackages && SettingsManager.isEnabled(this, ProtectedApp.TIKTOK)) {
            handler.removeCallbacks(tiktokTimeoutRunnable)
            handler.postDelayed(tiktokTimeoutRunnable, FOREGROUND_TIMEOUT_MS)
            if (!tiktokActive) {
                tiktokActive = true
                DopamineVpnService.isTikTokForeground.set(true)
                DopamineVpnService.instance?.rebuildTunnel()
                Log.d(TAG, "TikTok: FOREGROUND ▶")
            }
        }
        if (pkg in instagramPackages && SettingsManager.isEnabled(this, ProtectedApp.INSTAGRAM)) {
            handler.removeCallbacks(instagramTimeoutRunnable)
            handler.postDelayed(instagramTimeoutRunnable, FOREGROUND_TIMEOUT_MS)
            if (!instagramActive) {
                instagramActive = true
                DopamineVpnService.isInstagramForeground.set(true)
                DopamineVpnService.instance?.rebuildTunnel()
                Log.d(TAG, "Instagram: FOREGROUND ▶")
            }
        }

        // 3. Scanare conținut pentru profilul psihologic
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            val root = rootInActiveWindow ?: return
            val text = extractText(root)
            root.recycle()
            if (text.isNotBlank()) {
                val appName = packageToAppName[pkg] ?: pkg
                ContentAnalyzer.analyze(this, text, appName)

                // Training mode: swipe up automat dupa scanare
                if (trainingModeActive.get() && pkg in listOf(
                        "com.zhiliaoapp.musically",
                        "com.ss.android.ugc.trill",
                        "com.ss.android.ugc.aweme"
                    ) && !swipePending
                ) {
                    swipePending = true
                    handler.postDelayed({
                        swipePending = false
                        if (trainingModeActive.get()) swipeUpTikTok()
                    }, SWIPE_DELAY_MS)
                }
            }
        }
    }

    override fun onInterrupt() {
        if (shortsActive) {
            shortsActive = false
            DopamineVpnService.isYoutubeShortsActive.set(false)
            DopamineVpnService.instance?.rebuildTunnel()
        }
        tiktokActive = false
        instagramActive = false
        DopamineVpnService.isTikTokForeground.set(false)
        DopamineVpnService.isInstagramForeground.set(false)
        handler.removeCallbacks(tiktokTimeoutRunnable)
        handler.removeCallbacks(instagramTimeoutRunnable)
    }

    // ─── Detectare Shorts ─────────────────────────────────────────────────────

    private fun detectShorts(root: AccessibilityNodeInfo): Boolean {
        val nodes = root.findAccessibilityNodeInfosByText("Shorts")
        val found = nodes?.any { node ->
            val selected = node.isSelected || node.isChecked
            node.recycle()
            selected
        } ?: false
        return found
    }

    // ─── Extragere text din arborele de accesibilitate ────────────────────────

    private fun extractText(root: AccessibilityNodeInfo): String {
        val parts = mutableListOf<String>()
        collectText(root, parts, depth = 0)
        return parts.joinToString(" ").trim()
    }

    private fun collectText(node: AccessibilityNodeInfo, parts: MutableList<String>, depth: Int) {
        if (depth > MAX_DEPTH) return

        node.text?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        node.contentDescription?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let { parts.add(it) }

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                collectText(child, parts, depth + 1)
                child.recycle()
            }
        }
    }

    // ─── Training mode: swipe up in TikTok ───────────────────────────────────

    private fun swipeUpTikTok() {
        val display = resources.displayMetrics
        val w = display.widthPixels.toFloat()
        val h = display.heightPixels.toFloat()

        val path = Path().apply {
            moveTo(w / 2, h * 0.75f)  // porneste din 3/4 jos
            lineTo(w / 2, h * 0.25f)  // trage pana la 1/4 sus
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
            .build()

        dispatchGesture(gesture, null, null)
        Log.d(TAG, "Training mode: swipe up executat")
    }
}
