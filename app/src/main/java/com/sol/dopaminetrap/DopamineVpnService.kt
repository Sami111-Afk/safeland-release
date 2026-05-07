package com.sol.dopaminetrap

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import com.sol.dopaminetrap.FirebaseRepository
import com.sol.dopaminetrap.OnboardingManager
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.*
import java.net.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * DopamineVpnService — Mini-TCP transparent proxy cu Burst-and-Pause throttling.
 *
 * TikTok / Instagram / Douyin: mereu în VPN, throttle permanent.
 *
 * YouTube: intră în VPN DOAR când DopamineAccessibilityService confirmă că
 * secțiunea Shorts e activă. În rest, traficul YouTube merge direct la internet,
 * complet neafectat. Tranziția durează ~500ms (rebuild tunel).
 *
 * Mecanismul de rebuild: DopamineAccessibilityService setează isYoutubeShortsActive
 * și apelează instance?.rebuildTunnel(). rebuildTunnel() închide ParcelFileDescriptor-ul
 * curent — read() din run() aruncă IOException → run() reia bucla și construiește un
 * tunel nou cu lista de pachete actualizată.
 */
class DopamineVpnService : VpnService(), Runnable {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val isRunning = AtomicBoolean(false)
    private var vpnThread: Thread? = null
    private lateinit var executor: ExecutorService
    private val connections = ConcurrentHashMap<Int, TcpConn>()

    companion object {
        private const val TAG = "DopamineVpn"
        private const val CHANNEL_ID = "SafelandSilent"
        private const val NOTIF_ID = 1
        private const val ACTION_STOP = "com.sol.dopaminetrap.STOP"
        private const val MTU = 1500

        /** Configurabil din FirebaseRepository — default 64 KB burst, 3s freeze. */
        val burstBytes = java.util.concurrent.atomic.AtomicLong(64 * 1024L)
        val pauseMs    = java.util.concurrent.atomic.AtomicLong(3_000L)

        /** Setat de DopamineAccessibilityService când Shorts e vizibil. */
        val isYoutubeShortsActive = AtomicBoolean(false)

        /** Setat de DopamineAccessibilityService când TikTok / Instagram sunt în foreground. */
        val isTikTokForeground   = AtomicBoolean(false)
        val isInstagramForeground = AtomicBoolean(false)

        /** Referință la serviciul curent — folosit pentru rebuild din alt thread. */
        @Volatile var instance: DopamineVpnService? = null

        /** Sentinel injectat în outQueue la closeConn() ca să trezească take() din upload thread. */
        val POISON_PILL = ByteArray(0)
    }

