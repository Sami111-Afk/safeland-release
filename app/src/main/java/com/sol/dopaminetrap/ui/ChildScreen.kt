package com.sol.dopaminetrap.ui

import android.content.Intent
import android.net.Uri
import android.os.BatteryManager
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import com.sol.dopaminetrap.ui.theme.*
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

    var vpnActiveState by remember { mutableStateOf(DopamineVpnService.instance != null) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vpnActiveState = DopamineVpnService.instance != null
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showPinDialog     by remember { mutableStateOf(false) }
    var showHiddenSection by remember { mutableStateOf(false) }

    if (showPinDialog) {
        PinAccessDialog(
            correctPin = settings.lockCode.ifEmpty { null },
            onSuccess  = { showHiddenSection = true; showPinDialog = false },
            onDismiss  = { showPinDialog = false }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SfCream, SfBg2)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            ShieldHeader(isActive = vpnActiveState)

            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(4.dp))

                // Status card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = CardDefaults.cardColors(
                        containerColor = if (vpnActiveState) SfWarnBg else Color.White
                    ),
                    border   = BorderStroke(1.dp, SfBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            if (vpnActiveState) "Ești protejat 🛡️" else "Protecția nu este activă",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color      = SfDark,
                            textAlign  = TextAlign.Center
                        )
                        Text(
                            if (vpnActiveState)
                                "Safeland funcționează în fundal. Nu trebuie să faci nimic."
                            else
                                "Apasă butonul de mai jos pentru a porni protecția.",
                            style     = MaterialTheme.typography.bodySmall,
                            color     = SfGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                if (!vpnActiveState) {
                    Button(
                        onClick  = { onStartVpn(); vpnActiveState = true },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape    = RoundedCornerShape(28.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = SfDark,
                            contentColor   = SfCream
                        )
                    ) {
                        Text(
                            "Pornește protecția",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Warning: incomplete permissions
                val permsMissing = !isAccessibilityEnabled() || !isNotificationListenerEnabled()
                if (permsMissing) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = CardDefaults.cardColors(containerColor = SfWarnBg),
                        border   = BorderStroke(1.dp, SfBorder)
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
                                    color      = SfDark
                                )
                                Text(
                                    "Cere ajutorul părintelui tău → Setări",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SfGray
                                )
                            }
                        }
                    }
                }

                // Hidden section (needs PIN)
                if (showHiddenSection) {
                    PermissionsCard(
                        a11yEnabled  = isAccessibilityEnabled(),
                        notifEnabled = isNotificationListenerEnabled(),
                        smsEnabled   = hasSmsPermission(),
                        onOpenA11y   = onOpenAccessibilitySettings,
                        onOpenNotif  = onOpenNotificationSettings,
                        onRequestSms = { onRequestSmsPermission() }
                    )

                    val ctx = LocalContext.current
                    Button(
                        onClick = {
                            ctx.stopService(android.content.Intent(ctx, DopamineVpnService::class.java))
                            vpnActiveState = false
                            showHiddenSection = false
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(28.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor   = Color.White
                        )
                    ) {
                        Text("Oprește protecția", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    PairingCodeCard()
                    if (BuildConfig.DEBUG) {
                        DevToolsSection()
                    }
                    TextButton(onClick = { showHiddenSection = false }) {
                        Text("Închide setările", style = MaterialTheme.typography.labelSmall, color = SfLGray)
                    }
                } else {
                    Spacer(Modifier.height(24.dp))
                    TextButton(onClick = { showPinDialog = true }) {
                        Text("Setări", style = MaterialTheme.typography.labelSmall, color = SfLGray)
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
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
        title = { Text("Cod de acces", fontWeight = FontWeight.Bold, color = SfDark) },
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
                    isError         = error,
                    colors          = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = SfDark,
                        unfocusedBorderColor = SfBorder,
                        focusedLabelColor    = SfDark
                    )
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
            ) { Text("OK", color = SfDark, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anulează", color = SfGray) }
        }
    )
}

// ── Shield header ─────────────────────────────────────────────────────────────

@Composable
private fun ShieldHeader(isActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = if (isActive) 1.12f else 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SfMid)
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        Column {
            Text(
                "Safeland",
                style        = MaterialTheme.typography.titleSmall,
                color        = SfCream.copy(alpha = 0.65f),
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (isActive) "Protecție activă" else "Configurare necesară",
                style      = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color      = SfCream
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(60.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(SfCream.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(if (isActive) "🛡️" else "⚠️", fontSize = 28.sp)
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
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        border    = BorderStroke(1.dp, SfBorder),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Stare sistem",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color      = SfGray
            )
            Spacer(Modifier.height(12.dp))

            StatusRow("Detectare conținut", a11yEnabled,  onOpenA11y)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = SfBorder.copy(0.5f))
            StatusRow("Monitorizare mesaje", notifEnabled, onOpenNotif)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = SfBorder.copy(0.5f))
            StatusRow("Mesaje SMS",          smsEnabled,   onRequestSms)
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
            Text(label, style = MaterialTheme.typography.bodyMedium, color = SfDark)
        }
        if (!enabled) {
            TextButton(
                onClick = onAction,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text("Activează", style = MaterialTheme.typography.labelMedium, color = SfDark,
                    fontWeight = FontWeight.SemiBold)
            }
        } else {
            Text("Activ", style = MaterialTheme.typography.labelSmall, color = StatusGreen,
                fontWeight = FontWeight.SemiBold)
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
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        border    = BorderStroke(1.dp, SfBorder),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Conectare cu părintele",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color      = SfGray
            )

            if (code == null) {
                Text(
                    "Generează un cod de 6 cifre pe care părintele îl introduce în aplicația lui.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SfGray
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
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { code = newCode }
                            }.onFailure {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    error = "Eroare. Verifică internetul."
                                }
                            }
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { isLoading = false }
                        }
                    },
                    enabled  = !isLoading && familyId.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(28.dp),
                    border   = BorderStroke(1.dp, SfBorder),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = SfDark)
                ) {
                    if (isLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = SfDark)
                    else Text("Generează cod de conectare", style = MaterialTheme.typography.bodySmall, color = SfDark)
                }
            } else {
                Text(
                    "Dă acest cod părintelui (expiră în 15 min):",
                    style = MaterialTheme.typography.bodySmall,
                    color = SfGray
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    color    = SfBg2
                ) {
                    Text(
                        code!!,
                        modifier   = Modifier.padding(vertical = 16.dp).fillMaxWidth(),
                        style      = MaterialTheme.typography.displaySmall.copy(
                            fontFamily    = androidx.compose.ui.text.font.FontFamily.Monospace,
                            letterSpacing = 8.sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color      = SfDark,
                        textAlign  = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                TextButton(
                    onClick  = { code = null },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Generează alt cod", style = MaterialTheme.typography.labelSmall, color = SfLGray)
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
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        border    = BorderStroke(1.dp, SfBorder),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Unelte test (dev)", style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold, color = SfGray)

            OutlinedButton(
                onClick  = { WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<WeeklyReportWorker>().build()) },
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(10.dp),
                border   = BorderStroke(1.dp, SfBorder),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = SfDark)
            ) { Text("Test raport săptămânal", style = MaterialTheme.typography.bodySmall) }

            OutlinedButton(
                onClick  = { WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<DailyWellbeingWorker>().build()) },
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(10.dp),
                border   = BorderStroke(1.dp, SfBorder),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = SfDark)
            ) { Text("Test well-being zilnic", style = MaterialTheme.typography.bodySmall) }

            var trainingMode by remember { mutableStateOf(com.sol.dopaminetrap.DopamineAccessibilityService.trainingModeActive.get()) }
            OutlinedButton(
                onClick  = { trainingMode = !trainingMode; com.sol.dopaminetrap.DopamineAccessibilityService.trainingModeActive.set(trainingMode) },
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(10.dp),
                border   = BorderStroke(1.dp, SfBorder),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = SfDark)
            ) {
                Text(if (trainingMode) "Training TikTok: ACTIV — oprește" else "Pornește training mode TikTok",
                    style = MaterialTheme.typography.bodySmall)
            }

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
                        withContext(Dispatchers.Main) { context.startActivity(Intent.createChooser(intent, "Export training data")) }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(10.dp),
                border   = BorderStroke(1.dp, SfBorder),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = SfDark)
            ) {
                Text(if (usingAi) "Model AI activ — Export CSV" else "Export date training CSV",
                    style = MaterialTheme.typography.bodySmall)
            }

            var forceStandard by remember { mutableStateOf(com.sol.dopaminetrap.DeviceTier.forceStandard.get()) }
            OutlinedButton(
                onClick  = { forceStandard = !forceStandard; com.sol.dopaminetrap.DeviceTier.forceStandard.set(forceStandard) },
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(10.dp),
                border   = BorderStroke(1.dp, SfBorder),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = SfDark)
            ) {
                Text(if (forceStandard) "DeviceTier: STANDARD forțat — resetează" else "Forțează mod STANDARD",
                    style = MaterialTheme.typography.bodySmall)
            }

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

    HorizontalDivider(color = SfBorder.copy(0.5f))
    Text("Monitor baterie", style = MaterialTheme.typography.labelSmall, color = SfGray)
    Text(
        if (unsupported) "BATTERY_PROPERTY_CURRENT_NOW nesupport"
        else "Curent: ${currentMa} mA  |  Medie 30s: ${avgMa} mA",
        style = MaterialTheme.typography.bodySmall,
        color = if (unsupported) MaterialTheme.colorScheme.error else SfGray
    )
}

