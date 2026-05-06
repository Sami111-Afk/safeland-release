package com.sol.dopaminetrap.ui

import android.content.Intent
import android.net.Uri
import android.os.BatteryManager
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.core.content.FileProvider
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.sol.dopaminetrap.data.AppDatabase
import com.sol.dopaminetrap.ml.ModelManager
import com.sol.dopaminetrap.worker.DailyWellbeingWorker
import com.sol.dopaminetrap.worker.WeeklyReportWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sol.dopaminetrap.*
import com.sol.dopaminetrap.BuildConfig
import com.sol.dopaminetrap.FirebaseRepository
import com.sol.dopaminetrap.ui.theme.BrandIndigo
import com.sol.dopaminetrap.ui.theme.StatusGreen
import com.sol.dopaminetrap.ui.theme.StatusRed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign

@Composable
fun ChildScreen(
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

    // ── Observe settings for app lock / disable ───────────────────────────────
    val settings by FirebaseRepository.settingsFlow.collectAsStateWithLifecycle()
    var pinUnlocked by remember { mutableStateOf(false) }

    if (!settings.appEnabled) {
        AppDisabledScreen()
        return
    }

    if (settings.lockEnabled && !pinUnlocked) {
        PinLockScreen(
            correctPin = settings.lockCode,
            onUnlocked = { pinUnlocked = true }
        )
        return
    }

    val vpnActive = DopamineVpnService.instance != null

    // Refresh VPN state on resume
    var vpnActiveState by remember { mutableStateOf(vpnActive) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vpnActiveState = DopamineVpnService.instance != null
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // PIN dialog state (pentru acces setări ascunse)
    var showPinDialog     by remember { mutableStateOf(false) }
    var showHiddenSection by remember { mutableStateOf(false) }

    if (showPinDialog) {
        PinAccessDialog(
            correctPin = settings.lockCode.ifEmpty { null },
            onSuccess  = { showHiddenSection = true; showPinDialog = false },
            onDismiss  = { showPinDialog = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ShieldHeader(isActive = vpnActiveState)

        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            // Mesaj simplu pentru copil
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(16.dp),
                colors   = CardDefaults.cardColors(
                    containerColor = if (vpnActiveState)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        if (vpnActiveState) "Ești protejat 🛡️" else "Protecția nu este activă",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign  = TextAlign.Center
                    )
                    Text(
                        if (vpnActiveState)
                            "Safeland funcționează în fundal. Nu trebuie să faci nimic."
                        else
                            "Apasă butonul de mai jos pentru a porni protecția.",
                        style     = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!vpnActiveState) {
                Button(
                    onClick  = { onStartVpn(); vpnActiveState = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = BrandIndigo)
                ) {
                    Text("Pornește protecția", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                }
            }

            // Indicator discret dacă lipsesc permisiuni (copilul e îndrumat spre Setări)
            val permsMissing = !isAccessibilityEnabled() || !isNotificationListenerEnabled()
            if (permsMissing) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("⚠️", fontSize = 18.sp)
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Protecția nu este completă",
                                style      = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color      = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                "Cere ajutorul părintelui tău → Setări",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Secțiunea ascunsă — necesită PIN dacă lockEnabled
            if (showHiddenSection) {
                // Permisiuni
                PermissionsCard(
                    a11yEnabled  = isAccessibilityEnabled(),
                    notifEnabled = isNotificationListenerEnabled(),
                    smsEnabled   = hasSmsPermission(),
                    onOpenA11y   = onOpenAccessibilitySettings,
                    onOpenNotif  = onOpenNotificationSettings,
                    onRequestSms = { onRequestSmsPermission() }
                )

                // Oprire VPN
                val ctx = LocalContext.current
                Button(
                    onClick = {
                        ctx.stopService(android.content.Intent(ctx, DopamineVpnService::class.java))
                        vpnActiveState = false
                        showHiddenSection = false
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Oprește protecția", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                }

                PairingCodeCard()
                if (BuildConfig.DEBUG) {
                    DevToolsSection()
                }
                TextButton(onClick = { showHiddenSection = false }) {
                    Text("Închide setările", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                // Link discret în josul ecranului
                Spacer(Modifier.height(32.dp))
                TextButton(onClick = { showPinDialog = true }) {
                    Text("Setări", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PinAccessDialog(
    correctPin: String?,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    var pin   by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cod de acces") },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value         = pin,
                    onValueChange = {
                        if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                            pin = it; error = false
                        }
                    },
                    label           = { Text("PIN 4 cifre") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine      = true,
                    isError         = error
                )
                if (error) Text("Cod incorect.", color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall)
            }
        },
        confirmButton = {
            TextButton(
                enabled = pin.length == 4,
                onClick = {
                    if (correctPin == null || pin == correctPin) onSuccess()
                    else { error = true; pin = "" }
                }
            ) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anulează") }
        }
    )
}

// ── Shield header ─────────────────────────────────────────────────────────────

@Composable
private fun ShieldHeader(isActive: Boolean) {
    val bgColor by animateColorAsState(
        targetValue = if (isActive) Color(0xFF3B4FCC) else Color(0xFF5A5A72),
        animationSpec = tween(600),
        label = "header_color"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = if (isActive) 1.12f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(bgColor, bgColor.copy(alpha = 0.85f))
                )
            )
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Column {
            Text(
                "Safeland",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White.copy(alpha = 0.7f),
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (isActive) "Protecție activă" else "Configurare necesară",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(64.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(if (isActive) "🛡️" else "⚠️", fontSize = 32.sp)
        }
    }
}

// ── Permissions card ──────────────────────────────────────────────────────────

@Composable
private fun PermissionsCard(
    a11yEnabled: Boolean,
    notifEnabled: Boolean,
    smsEnabled: Boolean,
    onOpenA11y: () -> Unit,
    onOpenNotif: () -> Unit,
    onRequestSms: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Stare sistem",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            StatusRow(
                label    = "Detectare conținut",
                enabled  = a11yEnabled,
                onAction = onOpenA11y
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(0.4f))
            StatusRow(
                label    = "Monitorizare mesaje",
                enabled  = notifEnabled,
                onAction = onOpenNotif
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(0.4f))
            StatusRow(
                label    = "Mesaje SMS",
                enabled  = smsEnabled,
                onAction = onRequestSms
            )
        }
    }
}

@Composable
private fun StatusRow(label: String, enabled: Boolean, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (enabled) StatusGreen else StatusRed)
            )
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
        if (!enabled) {
            TextButton(
                onClick = onAction,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text(
                    "Activează",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            Text(
                "Activ",
                style = MaterialTheme.typography.labelSmall,
                color = StatusGreen,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ── Protected apps card ───────────────────────────────────────────────────────

@Composable
private fun ProtectedAppsCard(
    appStates: Map<ProtectedApp, Boolean>,
    onToggle: (ProtectedApp, Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Aplicații protejate",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            ProtectedApp.entries.forEachIndexed { index, app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(app.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            "Throttle automat",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = appStates[app] ?: true,
                        onCheckedChange = { onToggle(app, it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = BrandIndigo
                        )
                    )
                }
                if (index < ProtectedApp.entries.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outline.copy(0.4f)
                    )
                }
            }
        }
    }
}

// ── Pairing code card ─────────────────────────────────────────────────────────

@Composable
private fun PairingCodeCard() {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val familyId  = remember { com.sol.dopaminetrap.OnboardingManager.getFamilyId(context) ?: "" }
    val childId   = remember { com.sol.dopaminetrap.OnboardingManager.getChildId(context) ?: "" }
    val childName = remember { com.sol.dopaminetrap.OnboardingManager.getChildName(context) ?: "Copil" }

    var code      by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error     by remember { mutableStateOf<String?>(null) }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Conectare cu părintele",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (code == null) {
                Text(
                    "Generează un cod de 6 cifre pe care părintele îl introduce în aplicația lui.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = {
                        isLoading = true
                        error = null
                        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            runCatching {
                                val newCode = com.sol.dopaminetrap.FirebaseRepository
                                    .generatePairingCode(familyId, childId, childName)
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    code = newCode
                                }
                            }.onFailure {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    error = "Eroare. Verifică internetul."
                                }
                            }
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                isLoading = false
                            }
                        }
                    },
                    enabled  = !isLoading && familyId.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(10.dp)
                ) {
                    if (isLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Generează cod de conectare", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Text(
                    "Dă acest cod părintelui (expiră în 15 min):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    color    = BrandIndigo.copy(alpha = 0.10f)
                ) {
                    Text(
                        code!!,
                        modifier   = Modifier.padding(vertical = 16.dp).fillMaxWidth(),
                        style      = MaterialTheme.typography.displaySmall.copy(
                            fontFamily    = androidx.compose.ui.text.font.FontFamily.Monospace,
                            letterSpacing = 8.sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color      = BrandIndigo,
                        textAlign  = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                TextButton(
                    onClick  = { code = null },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Generează alt cod", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

// ── Dev tools ─────────────────────────────────────────────────────────────────

@Composable
private fun DevToolsSection() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Unelte test (dev)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Test raport săptămânal
            OutlinedButton(
                onClick = {
                    WorkManager.getInstance(context)
                        .enqueue(OneTimeWorkRequestBuilder<WeeklyReportWorker>().build())
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Test raport săptămânal", style = MaterialTheme.typography.bodySmall)
            }

            // Test well-being zilnic
            OutlinedButton(
                onClick = {
                    WorkManager.getInstance(context)
                        .enqueue(OneTimeWorkRequestBuilder<DailyWellbeingWorker>().build())
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Test well-being zilnic", style = MaterialTheme.typography.bodySmall)
            }

            // Training mode TikTok
            var trainingMode by remember { mutableStateOf(com.sol.dopaminetrap.DopamineAccessibilityService.trainingModeActive.get()) }
            OutlinedButton(
                onClick = {
                    trainingMode = !trainingMode
                    com.sol.dopaminetrap.DopamineAccessibilityService.trainingModeActive.set(trainingMode)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    if (trainingMode) "Training TikTok: ACTIV — oprește" else "Pornește training mode TikTok",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Export date training
            val usingAi = remember { ModelManager.isUsingAiModel(context) }
            OutlinedButton(
                onClick = {
                    scope.launch(Dispatchers.IO) {
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
                        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
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
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    if (usingAi) "Model AI activ — Export CSV" else "Export date training CSV",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Force STANDARD device tier
            var forceStandard by remember { mutableStateOf(com.sol.dopaminetrap.DeviceTier.forceStandard.get()) }
            OutlinedButton(
                onClick = {
                    forceStandard = !forceStandard
                    com.sol.dopaminetrap.DeviceTier.forceStandard.set(forceStandard)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    if (forceStandard) "DeviceTier: STANDARD forțat — resetează" else "Forțează mod STANDARD",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Battery monitor
            BatteryMonitorCard()
        }
    }
}

@Composable
private fun BatteryMonitorCard() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val batteryManager = remember { context.getSystemService(BatteryManager::class.java) }
    val readings = remember { ArrayDeque<Int>(15) }
    var avgMa     by remember { mutableIntStateOf(0) }
    var currentMa by remember { mutableIntStateOf(0) }
    var unsupported by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            val raw = batteryManager?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: Long.MIN_VALUE
            if (raw == Long.MIN_VALUE) {
                unsupported = true
            } else {
                unsupported = false
                val mA = (Math.abs(raw) / 1000).toInt()
                currentMa = mA
                if (readings.size >= 15) readings.removeFirst()
                readings.addLast(mA)
                avgMa = readings.average().toInt()
            }
            delay(2_000)
        }
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.3f))
    Text(
        "Monitor baterie",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
        if (unsupported) "BATTERY_PROPERTY_CURRENT_NOW nesupport (Xiaomi?)"
        else "Curent: ${currentMa} mA  |  Medie 30s: ${avgMa} mA",
        style = MaterialTheme.typography.bodySmall,
        color = if (unsupported) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// ── Overlay app dezactivată ───────────────────────────────────────────────────

@Composable
fun AppDisabledScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.errorContainer),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(40.dp)
        ) {
            Text("🔒", fontSize = 64.sp)
            Text(
                "Aplicația a fost dezactivată",
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center,
                color      = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                "Părintele tău a dezactivat temporar accesul. Vorbește cu el dacă crezi că e o greșeală.",
                style     = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color     = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
            )
        }
    }
}

// ── Ecran PIN lock ────────────────────────────────────────────────────────────

@Composable
fun PinLockScreen(correctPin: String, onUnlocked: () -> Unit) {
    var pin        by remember { mutableStateOf("") }
    var error      by remember { mutableStateOf(false) }
    var showInput  by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(androidx.compose.ui.graphics.Color(0xFF3B4FCC),
                           androidx.compose.ui.graphics.Color(0xFF1A237E))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(40.dp)
        ) {
            Text("🔐", fontSize = 72.sp)
            Text(
                "Safeland",
                style      = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color      = androidx.compose.ui.graphics.Color.White
            )
            Text(
                "Aplicația este blocată.",
                style     = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color     = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f)
            )
            Text(
                "Cere codul de deblocare de la părintele tău.",
                style     = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color     = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(8.dp))

            if (!showInput) {
                OutlinedButton(
                    onClick = { showInput = true },
                    colors  = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = androidx.compose.ui.graphics.Color.White
                    ),
                    border  = androidx.compose.foundation.BorderStroke(
                        1.dp, androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f)
                    )
                ) {
                    Text("Am codul")
                }
            } else {
                OutlinedTextField(
                    value         = pin,
                    onValueChange = {
                        if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                            pin   = it
                            error = false
                        }
                    },
                    label           = { Text("Cod de deblocare", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine      = true,
                    isError         = error,
                    colors          = OutlinedTextFieldDefaults.colors(
                        focusedTextColor      = androidx.compose.ui.graphics.Color.White,
                        unfocusedTextColor    = androidx.compose.ui.graphics.Color.White,
                        focusedBorderColor    = androidx.compose.ui.graphics.Color.White,
                        unfocusedBorderColor  = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f),
                        errorBorderColor      = androidx.compose.ui.graphics.Color(0xFFFF6B6B)
                    ),
                    supportingText  = if (error) {{ Text("Cod incorect.", color = androidx.compose.ui.graphics.Color(0xFFFF6B6B)) }} else null,
                    modifier        = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (pin == correctPin) onUnlocked()
                        else { error = true; pin = "" }
                    },
                    enabled  = pin.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors   = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color.White,
                        contentColor   = androidx.compose.ui.graphics.Color(0xFF3B4FCC)
                    )
                ) {
                    Text("Deblochează", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