    inner class TcpConn(
        val srcIp: ByteArray,
        val srcPort: Int,
        val dstIp: ByteArray,
        val dstPort: Int,
        clientIsn: Long
    ) {
        @Volatile var serverSeq: Long = (Math.random() * 0xFFFFFFFFL).toLong() and 0xFFFFFFFFL
        @Volatile var clientSeq: Long = clientIsn + 1
        val outQueue = LinkedBlockingQueue<ByteArray>(512)
        @Volatile var socket: Socket? = null
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        instance = this
        executor = if (DeviceTier.isStandard(this))
            Executors.newFixedThreadPool(12)
        else
            Executors.newCachedThreadPool()
        val familyId = OnboardingManager.getFamilyId(this)
        val childId  = OnboardingManager.getChildId(this)
        if (familyId != null && childId != null) {
            FirebaseRepository.startChildListener(this, familyId, childId)
            SessionTracker.onThresholdCrossed = { app, threshold ->
                serviceScope.launch {
                    val msg = if (threshold == 100)
                        "${app.displayName} — limita zilnică de timp a fost atinsă!"
                    else
                        "${app.displayName} — 80% din limita zilnică utilizată."
                    val level = if (threshold == 100) "HIGH" else "MEDIUM"
                    runCatching { FirebaseRepository.pushAlert(familyId, childId, "time_limit", msg, app.displayName, level) }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            doCleanup()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            stopSelf()
            return START_NOT_STICKY
        }
        if (!isRunning.getAndSet(true)) {
            showNotification()
            vpnThread = Thread(this, "DopamineVpnThread").apply { start() }
        }
        return START_STICKY
    }

    /**
     * Bucla principală. La fiecare iterație construiește un tunel nou cu
     * lista de pachete corectă (cu sau fără YouTube).
     * Când rebuildTunnel() închide interfața curentă, read() aruncă IOException
     * și bucla reia de la capăt.
     */
    override fun run() {
        while (isRunning.get()) {
            try {
                val tun = buildTunnel()
                if (tun == null) {
                    // Nicio aplicatie selectata — asteapta pana user-ul activeza un toggle
                    if (isRunning.get()) Thread.sleep(500)
                    continue
                }
                vpnInterface = tun
                val input  = FileInputStream(tun.fileDescriptor)
                val output = FileOutputStream(tun.fileDescriptor)
                val buf    = ByteArray(MTU)
                try {
                    while (isRunning.get()) {
                        val len = input.read(buf)
                        if (len > 0) processPacket(buf.copyOf(len), len, output)
                    }
                } catch (_: IOException) {
                    // Tunel închis de rebuildTunnel() sau eroare → iterăm din nou
                    if (isRunning.get()) Log.d(TAG, "Rebuild tunel (Shorts=${isYoutubeShortsActive.get()})")
                } finally {
                    try { tun.close() } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                Log.e(TAG, "VPN error: ${e.message}")
                if (isRunning.get()) Thread.sleep(500)
            }
        }
        doCleanup()
    }

    override fun onDestroy() {
        instance = null
        FirebaseRepository.stopListener()
        SessionTracker.onThresholdCrossed = null
        val familyId = OnboardingManager.getFamilyId(this)
        val childId  = OnboardingManager.getChildId(this)
        if (familyId != null && childId != null) {
            serviceScope.launch {
                runCatching {
                    FirebaseRepository.pushAlert(familyId, childId, "vpn_stopped",
                        "Protecția VPN a fost dezactivată de pe telefonul copilului.",
                        "", "HIGH")
                }
            }
        }
        doCleanup()
        executor.shutdownNow()
        super.onDestroy()
    }

    /**
     * Construiește interfața VPN pe baza toggle-urilor salvate.
     * YouTube e inclus DOAR dacă protecția e activă ȘI Shorts e detectat.
     * Returnează null dacă nicio aplicație nu e selectată — run() va retry.
     */
    private fun buildTunnel(): ParcelFileDescriptor? {
        val packages = ProtectedApp.entries
            .filter { app ->
                val enabled = SettingsManager.isEnabled(this, app)
                when (app) {
                    ProtectedApp.YOUTUBE_SHORTS    -> enabled && isYoutubeShortsActive.get()
                    ProtectedApp.TIKTOK            -> enabled && isTikTokForeground.get()
                    ProtectedApp.INSTAGRAM         -> enabled && isInstagramForeground.get()
                    ProtectedApp.INSTAGRAM_REELS   -> enabled && isInstagramForeground.get()
                    ProtectedApp.YOUTUBE           -> enabled
                    ProtectedApp.FACEBOOK          -> enabled
                }
            }
            .flatMap { it.packages }

        if (packages.isEmpty()) return null

        val builder = Builder()
            .setSession("Safeland")
            .setMtu(MTU)
            .addAddress("10.0.0.1", 24)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("8.8.8.8")

        packages.forEach { pkg ->
            try { builder.addAllowedApplication(pkg) } catch (_: Exception) {}
        }
        return builder.establish()
    }

    /**
     * Apelat de DopamineAccessibilityService când starea Shorts se schimbă.
     * Închide tunelul curent — run() va reconstrui automat cu noua listă.
     */
    fun rebuildTunnel() {
        val old = vpnInterface
        vpnInterface = null
        try { old?.close() } catch (_: Exception) {}
    }

    // ─── Packet processing ────────────────────────────────────────────────────

    private fun processPacket(data: ByteArray, len: Int, out: FileOutputStream) {
        if (len < 20) return
        val protocol = data[9].toInt() and 0xFF
        if (protocol == 17) { sendIcmpUnreachable(data, len, out); return }  // UDP → ICMP unreachable → fallback instant la TCP
        if (protocol != 6) return  // IPv4 TCP only

        val ipHl = (data[0].toInt() and 0x0F) * 4
        if (len < ipHl + 20) return

        val srcPort  = u16(data, ipHl)
        val dstPort  = u16(data, ipHl + 2)
        val seq      = u32(data, ipHl + 4)
        val flags    = data[ipHl + 13].toInt() and 0xFF
        val tcpHl    = ((data[ipHl + 12].toInt() and 0xF0) shr 4) * 4
        val payStart = ipHl + tcpHl
        val payLen   = len - payStart

        val srcIp = data.sliceArray(12..15)
        val dstIp = data.sliceArray(16..19)

        val isSyn  = flags and 0x02 != 0 && flags and 0x10 == 0
        val isFin  = flags and 0x01 != 0
        val isRst  = flags and 0x04 != 0
        val isData = flags and 0x10 != 0 && payLen > 0

        when {
            isSyn          -> handleSyn(srcIp, srcPort, dstIp, dstPort, seq, out)
            isRst || isFin -> closeConn(srcPort)
            isData -> {
                val conn = connections[srcPort] ?: return
                conn.clientSeq = seq + payLen
                sendTcpPkt(conn, out, flags = 0x10, payload = null)  // ACK
                conn.outQueue.offer(data.copyOfRange(payStart, payStart + payLen))
            }
        }
    }

    // ─── SYN: răspuns imediat + conectare server real în background ───────────

    private fun handleSyn(
        srcIp: ByteArray, srcPort: Int,
        dstIp: ByteArray, dstPort: Int,
        clientIsn: Long, out: FileOutputStream
    ) {
        if (connections.containsKey(srcPort)) return

        val conn = TcpConn(srcIp, srcPort, dstIp, dstPort, clientIsn)
        connections[srcPort] = conn

        sendTcpPkt(conn, out, flags = 0x12, payload = null)  // SYN-ACK imediat
        conn.serverSeq++

        executor.execute {
            try {
                val socket = Socket()
                protect(socket)
                socket.connect(InetSocketAddress(InetAddress.getByAddress(dstIp), dstPort), 10_000)
                conn.socket = socket

                val uploadThread = Thread {
                    try {
                        val serverOut = socket.getOutputStream()
                        val batch = ArrayList<ByteArray>(16)
                        while (true) {
                            val chunk = conn.outQueue.take()           // blochează fără spin (Opt 2)
                            if (chunk === POISON_PILL) break           // closeConn() a injectat sentinela
                            if (socket.isClosed) break
                            batch.clear()
                            batch.add(chunk)
                            conn.outQueue.drainTo(batch, 15)           // adună ce mai e în coadă (Opt 3)
                            var poisoned = false
                            for (b in batch) {
                                if (b === POISON_PILL) { poisoned = true; break }
                                serverOut.write(b)
                            }
                            serverOut.flush()                          // un singur flush per batch
                            if (poisoned) break
                        }
                    } catch (_: Exception) {}
                }
                uploadThread.isDaemon = true
                uploadThread.start()

                readServerWithThrottle(conn, socket, out)
                uploadThread.interrupt()

            } catch (e: Exception) {
                Log.e(TAG, "Conn failed [port $srcPort]: ${e.message}")
            } finally {
                closeConn(srcPort)
            }
        }
    }

    // ─── Throttle: 64KB burst → 3s freeze ────────────────────────────────────

    private fun readServerWithThrottle(conn: TcpConn, socket: Socket, out: FileOutputStream) {
        val buf = ByteArray(32_768)
        var burst = 0L
        try {
            val serverIn = socket.getInputStream()
            var read: Int
            while (serverIn.read(buf).also { read = it } != -1) {
                burst += read

                // null = limită setată + timp suficient → fără throttle
                val multiplier: Float? = when {
                    isTikTokForeground.get()    -> SessionTracker.getThrottleMultiplier(ProtectedApp.TIKTOK)
                    isInstagramForeground.get() -> SessionTracker.getThrottleMultiplier(ProtectedApp.INSTAGRAM)
                    isYoutubeShortsActive.get() -> SessionTracker.getThrottleMultiplier(ProtectedApp.YOUTUBE_SHORTS)
                    else -> null
                }
                if (multiplier != null) {
                    val currentBurstBytes = (burstBytes.get() * multiplier).toLong().coerceAtLeast(4_096L)
                    val currentPauseMs    = (pauseMs.get() / multiplier.coerceAtLeast(0.02f)).toLong().coerceAtMost(60_000L)
                    if (burst >= currentBurstBytes) {
                        Log.d(TAG, "⏸ Throttle ${currentPauseMs}ms [port ${conn.srcPort}, mult=${"%.2f".format(multiplier)}]")
                        Thread.sleep(currentPauseMs)
                        burst = 0
                    }
                } else {
                    burst = 0
                }
                val payload = buf.copyOf(read)
                sendTcpPkt(conn, out, flags = 0x18, payload = payload)  // PSH+ACK
                conn.serverSeq += read
            }
        } catch (_: Exception) {
        } finally {
            closeConn(conn.srcPort)
        }
    }

    // ─── ICMP Port Unreachable → forțează fallback imediat la TCP ────────────

    private fun sendIcmpUnreachable(orig: ByteArray, len: Int, out: FileOutputStream) {
        // Structură: IP header (20) + ICMP header (8) + IP header original (20) + primii 8 octeți UDP
        val origCopy = minOf(28, len)
        val totalLen = 20 + 8 + origCopy
        val pkt = ByteArray(totalLen)

        // IP header — src/dst inversate față de pachetul original
        pkt[0] = 0x45.toByte()
        pkt[2] = (totalLen shr 8).toByte()
        pkt[3] = (totalLen and 0xFF).toByte()
        pkt[6] = 0x40.toByte()  // Don't Fragment
        pkt[8] = 64             // TTL
        pkt[9] = 1              // protocol ICMP
        orig.copyInto(pkt, 12, 16, 20)  // src = dst original
        orig.copyInto(pkt, 16, 12, 16)  // dst = src original
        computeIpChecksum(pkt)

        // ICMP header: Type 3 (Destination Unreachable), Code 3 (Port Unreachable)
        pkt[20] = 3
        pkt[21] = 3
        // bytes 22-23: checksum (calculat mai jos), 24-27: unused = 0

        // Payload ICMP: IP header original + primii 8 octeți UDP
        orig.copyInto(pkt, 28, 0, origCopy)

        computeIcmpChecksum(pkt, 20, totalLen - 20)
        synchronized(out) { out.write(pkt); out.flush() }
    }

    private fun computeIcmpChecksum(pkt: ByteArray, offset: Int, length: Int) {
        pkt[offset + 2] = 0; pkt[offset + 3] = 0
        var sum = 0
        var i = offset
        while (i < offset + length - 1) {
            sum += (pkt[i].toInt() and 0xFF shl 8) or (pkt[i + 1].toInt() and 0xFF)
            i += 2
        }
        if (i < offset + length) sum += pkt[i].toInt() and 0xFF shl 8
        while (sum shr 16 != 0) sum = (sum and 0xFFFF) + (sum shr 16)
        sum = sum.inv() and 0xFFFF
        pkt[offset + 2] = (sum shr 8).toByte()
        pkt[offset + 3] = (sum and 0xFF).toByte()
    }

    // ─── Packet crafting ──────────────────────────────────────────────────────

    private fun sendTcpPkt(conn: TcpConn, out: FileOutputStream, flags: Int, payload: ByteArray?) {
        val payLen   = payload?.size ?: 0
        val totalLen = 40 + payLen
        val pkt      = ByteArray(totalLen)

        pkt[0]  = 0x45.toByte()
        pkt[2]  = (totalLen shr 8).toByte()
        pkt[3]  = (totalLen and 0xFF).toByte()
        pkt[6]  = 0x40.toByte()
        pkt[8]  = 64
        pkt[9]  = 6
        conn.dstIp.copyInto(pkt, 12)
        conn.srcIp.copyInto(pkt, 16)

        pkt[20] = (conn.dstPort shr 8).toByte()
        pkt[21] = (conn.dstPort and 0xFF).toByte()
        pkt[22] = (conn.srcPort shr 8).toByte()
        pkt[23] = (conn.srcPort and 0xFF).toByte()
        writeU32(pkt, 24, conn.serverSeq)
        writeU32(pkt, 28, conn.clientSeq)
        pkt[32] = 0x50.toByte()
        pkt[33] = flags.toByte()
        pkt[34] = 0xFF.toByte(); pkt[35] = 0xFF.toByte()

        payload?.copyInto(pkt, 40)
        computeIpChecksum(pkt)
        computeTcpChecksum(pkt, 20 + payLen)

        synchronized(out) { out.write(pkt); out.flush() }
    }

    private fun computeIpChecksum(pkt: ByteArray) {
        pkt[10] = 0; pkt[11] = 0
        var sum = 0
        for (i in 0 until 20 step 2)
            sum += (pkt[i].toInt() and 0xFF shl 8) or (pkt[i + 1].toInt() and 0xFF)
        while (sum shr 16 != 0) sum = (sum and 0xFFFF) + (sum shr 16)
        sum = sum.inv() and 0xFFFF
        pkt[10] = (sum shr 8).toByte(); pkt[11] = (sum and 0xFF).toByte()
    }

    private fun computeTcpChecksum(pkt: ByteArray, tcpLen: Int) {
        pkt[36] = 0; pkt[37] = 0
        var sum = 0
        for (i in 12..19 step 2)
            sum += (pkt[i].toInt() and 0xFF shl 8) or (pkt[i + 1].toInt() and 0xFF)
        sum += 6 + tcpLen
        var i = 20
        while (i < pkt.size - 1) {
            sum += (pkt[i].toInt() and 0xFF shl 8) or (pkt[i + 1].toInt() and 0xFF)
            i += 2
        }
        if (i < pkt.size) sum += pkt[i].toInt() and 0xFF shl 8
        while (sum shr 16 != 0) sum = (sum and 0xFFFF) + (sum shr 16)
        sum = sum.inv() and 0xFFFF
        pkt[36] = (sum shr 8).toByte(); pkt[37] = (sum and 0xFF).toByte()
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun closeConn(srcPort: Int) {
        val conn = connections.remove(srcPort) ?: return
        conn.outQueue.offer(POISON_PILL)       // trezește take() din upload thread
        conn.socket?.let { try { it.close() } catch (_: Exception) {} }
    }

    private fun doCleanup() {
        isRunning.set(false)
        isYoutubeShortsActive.set(false)
        isTikTokForeground.set(false)
        isInstagramForeground.set(false)
        connections.keys.toList().forEach { closeConn(it) }
        try { vpnInterface?.close() } catch (_: Exception) {}
        vpnInterface = null
    }

    private fun u16(data: ByteArray, o: Int) =
        (data[o].toInt() and 0xFF shl 8) or (data[o + 1].toInt() and 0xFF)

    private fun u32(data: ByteArray, o: Int): Long =
        ((data[o].toLong() and 0xFF) shl 24) or
        ((data[o + 1].toLong() and 0xFF) shl 16) or
        ((data[o + 2].toLong() and 0xFF) shl 8) or
        (data[o + 3].toLong() and 0xFF)

    private fun writeU32(pkt: ByteArray, o: Int, v: Long) {
        pkt[o]     = (v shr 24 and 0xFF).toByte()
        pkt[o + 1] = (v shr 16 and 0xFF).toByte()
        pkt[o + 2] = (v shr 8  and 0xFF).toByte()
        pkt[o + 3] = (v        and 0xFF).toByte()
    }

    // ─── Notification ─────────────────────────────────────────────────────────

    private fun showNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(CHANNEL_ID, "Safeland", NotificationManager.IMPORTANCE_MIN)
                .apply {
                    setShowBadge(false)
                    enableVibration(false)
                    enableLights(false)
                }
            getSystemService(NotificationManager::class.java).createNotificationChannel(chan)
        }
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Safeland")
            .setContentText("Protecție activă")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setShowWhen(false)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }
}
