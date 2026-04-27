package com.sol.dopaminetrap.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sol.dopaminetrap.ChildInfo
import com.sol.dopaminetrap.FirebaseRepository
import com.sol.dopaminetrap.OnboardingManager
import com.sol.dopaminetrap.data.FamilySettings
import com.sol.dopaminetrap.data.WellbeingProfile
import com.sol.dopaminetrap.ui.theme.BrandIndigo
import com.sol.dopaminetrap.ui.theme.StatusAmber
import com.sol.dopaminetrap.ui.theme.StatusGreen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// ── Root screen ───────────────────────────────────────────────────────────────

@Composable
fun ParentScreen() {
    val context  = LocalContext.current
    val familyId = remember { OnboardingManager.getFamilyId(context) ?: "" }

    var children           by remember { mutableStateOf<List<ChildInfo>>(emptyList()) }
    var selectedChild      by remember { mutableStateOf<ChildInfo?>(null) }
    var isLoadingChildren  by remember { mutableStateOf(true) }

    LaunchedEffect(familyId) {
        if (familyId.isNotEmpty()) {
            children = FirebaseRepository.fetchChildren(familyId)
            if (children.size == 1) selectedChild = children.first()
            if (children.isEmpty()) {
                val id   = OnboardingManager.getChildId(context)
                val name = OnboardingManager.getChildName(context) ?: "Copil"
                if (id != null) selectedChild = ChildInfo(id, name)
            }
        }
        isLoadingChildren = false
    }

    Column(Modifier.fillMaxSize()) {
        ParentHeader(childName = selectedChild?.childName)

        if (isLoadingChildren) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandIndigo)
            }
            return@Column
        }

        if (children.size > 1) {
            ChildSelectorBar(children = children, selected = selectedChild, onSelect = { selectedChild = it })
        }

        val child = selectedChild
        if (child == null) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Text(
                        "Niciun copil conectat. Fă pairing mai întâi de pe telefonul copilului.",
                        modifier = Modifier.padding(20.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return@Column
        }

        var selectedTab by remember { mutableStateOf(0) }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor   = MaterialTheme.colorScheme.surface,
            contentColor     = BrandIndigo
        ) {
            listOf("🔔 Alerte", "⚙️ Control", "🧠 Bunăstare", "📊 Rapoarte")
                .forEachIndexed { i, label ->
                    Tab(
                        selected = selectedTab == i,
                        onClick  = { selectedTab = i },
                        text     = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
        }

        when (selectedTab) {
            0 -> NotificationsTab(familyId = familyId, childId = child.childId)
            1 -> ControlTab(familyId = familyId, child = child)
            2 -> WellbeingTab(familyId = familyId, childId = child.childId)
            3 -> ReportsTab(familyId = familyId, childId = child.childId)
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun ParentHeader(childName: String?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(Color(0xFF5C6BC0), Color(0xFF3F8EFC))))
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Column {
            Text(
                "Panou Părinte",
                style        = MaterialTheme.typography.titleSmall,
                color        = Color.White.copy(alpha = 0.75f),
                letterSpacing = 1.sp
            )
            if (childName != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    childName,
                    style      = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White
                )
            }
        }
        Text("👨‍👧", fontSize = 34.sp, modifier = Modifier.align(Alignment.CenterEnd))
    }
}

// ── Child selector bar ────────────────────────────────────────────────────────

@Composable
private fun ChildSelectorBar(
    children: List<ChildInfo>,
    selected: ChildInfo?,
    onSelect: (ChildInfo) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            Modifier.clickable { expanded = true },
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("👤", fontSize = 14.sp)
            Text(
                selected?.childName ?: "Alege copilul",
                style      = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text("▾", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            children.forEach { child ->
                DropdownMenuItem(
                    text    = { Text(child.childName) },
                    onClick = { onSelect(child); expanded = false }
                )
            }
        }
    }
}

// ── Tab 0: Alerte ─────────────────────────────────────────────────────────────

