package com.sol.dopaminetrap.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.sol.dopaminetrap.FirebaseRepository
import com.sol.dopaminetrap.OnboardingManager
import com.sol.dopaminetrap.OnboardingManager.DeviceMode
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.math.roundToInt

private fun generatePairingCode(): String =
    (100000..999999).random().toString()

// ── Pasul 1 — alegere mod ────────────────────────────────────────────────────

@Composable
fun ModeSelectionScreen(onModeSelected: (DeviceMode) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Safeland",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Cine foloseste acest telefon?",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(48.dp))

        Button(
            onClick = { onModeSelected(DeviceMode.PARENT) },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Sunt parinte", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = { onModeSelected(DeviceMode.CHILD) },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Sunt copil / Configureaza pentru copil", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ── Pasul 2a — copil: nume + vârstă, generează cod ──────────────────────────

@Composable
fun ChildSetupScreen(onDone: (familyId: String, childId: String, childName: String, childAge: Int) -> Unit) {
    val scope = rememberCoroutineScope()

    var childName by remember { mutableStateOf("") }
    var childAge  by remember { mutableStateOf(13) }
    var nameConfirmed by remember { mutableStateOf(false) }

    var pairingCode by remember { mutableStateOf("") }
    var familyId    by remember { mutableStateOf("") }
    var childId     by remember { mutableStateOf("") }
    var isLoading   by remember { mutableStateOf(false) }
    var error       by remember { mutableStateOf<String?>(null) }

    fun generateCode(name: String) {
        isLoading = true
        error = null
        scope.launch {
            runCatching {
                val fid  = UUID.randomUUID().toString()
                val cid  = UUID.randomUUID().toString()
                val code = generatePairingCode()
                Firebase.firestore.collection("pairing").document(code).set(
                    mapOf(
                        "familyId"  to fid,
                        "childId"   to cid,
                        "childName" to name,
                        "createdAt" to System.currentTimeMillis()
                    )
                ).await()
                FirebaseRepository.registerChild(fid, cid, name)
                familyId    = fid
                childId     = cid
                pairingCode = code
            }.onFailure {
                error = "Eroare la generarea codului. Verifica conexiunea la internet."
            }
            isLoading = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Configureaza pentru copil",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))

        if (!nameConfirmed) {
            // ── Ecran: nume + vârstă ──────────────────────────────────────────
            Text(
                "Cum se numeste copilul si cati ani are?",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = childName,
                onValueChange = { childName = it },
                label = { Text("Prenumele copilului") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))

            // Slider vârstă
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Vârsta", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "$childAge ani",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = childAge.toFloat(),
                onValueChange = { childAge = it.roundToInt() },
                valueRange = 8f..18f,
                steps = 9,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("8 ani", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("18 ani", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    nameConfirmed = true
                    generateCode(childName.trim().ifEmpty { "Copil" })
                },
                enabled = true,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Continua", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            // ── Ecran: cod pairing ────────────────────────────────────────────
            Text(
                "Da codul de mai jos parintelui. El il va introduce in aplicatia lui pentru a se conecta.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(40.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { generateCode(childName.trim().ifEmpty { "Copil" }) }) {
                    Text("Incearca din nou")
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        pairingCode,
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 8.sp
                        ),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(40.dp))
                Button(
                    onClick = {
                        isLoading = true
                        scope.launch {
                            val resolvedFamilyId = runCatching {
                                Firebase.firestore.collection("pairing").document(pairingCode)
                                    .get().await().getString("resolvedFamilyId")
                            }.getOrNull() ?: familyId
                            if (resolvedFamilyId != familyId) {
                                runCatching {
                                    FirebaseRepository.registerChild(
                                        resolvedFamilyId, childId,
                                        childName.trim().ifEmpty { "Copil" }
                                    )
                                }
                            }
                            isLoading = false
                            onDone(resolvedFamilyId, childId, childName.trim().ifEmpty { "Copil" }, childAge)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    if (isLoading)
                        CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    else
                        Text("Parintele a introdus codul — Continua")
                }
            }
        }
    }
}

// ── Pasul 2b — parinte: introduce codul copilului ────────────────────────────

@Composable
fun ParentSetupScreen(onDone: (familyId: String, childId: String, childName: String) -> Unit) {
    val scope = rememberCoroutineScope()
    var code      by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error     by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Conecteaza-te la copil", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text(
            "Introdu codul de 6 cifre afisat pe telefonul copilului.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(40.dp))

        OutlinedTextField(
            value = code,
            onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) code = it },
            label = { Text("Cod de 6 cifre") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 28.sp,
                letterSpacing = 6.sp,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                if (code.length != 6) { error = "Codul trebuie sa aiba 6 cifre."; return@Button }
                isLoading = true
                error = null
                scope.launch {
                    runCatching {
                        val doc = Firebase.firestore.collection("pairing").document(code).get().await()
                        if (!doc.exists()) throw Exception("Cod invalid sau expirat.")
                        val fid  = doc.getString("familyId")  ?: throw Exception("Date corupte.")
                        val cid  = doc.getString("childId")   ?: throw Exception("Date corupte.")
                        val name = doc.getString("childName") ?: "Copil"
                        onDone(fid, cid, name)
                    }.onFailure {
                        error = it.message ?: "Eroare necunoscuta."
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading && code.length == 6,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
            else Text("Conecteaza", style = MaterialTheme.typography.titleMedium)
        }
    }
}

// ── Pasul 3 (doar copil) — ghid permisiuni pas-cu-pas ───────────────────────

@Composable
fun PermissionsGuideScreen(onComplete: () -> Unit) {
    val context = LocalContext.current

    var notifGranted       by remember { mutableStateOf(false) }
    var a11yEnabled        by remember { mutableStateOf(false) }
    var notifListenerOn    by remember { mutableStateOf(false) }
    var smsGranted         by remember { mutableStateOf(false) }

    fun refreshPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifGranted = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            notifGranted = true
        }
        val a11yFlat = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        a11yEnabled = a11yFlat.contains(context.packageName, ignoreCase = true)
        val nlFlat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: ""
        notifListenerOn = nlFlat.contains(context.packageName)
        smsGranted = context.checkSelfPermission(Manifest.permission.RECEIVE_SMS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refreshPermissions() }

    val smsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refreshPermissions() }

    // Refresh when screen first appears and when resuming from settings
    LaunchedEffect(Unit) { refreshPermissions() }

    val permissions = listOf(
        Triple(
            "Notificări",
            "Necesar pentru alerte despre conținut periculos.",
            notifGranted
        ),
        Triple(
            "Serviciu de accesibilitate",
            "Detectează conținut YouTube Shorts și TikTok pentru throttle.",
            a11yEnabled
        ),
        Triple(
            "Acces la notificări",
            "Monitorizează mesajele din WhatsApp, Telegram și alte app-uri.",
            notifListenerOn
        ),
        Triple(
            "Permisiune SMS",
            "Monitorizează mesajele SMS primite.",
            smsGranted
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Activează permisiunile",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Aceste permisiuni permit Safeland să funcționeze corect. Le poți activa oricând și din setările telefonului.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))

        permissions.forEachIndexed { index, (title, desc, granted) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (granted)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(if (granted) "✅" else "⬜", fontSize = 22.sp)
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(desc, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (!granted) {
                        TextButton(
                            onClick = {
                                when (index) {
                                    0 -> {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    }
                                    1 -> context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                    2 -> context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                    3 -> smsLauncher.launch(arrayOf(
                                        Manifest.permission.RECEIVE_SMS,
                                        Manifest.permission.READ_SMS
                                    ))
                                }
                            }
                        ) {
                            Text("Activează", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(
                if (permissions.all { it.third }) "Gata — intru în aplicație" else "Continuă fără toate permisiunile",
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (!permissions.all { it.third }) {
            TextButton(onClick = { refreshPermissions() }) {
                Text("Actualizează starea permisiunilor", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// ── Ecran principal de onboarding — orchestrează flow-ul ─────────────────────

@Composable
fun OnboardingScreen(onOnboardingComplete: () -> Unit) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var selectedMode    by remember { mutableStateOf<DeviceMode?>(null) }
    var childSetupDone  by remember { mutableStateOf(false) }
    var pendingFamilyId by remember { mutableStateOf("") }
    var pendingChildId  by remember { mutableStateOf("") }
    var pendingName     by remember { mutableStateOf("") }
    var pendingAge      by remember { mutableStateOf(13) }

    when {
        selectedMode == null -> ModeSelectionScreen(onModeSelected = { selectedMode = it })

        selectedMode == DeviceMode.CHILD && !childSetupDone ->
            ChildSetupScreen(onDone = { fid, cid, name, age ->
                pendingFamilyId = fid
                pendingChildId  = cid
                pendingName     = name
                pendingAge      = age
                childSetupDone  = true
            })

        selectedMode == DeviceMode.CHILD && childSetupDone ->
            PermissionsGuideScreen(onComplete = {
                scope.launch {
                    // Push initial settings with childAge to Firestore
                    runCatching {
                        val initialSettings = com.sol.dopaminetrap.data.FamilySettings(childAge = pendingAge)
                        FirebaseRepository.pushSettings(pendingFamilyId, pendingChildId, initialSettings)
                    }
                    OnboardingManager.completeOnboarding(
                        context, DeviceMode.CHILD,
                        pendingFamilyId, pendingChildId, pendingName, pendingAge
                    )
                    onOnboardingComplete()
                }
            })

        selectedMode == DeviceMode.PARENT ->
            ParentSetupScreen(onDone = { fid, cid, name ->
                OnboardingManager.completeOnboarding(context, DeviceMode.PARENT, fid, cid, name)
                onOnboardingComplete()
            })
    }
}
