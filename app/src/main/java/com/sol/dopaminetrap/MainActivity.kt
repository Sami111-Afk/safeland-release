package com.sol.dopaminetrap

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.delay
import com.sol.dopaminetrap.data.AppDatabase
import com.sol.dopaminetrap.ml.ModelManager
import com.sol.dopaminetrap.ui.OnboardingScreen
import com.sol.dopaminetrap.ui.ParentScreen
import com.sol.dopaminetrap.ui.theme.DopamineTrapTheme
import com.sol.dopaminetrap.DopamineFcmService
import com.sol.dopaminetrap.worker.WeeklyReportWorker
import com.sol.dopaminetrap.FirebaseRepository
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) prepareAndStartVpn()
        else Toast.makeText(this, "Permisiunea pentru notificări este necesară.", Toast.LENGTH_LONG).show()
    }

    private val requestSmsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* rezultatul e verificat live in UI */ }

    private val vpnPrepareLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) startVpnService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WeeklyReportWorker.schedule(this)
        setContent {
            DopamineTrapTheme {
                var onboardingDone by remember {
                    mutableStateOf(OnboardingManager.isOnboardingDone(this@MainActivity))
                }
                var deviceMode by remember {
                    mutableStateOf(OnboardingManager.getMode(this@MainActivity))
                }

                if (!onboardingDone) {
                    OnboardingScreen(onOnboardingComplete = {
                        onboardingDone = true
                        deviceMode = OnboardingManager.getMode(this@MainActivity)
                    })
                } else when (deviceMode) {
                    OnboardingManager.DeviceMode.CHILD -> {
                        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                            MainScreen(
                                modifier = Modifier.padding(innerPadding),
                                onStartVpn = { checkPermissionsAndStart() },
                                isAccessibilityEnabled = { isAccessibilityServiceEnabled() },
                                onOpenAccessibilitySettings = { openAccessibilitySettings() },
                                isNotificationListenerEnabled = { isNotificationListenerEnabled() },
                                onOpenNotificationSettings = {
                                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                },
                                hasSmsPermission = { hasSmsPermission() },
                                onRequestSmsPermission = { requestSmsPermissions() }
                            )
                        }
                    }
                    OnboardingManager.DeviceMode.PARENT -> {
                        LaunchedEffect(Unit) {
                            val familyId = OnboardingManager.getFamilyId(this@MainActivity)
                            if (familyId != null) {
                                createAlertNotificationChannel()
                                DopamineFcmService.saveParentToken(familyId)
                            }
                        }
                        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                            Box(Modifier.padding(innerPadding)) {
                                ParentScreen()
                            }
                        }
                    }
                    null -> {
                        OnboardingManager.reset(this@MainActivity)
                        onboardingDone = false
                    }
                }
            }
        }
    }

    private fun createAlertNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                DopamineFcmService.CHANNEL_ID,
                "Alerte parinti",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerte critice despre continutul consumat de copil"
                enableVibration(true)
            }
            getSystemService(android.app.NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    fun isNotificationListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        return flat.contains(packageName)
    }

    fun hasSmsPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED

    fun requestSmsPermissions() {
        requestSmsPermissionLauncher.launch(
            arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
        )
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val component = "${packageName}/${DopamineAccessibilityService::class.java.canonicalName}"
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.contains(component, ignoreCase = true)
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun checkPermissionsAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        prepareAndStartVpn()
    }

    private fun prepareAndStartVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) vpnPrepareLauncher.launch(intent)
        else startVpnService()
    }

    private fun startVpnService() {
        val intent = Intent(this, DopamineVpnService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
        else startService(intent)
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    onStartVpn: () -> Unit,
    isAccessibilityEnabled: () -> Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    isNotificationListenerEnabled: () -> Boolean,
    onOpenNotificationSettings: () -> Unit,
    hasSmsPermission: () -> Boolean,
    onRequestSmsPermission: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var a11yEnabled by remember { mutableStateOf(isAccessibilityEnabled()) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) a11yEnabled = isAccessibilityEnabled()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var appStates by remember {
        mutableStateOf(ProtectedApp.entries.associateWith { SettingsManager.isEnabled(context, it) })
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 32.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Dopamine Trap", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(32.dp))

        // Card accesibilitate
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (a11yEnabled)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.errorContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    if (a11yEnabled) "Detectie Shorts: ACTIVA" else "Detectie Shorts: INACTIVA",
                    style = MaterialTheme.typography.titleSmall
                )
                if (!a11yEnabled) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Activează serviciul de accesibilitate pentru ca YouTube Shorts să fie throttled.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onOpenAccessibilitySettings) {
                        Text("Deschide Setari Accesibilitate")
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Card acces notificari (WhatsApp, Telegram etc.)
        var notifListenerEnabled by remember { mutableStateOf(isNotificationListenerEnabled()) }
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) notifListenerEnabled = isNotificationListenerEnabled()
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (notifListenerEnabled)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.errorContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    if (notifListenerEnabled) "Monitorizare mesaje: ACTIVA" else "Monitorizare mesaje: INACTIVA",
                    style = MaterialTheme.typography.titleSmall
                )
                if (!notifListenerEnabled) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Activează accesul la notificări pentru monitorizarea WhatsApp, Telegram și alte app-uri de mesagerie.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onOpenNotificationSettings) {
                        Text("Deschide Setari Notificari")
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Card permisiune SMS
        var smsGranted by remember { mutableStateOf(hasSmsPermission()) }
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) smsGranted = hasSmsPermission()
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (smsGranted)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.errorContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    if (smsGranted) "Monitorizare SMS: ACTIVA" else "Monitorizare SMS: INACTIVA",
                    style = MaterialTheme.typography.titleSmall
                )
                if (!smsGranted) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Acordă permisiunea pentru SMS ca să fie monitorizate mesajele text.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { onRequestSmsPermission(); smsGranted = hasSmsPermission() }) {
                        Text("Acorda permisiune SMS")
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Card toggle-uri aplicatii
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    "Aplicatii Protejate",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                ProtectedApp.entries.forEachIndexed { index, app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(app.displayName, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Throttle la detectie",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = appStates[app] ?: true,
                            onCheckedChange = { enabled ->
                                SettingsManager.setEnabled(context, app, enabled)
                                appStates = appStates + (app to enabled)
                                if (app == ProtectedApp.YOUTUBE_SHORTS && !enabled) {
                                    DopamineVpnService.isYoutubeShortsActive.set(false)
                                }
                                DopamineVpnService.instance?.rebuildTunnel()
                            }
                        )
                    }
                    if (index < ProtectedApp.entries.size - 1) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onStartVpn,
            modifier = Modifier.height(56.dp).fillMaxWidth(0.8f)
        ) {
            Text("Activeaza VPN")
        }

        // TODO: scoate înainte de release — test raport saptamanal FCM
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = {
                WorkManager.getInstance(context)
                    .enqueue(OneTimeWorkRequestBuilder<WeeklyReportWorker>().build())
            },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Test notificare raport", style = MaterialTheme.typography.bodySmall)
        }


        // TODO: scoate înainte de release — mod training auto-scroll TikTok
        Spacer(Modifier.height(8.dp))
        var trainingMode by remember { mutableStateOf(DopamineAccessibilityService.trainingModeActive.get()) }
        OutlinedButton(
            onClick = {
                trainingMode = !trainingMode
                DopamineAccessibilityService.trainingModeActive.set(trainingMode)
            },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text(
                if (trainingMode) "Training mode: ACTIV (apasa sa opresti)" else "Porneste training mode TikTok",
                style = MaterialTheme.typography.bodySmall
            )
        }

        // TODO: scoate înainte de release — export date pentru training AI
        Spacer(Modifier.height(8.dp))
        val usingAi = remember { ModelManager.isUsingAiModel(context) }
        OutlinedButton(
            onClick = {
                CoroutineScope(Dispatchers.IO).launch {
                    val events = AppDatabase.get(context).contentEventDao().getAll()
                    val csv = buildString {
                        appendLine("rawText,categories,concernLevel")
                        events.forEach { e ->
                            val safe = e.rawText.replace("\"", "\"\"")
                            appendLine("\"$safe\",\"${e.categories}\",${e.concernLevel}")
                        }
                    }
                    val file = File(context.cacheDir, "training_data.csv")
                    file.writeText(csv)
                    val uri: Uri = FileProvider.getUriForFile(
                        context, "${context.packageName}.provider", file
                    )
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    withContext(Dispatchers.Main) {
                        context.startActivity(Intent.createChooser(intent, "Export training data"))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text(
                if (usingAi) "Mod: AI Model activ" else "Export date training (${context.getString(android.R.string.ok)})",
                style = MaterialTheme.typography.bodySmall
            )
        }

        // TODO: scoate înainte de release — forțează DeviceTier STANDARD pentru test
        Spacer(Modifier.height(8.dp))
        var forceStandard by remember { mutableStateOf(DeviceTier.forceStandard.get()) }
        OutlinedButton(
            onClick = {
                forceStandard = !forceStandard
                DeviceTier.forceStandard.set(forceStandard)
            },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text(
                if (forceStandard) "DeviceTier: STANDARD forțat (apasă să resetezi)" else "Forțează mod STANDARD",
                style = MaterialTheme.typography.bodySmall
            )
        }

        // TODO: scoate înainte de release — monitor baterie pentru optimizare VPN
        Spacer(Modifier.height(16.dp))
        BatteryMonitorCard()
    }
}

@Composable
fun BatteryMonitorCard() {
    val context = LocalContext.current
    val batteryManager = remember {
        context.getSystemService(BatteryManager::class.java)
    }

    // Ultimele 15 citiri (una la 2s = fereastra de 30s)
    val readings = remember { ArrayDeque<Int>(15) }
    var avgMa by remember { mutableIntStateOf(0) }
    var currentMa by remember { mutableIntStateOf(0) }
    var unsupported by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            val rawLong = batteryManager
                ?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                ?: Long.MIN_VALUE
            // Long.MIN_VALUE = proprietate nesupportata de device
            if (rawLong == Long.MIN_VALUE) {
                unsupported = true
            } else {
                unsupported = false
                // rawLong e in µA; unele device-uri returneaza negativ la discharge
                val mA = (Math.abs(rawLong) / 1000).toInt()
                currentMa = mA
                if (readings.size >= 15) readings.removeFirst()
                readings.addLast(mA)
                avgMa = if (readings.isNotEmpty()) readings.average().toInt() else 0
            }
            delay(2_000)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "Monitor Baterie (debug)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            if (unsupported) {
                Text(
                    "BATTERY_PROPERTY_CURRENT_NOW returneaza 0 pe acest device (Xiaomi?)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text(
                    "Curent: ${currentMa} mA  |  Medie 30s: ${avgMa} mA",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