@Composable
private fun NotificationsTab(familyId: String, childId: String) {
    var allAlerts by remember(childId) { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember(childId) { mutableStateOf(true) }
    var clearedAt by remember(childId) { mutableStateOf(0L) }

    LaunchedEffect(childId) {
        isLoading = true
        allAlerts = FirebaseRepository.fetchRecentAlerts(familyId, childId)
        isLoading = false
    }

    val alerts = allAlerts.filter { (it["timestamp"] as? Long ?: 0L) > clearedAt }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Header row ────────────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "Alerte recente",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (alerts.isNotEmpty()) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.error) {
                        Text(
                            "${alerts.size}",
                            style    = MaterialTheme.typography.labelSmall,
                            color    = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            if (alerts.isNotEmpty()) {
                TextButton(
                    onClick        = { clearedAt = System.currentTimeMillis() },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text("Șterge tot", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // ── Lista ─────────────────────────────────────────────────────────────
        if (isLoading) {
            Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandIndigo)
            }
        } else if (alerts.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(top = 56.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✅", fontSize = 44.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Nicio alertă recentă",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            alerts.forEach { alert -> ExpandableAlertCard(alert) }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ExpandableAlertCard(alert: Map<String, Any>) {
    var expanded by remember { mutableStateOf(false) }

    val timestamp    = (alert["timestamp"] as? Long) ?: 0L
    val category     = alert["category"]    as? String ?: "necunoscut"
    val sourceApp    = alert["sourceApp"]   as? String ?: ""
    val message      = alert["message"]     as? String ?: ""
    val isWellbeing  = alert["sourceType"]  == "wellbeing"
    val concernLevel = alert["concernLevel"] as? String ?: "NONE"

    val accentColor = when {
        isWellbeing                                -> BrandIndigo
        concernLevel in listOf("CRITICAL", "HIGH") -> MaterialTheme.colorScheme.error
        concernLevel == "MEDIUM"                   -> StatusAmber
        else                                       -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier  = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        shape     = RoundedCornerShape(12.dp),
        border    = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            // ── Rând principal ────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(if (isWellbeing) "💭" else "⚠️", fontSize = 18.sp)
                Column(Modifier.weight(1f)) {
                    Text(
                        category,
                        style      = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color      = if (isWellbeing) BrandIndigo else MaterialTheme.colorScheme.error,
                        maxLines   = if (expanded) Int.MAX_VALUE else 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                    if (sourceApp.isNotEmpty()) {
                        Text(
                            sourceApp,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        relativeTime(timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (expanded) "▲" else "▼",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandIndigo
                    )
                }
            }

            // ── Detalii expandate ─────────────────────────────────────────────
            if (expanded) {
                if (message.isNotBlank()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color    = MaterialTheme.colorScheme.outline.copy(0.2f)
                    )
                    Text(
                        message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (concernLevel !in listOf("NONE", "")) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = accentColor.copy(0.12f)
                    ) {
                        Text(
                            "Nivel risc: $concernLevel",
                            style      = MaterialTheme.typography.labelSmall,
                            color      = accentColor,
                            fontWeight = FontWeight.SemiBold,
                            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun relativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val min  = diff / 60_000
    val h    = diff / 3_600_000
    val days = diff / 86_400_000
    return when {
        min  < 1   -> "acum"
        min  < 60  -> "acum ${min}m"
        h    < 24  -> "acum ${h}h"
        days == 1L -> "ieri"
        else       -> SimpleDateFormat("dd MMM", Locale("ro")).format(Date(timestamp))
    }
}

// ── Tab 1: Control ────────────────────────────────────────────────────────────

@Composable
private fun ControlTab(familyId: String, child: ChildInfo) {
    val scope = rememberCoroutineScope()
    var settings    by remember(child.childId) { mutableStateOf(FamilySettings()) }
    var isLoading   by remember(child.childId) { mutableStateOf(true) }
    var isSaving    by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(child.childId) {
        isLoading = true
        settings  = FirebaseRepository.fetchSettings(familyId, child.childId)
        isLoading = false
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isLoading) {
            Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = BrandIndigo)
            }
            return@Column
        }

        // Limite aplicații
        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Limite aplicații",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                AppLimitRow(
                    label         = "TikTok",
                    enabled       = settings.tiktokEnabled,
                    limitMinutes  = settings.tiktokLimitMinutes,
                    onToggle      = { settings = settings.copy(tiktokEnabled = it) },
                    onLimitChange = { settings = settings.copy(tiktokLimitMinutes = it) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color    = MaterialTheme.colorScheme.outline.copy(0.4f)
                )
                AppLimitRow(
                    label         = "Instagram",
                    enabled       = settings.instagramEnabled,
                    limitMinutes  = settings.instagramLimitMinutes,
                    onToggle      = { settings = settings.copy(instagramEnabled = it) },
                    onLimitChange = { settings = settings.copy(instagramLimitMinutes = it) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color    = MaterialTheme.colorScheme.outline.copy(0.4f)
                )
                AppLimitRow(
                    label         = "YouTube Shorts",
                    enabled       = settings.youtubeShortsEnabled,
                    limitMinutes  = settings.youtubeShortsLimitMinutes,
                    onToggle      = { settings = settings.copy(youtubeShortsEnabled = it) },
                    onLimitChange = { settings = settings.copy(youtubeShortsLimitMinutes = it) }
                )
            }
        }

        ThrottleAdvancedPanel(settings = settings, onSettingsChange = { settings = it })

        Button(
            onClick = {
                isSaving    = true
                saveMessage = null
                scope.launch {
                    runCatching {
                        FirebaseRepository.pushSettings(familyId, child.childId, settings)
                        saveMessage = "Setări salvate!"
                    }.onFailure {
                        saveMessage = "Eroare: ${it.message}"
                    }
                    isSaving = false
                }
            },
            enabled  = !isSaving,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = BrandIndigo)
        ) {
            if (isSaving)
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
            else
                Text("Aplică setările", fontWeight = FontWeight.SemiBold)
        }

        saveMessage?.let { msg ->
            Text(
                msg,
                style    = MaterialTheme.typography.bodySmall,
                color    = if (msg.startsWith("Eroare")) MaterialTheme.colorScheme.error
                           else MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ── Tab 2: Bunăstare ──────────────────────────────────────────────────────────

@Composable
private fun WellbeingTab(familyId: String, childId: String) {
    var profile   by remember(childId) { mutableStateOf<WellbeingProfile?>(null) }
    var isLoading by remember(childId) { mutableStateOf(true) }

    LaunchedEffect(childId) {
        isLoading = true
        profile   = FirebaseRepository.fetchWellbeingProfile(familyId, childId)
        isLoading = false
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isLoading) {
            Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandIndigo)
            }
        } else if (profile == null) {
            Box(Modifier.fillMaxWidth().padding(top = 56.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🧠", fontSize = 44.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Profilul va apărea după primul raport zilnic.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            WellbeingProfileCard(profile = profile!!)
        }
        Spacer(Modifier.height(8.dp))
    }
}

// ── Tab 3: Rapoarte ───────────────────────────────────────────────────────────

@Composable
private fun ReportsTab(familyId: String, childId: String) {
    var reports   by remember(childId) { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember(childId) { mutableStateOf(true) }

    LaunchedEffect(childId) {
        isLoading = true
        reports   = FirebaseRepository.fetchReports(familyId, childId)
        isLoading = false
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Rapoarte săptămânale",
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (isLoading) {
            Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandIndigo)
            }
        } else if (reports.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(top = 56.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📊", fontSize = 44.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Niciun raport încă.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Apasă butonul de test de pe telefonul copilului.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            reports.forEach { report -> ModernReportCard(report) }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ModernReportCard(report: Map<String, Any>) {
    var expanded by remember { mutableStateOf(false) }

    val timestamp    = (report["timestamp"] as? Long) ?: 0L
    val date         = SimpleDateFormat("dd MMMM yyyy", Locale("ro")).format(Date(timestamp))
    val title        = report["title"]        as? String ?: "Raport"
    val message      = report["message"]      as? String ?: ""
    val concernLevel = report["concernLevel"] as? String ?: "NONE"
    val suggestion   = report["suggestion"]   as? String

    val (accentColor, levelLabel, levelEmoji) = when (concernLevel) {
        "CRITICAL" -> Triple(MaterialTheme.colorScheme.error, "Critic",  "🔴")
        "HIGH"     -> Triple(MaterialTheme.colorScheme.error, "Ridicat", "🟠")
        "MEDIUM"   -> Triple(StatusAmber, "Mediu",  "🟡")
        else       -> Triple(StatusGreen, "Normal", "🟢")
    }

    Card(
        modifier  = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            // Band colorat sus
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(accentColor.copy(alpha = 0.10f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                    Text(
                        date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.18f)
                ) {
                    Text(
                        "$levelEmoji $levelLabel",
                        style      = MaterialTheme.typography.labelSmall,
                        color      = accentColor,
                        fontWeight = FontWeight.SemiBold,
                        modifier   = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Corp
            Column(Modifier.padding(16.dp)) {
                if (message.isNotEmpty()) {
                    Text(
                        message,
                        style    = MaterialTheme.typography.bodySmall,
                        maxLines = if (expanded) Int.MAX_VALUE else 3,
                        overflow = if (expanded) TextOverflow.Clip else TextOverflow.Ellipsis
                    )
                }

                if (expanded && !suggestion.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.25f))
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("💡", fontSize = 16.sp)
                        Column {
                            Text(
                                "Sugestie AI",
                                style      = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color      = BrandIndigo
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(suggestion, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))
                Text(
                    if (expanded) "▲ Restrânge" else "▼ Citește tot",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = BrandIndigo,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

// ── Panou avansat throttle (colapsabil) ───────────────────────────────────────

@Composable
private fun ThrottleAdvancedPanel(
    settings: FamilySettings,
    onSettingsChange: (FamilySettings) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Setări avansate throttle",
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Burst: ${settings.burstSizeKb} KB · Pauză: ${settings.pauseDurationMs / 1000}s",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                Text(
                    if (expanded) "▲" else "▼",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (expanded) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color    = MaterialTheme.colorScheme.outline.copy(0.3f)
                )
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Acești parametri definesc comportamentul de bază al throttle-ului. " +
                        "Limita de timp îi modifică automat pe măsură ce copilul se apropie de limită.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Date înainte de pauză", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "${settings.burstSizeKb} KB",
                            style      = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color      = BrandIndigo
                        )
                    }
                    Slider(
                        value         = settings.burstSizeKb.toFloat(),
                        onValueChange = { onSettingsChange(settings.copy(burstSizeKb = it.roundToInt())) },
                        valueRange    = 16f..256f,
                        steps         = 14,
                        colors        = SliderDefaults.colors(thumbColor = BrandIndigo, activeTrackColor = BrandIndigo)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("16 KB (agresiv)", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("256 KB (blând)", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Durată pauză de bază", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "${settings.pauseDurationMs / 1000}s",
                            style      = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color      = BrandIndigo
                        )
                    }
                    Slider(
                        value         = settings.pauseDurationMs / 1000f,
                        onValueChange = { onSettingsChange(settings.copy(pauseDurationMs = (it * 1000).toLong())) },
                        valueRange    = 1f..10f,
                        steps         = 8,
                        colors        = SliderDefaults.colors(thumbColor = BrandIndigo, activeTrackColor = BrandIndigo)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("1s (blând)", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("10s (agresiv)", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// ── App toggle + limită de timp ───────────────────────────────────────────────

private val TIME_LIMIT_OPTIONS = listOf(0, 30, 60, 90, 120)

private fun limitLabel(minutes: Int) = when (minutes) {
    0   -> "Fără"
    60  -> "1h"
    90  -> "1h 30m"
    120 -> "2h"
    else -> "${minutes}m"
}

@Composable
private fun AppLimitRow(
    label: String,
    enabled: Boolean,
    limitMinutes: Int,
    onToggle: (Boolean) -> Unit,
    onLimitChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Switch(
                checked         = enabled,
                onCheckedChange = onToggle,
                colors          = SwitchDefaults.colors(checkedTrackColor = BrandIndigo)
            )
        }
        if (enabled) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "Limită zilnică:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TIME_LIMIT_OPTIONS.forEach { option ->
                    FilterChip(
                        selected = limitMinutes == option,
                        onClick  = { onLimitChange(option) },
                        label    = { Text(limitLabel(option), style = MaterialTheme.typography.labelSmall) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandIndigo,
                            selectedLabelColor     = Color.White
                        )
                    )
                }
            }
        }
    }
}
