package com.sol.dopaminetrap.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sol.dopaminetrap.ChildInfo
import com.sol.dopaminetrap.FirebaseRepository
import com.sol.dopaminetrap.OnboardingManager
import com.sol.dopaminetrap.data.FamilySettings
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ParentScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val familyId = remember { OnboardingManager.getFamilyId(context) ?: "" }

    var children by remember { mutableStateOf<List<ChildInfo>>(emptyList()) }
    var selectedChild by remember { mutableStateOf<ChildInfo?>(null) }
    var isLoadingChildren by remember { mutableStateOf(true) }

    // Incarca lista de copii
    LaunchedEffect(familyId) {
        if (familyId.isNotEmpty()) {
            children = FirebaseRepository.fetchChildren(familyId)
            // Daca parintele e conectat la un singur copil (cazul standard), selecteaza-l automat
            if (children.size == 1) selectedChild = children.first()
            // Daca parintele e el insusi un copil pairat, selecteaza dupa childId local
            if (children.isEmpty()) {
                val localChildId = OnboardingManager.getChildId(context)
                val localChildName = OnboardingManager.getChildName(context) ?: "Copil"
                if (localChildId != null) {
                    selectedChild = ChildInfo(localChildId, localChildName)
                }
            }
        }
        isLoadingChildren = false
    }

    if (isLoadingChildren) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Panou Parinte",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // ── Selector copil (doar daca sunt mai multi) ─────────────────────────
        if (children.size > 1) {
            ChildSelector(
                children = children,
                selected = selectedChild,
                onSelect = { selectedChild = it }
            )
        } else if (selectedChild != null) {
            Text(
                "Copil: ${selectedChild!!.childName}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val child = selectedChild
        if (child == null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Niciun copil conectat. Fa pairing mai intai de pe telefonul copilului.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Column
        }

        // ── Setari per copil ──────────────────────────────────────────────────
        ChildSettingsSection(
            familyId = familyId,
            child = child
        )

        // ── Rapoarte saptamanale ──────────────────────────────────────────────
        ReportsSection(familyId = familyId, childId = child.childId)

        // ── Alerte recente ────────────────────────────────────────────────────
        AlertsSection(familyId = familyId, childId = child.childId)

        Spacer(Modifier.height(8.dp))
    }
}

// ── Selector copii ────────────────────────────────────────────────────────────