// ── App dezactivată ───────────────────────────────────────────────────────────

@Composable
fun AppDisabledScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SfCream, SfBg2))),
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
                color      = SfDark
            )
            Text(
                "Părintele tău a dezactivat temporar accesul. Vorbește cu el dacă crezi că e o greșeală.",
                style     = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color     = SfGray
            )
        }
    }
}

// ── PIN lock screen ───────────────────────────────────────────────────────────

@Composable
fun PinLockScreen(correctPin: String, onUnlocked: () -> Unit) {
    var pin       by remember { mutableStateOf("") }
    var error     by remember { mutableStateOf(false) }
    var showInput by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SfMid),
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
                color      = SfCream
            )
            Text(
                "Aplicația este blocată.",
                style     = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color     = SfCream.copy(alpha = 0.9f)
            )
            Text(
                "Cere codul de deblocare de la părintele tău.",
                style     = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color     = SfCream.copy(alpha = 0.65f)
            )

            Spacer(Modifier.height(8.dp))

            if (!showInput) {
                OutlinedButton(
                    onClick = { showInput = true },
                    shape   = RoundedCornerShape(28.dp),
                    border  = BorderStroke(1.dp, SfCream.copy(alpha = 0.5f)),
                    colors  = ButtonDefaults.outlinedButtonColors(contentColor = SfCream)
                ) {
                    Text("Am codul", fontWeight = FontWeight.Medium)
                }
            } else {
                OutlinedTextField(
                    value         = pin,
                    onValueChange = {
                        if (it.length <= 6 && it.all { c -> c.isDigit() }) { pin = it; error = false }
                    },
                    label           = { Text("Cod de deblocare", color = SfCream.copy(alpha = 0.7f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine      = true,
                    isError         = error,
                    colors          = OutlinedTextFieldDefaults.colors(
                        focusedTextColor      = SfCream,
                        unfocusedTextColor    = SfCream,
                        focusedBorderColor    = SfCream,
                        unfocusedBorderColor  = SfCream.copy(alpha = 0.5f),
                        errorBorderColor      = StatusRed
                    ),
                    supportingText = if (error) {{ Text("Cod incorect.", color = StatusRed) }} else null,
                    modifier       = Modifier.fillMaxWidth()
                )

                Button(
                    onClick  = { if (pin == correctPin) onUnlocked() else { error = true; pin = "" } },
                    enabled  = pin.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(28.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = SfCream,
                        contentColor   = SfDark
                    )
                ) {
                    Text("Deblochează", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