@Composable
private fun ChildSelector(
    children: List<ChildInfo>,
    selected: ChildInfo?,
    onSelect: (ChildInfo) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Selecteaza copilul", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(selected?.childName ?: "Alege copilul")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    children.forEach { child ->
                        DropdownMenuItem(
                            text = { Text(child.childName) },
                            onClick = {
                                onSelect(child)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

// ── Setari + buton salvare ────────────────────────────────────────────────────

@Composable
private fun ChildSettingsSection(familyId: String, child: ChildInfo) {
    val scope = rememberCoroutineScope()
    var settings by remember(child.childId) { mutableStateOf(FamilySettings()) }
    var isLoading by remember(child.childId) { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(child.childId) {
        isLoading = true
        settings = FirebaseRepository.fetchSettings(familyId, child.childId)
        isLoading = false
    }

    if (isLoading) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
        }
        return
    }

    // Aplicatii protejate
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Aplicatii protejate",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            AppToggleRow("TikTok", settings.tiktokEnabled) { settings = settings.copy(tiktokEnabled = it) }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            AppToggleRow("Instagram", settings.instagramEnabled) { settings = settings.copy(instagramEnabled = it) }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            AppToggleRow("YouTube Shorts", settings.youtubeShortsEnabled) { settings = settings.copy(youtubeShortsEnabled = it) }
        }
    }

    // Throttle
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Intensitate throttle",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Controleaza cat de agresiva este limitarea vitezei.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Date inainte de pauza", style = MaterialTheme.typography.bodyMedium)
                Text("${settings.burstSizeKb} KB", style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = settings.burstSizeKb.toFloat(),
                onValueChange = { settings = settings.copy(burstSizeKb = it.roundToInt()) },
                valueRange = 16f..256f, steps = 14,
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("16 KB (agresiv)", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("256 KB (bland)", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Durata pauza", style = MaterialTheme.typography.bodyMedium)
                Text("${settings.pauseDurationMs / 1000}s", style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = (settings.pauseDurationMs / 1000f),
                onValueChange = { settings = settings.copy(pauseDurationMs = (it * 1000).toLong()) },
                valueRange = 1f..10f, steps = 8,
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("1s (bland)", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("10s (agresiv)", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    // Buton salvare
    Button(
        onClick = {
            isSaving = true
            saveMessage = null
            scope.launch {
                runCatching {
                    FirebaseRepository.pushSettings(familyId, child.childId, settings)
                    saveMessage = "Setari trimise cu succes!"
                }.onFailure {
                    saveMessage = "Eroare: ${it.message}"
                }
                isSaving = false
            }
        },
        enabled = !isSaving,
        modifier = Modifier.fillMaxWidth().height(56.dp)
    ) {
        if (isSaving) CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.dp
        )
        else Text("Aplica setarile", style = MaterialTheme.typography.titleMedium)
    }

    saveMessage?.let { msg ->
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                msg,
                style = MaterialTheme.typography.bodySmall,
                color = if (msg.startsWith("Eroare")) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ── Rapoarte saptamanale ──────────────────────────────────────────────────────

@Composable
private fun ReportsSection(familyId: String, childId: String) {
    val scope = rememberCoroutineScope()
    var reports by remember(childId) { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember(childId) { mutableStateOf(true) }

    LaunchedEffect(childId) {
        isLoading = true
        reports = FirebaseRepository.fetchReports(familyId, childId)
        isLoading = false
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Rapoarte saptamanale",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally))
            } else if (reports.isEmpty()) {
                Text(
                    "Niciun raport inca. Apasa butonul de test de pe telefonul copilului pentru a genera primul raport.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                reports.forEach { report ->
                    ReportCard(report)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ReportCard(report: Map<String, Any>) {
    val timestamp = (report["timestamp"] as? Long) ?: 0L
    val date = SimpleDateFormat("dd MMM yyyy", Locale("ro")).format(Date(timestamp))
    val title = report["title"] as? String ?: ""
    val message = report["message"] as? String ?: ""
    val concernLevel = report["concernLevel"] as? String ?: "NONE"

    val containerColor = when (concernLevel) {
        "CRITICAL", "HIGH" -> MaterialTheme.colorScheme.errorContainer
        "MEDIUM" -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f))
                Text(date, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (message.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(message, style = MaterialTheme.typography.bodySmall)
            }
            val suggestion = report["suggestion"] as? String
            if (!suggestion.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(Modifier.height(8.dp))
                Text(
                    "Sugestie AI",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(suggestion, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// ── Alerte recente ────────────────────────────────────────────────────────────

@Composable
private fun AlertsSection(familyId: String, childId: String) {
    var alerts by remember(childId) { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember(childId) { mutableStateOf(true) }

    LaunchedEffect(childId) {
        isLoading = true
        alerts = FirebaseRepository.fetchRecentAlerts(familyId, childId)
        isLoading = false
    }

    if (!isLoading && alerts.isEmpty()) return

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Alerte recente",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally))
            } else {
                alerts.forEach { alert ->
                    AlertRow(alert)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun AlertRow(alert: Map<String, Any>) {
    val timestamp = (alert["timestamp"] as? Long) ?: 0L
    val date = SimpleDateFormat("dd MMM, HH:mm", Locale("ro")).format(Date(timestamp))
    val category = alert["category"] as? String ?: "necunoscut"
    val sourceApp = alert["sourceApp"] as? String ?: ""

    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(category, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.error)
            if (sourceApp.isNotEmpty()) {
                Text("in $sourceApp", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(date, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── App toggle row ────────────────────────────────────────────────────────────

@Composable
private fun AppToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
