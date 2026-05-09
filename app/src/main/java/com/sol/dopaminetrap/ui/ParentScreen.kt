package com.sol.dopaminetrap.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sol.dopaminetrap.ChildInfo
import com.sol.dopaminetrap.FirebaseRepository
import com.sol.dopaminetrap.analysis.categoryLabel
import com.sol.dopaminetrap.OnboardingManager
import com.sol.dopaminetrap.data.FamilySettings
import com.sol.dopaminetrap.data.WellbeingProfile
import com.sol.dopaminetrap.ui.theme.*
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

    var children          by remember { mutableStateOf<List<ChildInfo>>(emptyList()) }
    var selectedChild     by remember { mutableStateOf<ChildInfo?>(null) }
    var isLoadingChildren by remember { mutableStateOf(true) }
    var loadError         by remember { mutableStateOf<String?>(null) }
    var retryKey          by remember { mutableStateOf(0) }

    LaunchedEffect(familyId, retryKey) {
        isLoadingChildren = true
        loadError         = null
        if (familyId.isNotEmpty()) {
            runCatching {
                children = FirebaseRepository.fetchChildren(familyId)
                if (children.size == 1) selectedChild = children.first()
                if (children.isEmpty()) {
                    val id   = OnboardingManager.getChildId(context)
                    val name = OnboardingManager.getChildName(context) ?: "Copil"
                    if (id != null) selectedChild = ChildInfo(id, name)
                }
            }.onFailure { e -> loadError = e.message ?: e.javaClass.simpleName }
        }
        isLoadingChildren = false
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(SfCream, SfBg2)))
    ) {
        ParentHeader(childName = selectedChild?.childName)

        if (isLoadingChildren) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SfDark)
            }
            return@Column
        }

        // Eroare la încărcare — afișează eroarea și buton retry
        if (loadError != null) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Nu s-au putut încărca datele.", style = MaterialTheme.typography.bodyMedium)
                    Text(loadError!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    Button(onClick = { retryKey++ }, colors = ButtonDefaults.buttonColors(containerColor = SfDark)) {
                        Text("Încearcă din nou")
                    }
                }
            }
            return@Column
        }

        // Niciun copil — arată direct tab-ul Familie ca să știe cum să facă pairing
        if (children.isEmpty()) {
            FamilieTab(
                familyId         = familyId,
                children         = emptyList(),
                onChildrenChanged = { }
            )
            return@Column
        }

        if (children.size > 1) {
            ChildSelectorBar(children = children, selected = selectedChild, onSelect = { selectedChild = it })
        }

        val child = selectedChild ?: children.first()

        var selectedTab  by remember { mutableStateOf(0) }
        var alertCount   by remember(child.childId) { mutableStateOf(0) }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor   = Color.White,
            contentColor     = SfDark
        ) {
            listOf("🔔 Alerte", "⚙️ Control", "🧠 Bunăstare", "📊 Rapoarte", "👨‍👧 Familie", "💬 Suport")
                .forEachIndexed { i, label ->
                    Tab(
                        selected = selectedTab == i,
                        onClick  = { selectedTab = i },
                        text     = {
                            if (i == 0 && alertCount > 0) {
                                BadgedBox(badge = {
                                    Badge { Text("$alertCount", style = MaterialTheme.typography.labelSmall) }
                                }) {
                                    Text(label, style = MaterialTheme.typography.labelSmall)
                                }
                            } else {
                                Text(label, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    )
                }
        }

        when (selectedTab) {
            0 -> NotificationsTab(familyId = familyId, childId = child.childId, onUnreadCount = { alertCount = it })
            1 -> ControlTab(familyId = familyId, child = child)
            2 -> WellbeingTab(familyId = familyId, childId = child.childId)
            3 -> ReportsTab(familyId = familyId, childId = child.childId)
            4 -> FamilieTab(
                    familyId          = familyId,
                    children          = children,
                    onChildrenChanged = { updated ->
                        children      = updated
                        selectedChild = updated.find { it.childId == selectedChild?.childId }
                                        ?: updated.firstOrNull()
                    }
                 )
            5 -> SupportTab(familyId = familyId, childId = child.childId)
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun ParentHeader(childName: String?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SfMid)
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Column {
            Text(
                "Panou Părinte",
                style        = MaterialTheme.typography.titleSmall,
                color        = SfCream.copy(alpha = 0.65f),
                letterSpacing = 1.sp
            )
            if (childName != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    childName,
                    style      = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color      = SfCream
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
private fun NotificationsTab(familyId: String, childId: String, onUnreadCount: (Int) -> Unit = {}) {
    var allAlerts by remember(childId) { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember(childId) { mutableStateOf(true) }
    var clearedAt by remember(childId) { mutableStateOf(0L) }

    DisposableEffect(familyId, childId) {
        val reg = FirebaseRepository.startAlertsListener(familyId, childId) { list ->
            allAlerts = list
            isLoading = false
        }
        onDispose { reg.remove() }
    }

    val alerts = allAlerts.filter { (it["timestamp"] as? Long ?: 0L) > clearedAt }

    LaunchedEffect(alerts.size) { onUnreadCount(alerts.size) }

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
                CircularProgressIndicator(color = SfDark)
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

    val timestamp      = (alert["timestamp"] as? Long) ?: 0L
    val categoryRaw    = alert["category"]       as? String ?: "necunoscut"
    val typeRaw        = alert["type"]            as? String ?: categoryRaw
    val allCategoriesRaw = alert["allCategories"] as? String ?: ""
    val sourceApp      = alert["sourceApp"]   as? String ?: ""
    val isWellbeing    = alert["sourceType"]  == "wellbeing"
    val isSystem       = alert["sourceType"]  == "system"
    val concernLevel   = alert["concernLevel"] as? String ?: "NONE"
    val category = when (typeRaw) {
        "time_limit"  -> "Limită timp"
        "vpn_stopped" -> "VPN dezactivat"
        else          -> categoryLabel(categoryRaw)
    }
    val message        = (alert["message"] as? String)
        ?.takeIf { it.isNotBlank() }
        ?: allCategoriesRaw.split("|").filter { it.isNotEmpty() }
            .joinToString(", ") { categoryLabel(it) }

    val accentColor = when {
        isWellbeing                                -> SfDark
        typeRaw == "vpn_stopped"                   -> MaterialTheme.colorScheme.error
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
                Text(when {
                    isWellbeing              -> "💭"
                    typeRaw == "vpn_stopped" -> "🛡️"
                    typeRaw == "time_limit"  -> "⏰"
                    else                     -> "⚠️"
                }, fontSize = 18.sp)
                Column(Modifier.weight(1f)) {
                    Text(
                        category,
                        style      = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color      = accentColor,
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
                        color = SfDark
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
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = SfDark)
            }
            return@Column
        }

        Text(
            "Aplicații protejate",
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onSurfaceVariant
        )

        AppLimitCard("TikTok", "🎵", Color(0xFF010101),
            settings.tiktokEnabled, settings.tiktokLimitMinutes,
            { settings = settings.copy(tiktokEnabled = it) },
            { settings = settings.copy(tiktokLimitMinutes = it) })
        AppLimitCard("Instagram", "📸", Color(0xFFE1306C),
            settings.instagramEnabled, settings.instagramLimitMinutes,
            { settings = settings.copy(instagramEnabled = it) },
            { settings = settings.copy(instagramLimitMinutes = it) })
        AppLimitCard("Instagram Reels", "🎬", Color(0xFFC13584),
            settings.instagramReelsEnabled, settings.instagramReelsLimitMinutes,
            { settings = settings.copy(instagramReelsEnabled = it) },
            { settings = settings.copy(instagramReelsLimitMinutes = it) })
        AppLimitCard("YouTube Shorts", "▶️", Color(0xFFFF0000),
            settings.youtubeShortsEnabled, settings.youtubeShortsLimitMinutes,
            { settings = settings.copy(youtubeShortsEnabled = it) },
            { settings = settings.copy(youtubeShortsLimitMinutes = it) })
        AppLimitCard("YouTube", "📺", Color(0xFFCC0000),
            settings.youtubeEnabled, settings.youtubeLimitMinutes,
            { settings = settings.copy(youtubeEnabled = it) },
            { settings = settings.copy(youtubeLimitMinutes = it) })
        AppLimitCard("Facebook", "👥", Color(0xFF1877F2),
            settings.facebookEnabled, settings.facebookLimitMinutes,
            { settings = settings.copy(facebookEnabled = it) },
            { settings = settings.copy(facebookLimitMinutes = it) })

        ThrottleAdvancedPanel(settings = settings, onSettingsChange = { settings = it })

        FeatureTogglesCard(settings = settings, onSettingsChange = { settings = it })

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
            colors   = ButtonDefaults.buttonColors(
                containerColor = SfDark,
                contentColor   = SfCream,
                disabledContainerColor = SfDark.copy(0.5f)
            )
        ) {
            if (isSaving)
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
            else
                Text("Aplică setările", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }

        saveMessage?.let { msg ->
            Surface(
                shape  = RoundedCornerShape(10.dp),
                color  = if (msg.startsWith("Eroare"))
                    MaterialTheme.colorScheme.errorContainer
                else Color(0xFF1B5E20).copy(0.12f)
            ) {
                Text(
                    msg,
                    style    = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color    = if (msg.startsWith("Eroare")) MaterialTheme.colorScheme.onErrorContainer
                               else Color(0xFF1B5E20),
                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                )
            }
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
                CircularProgressIndicator(color = SfDark)
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
                CircularProgressIndicator(color = SfDark)
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
                                color      = SfDark
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
                    color    = SfDark,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

// ── Tab 4: Familie ────────────────────────────────────────────────────────────

@Composable
private fun ChildControlCard(familyId: String, child: ChildInfo) {
    val scope = rememberCoroutineScope()
    var settings    by remember(child.childId) { mutableStateOf(FamilySettings()) }
    var isLoading   by remember(child.childId) { mutableStateOf(true) }
    var isSaving    by remember { mutableStateOf(false) }

    LaunchedEffect(child.childId) {
        isLoading = true
        settings  = FirebaseRepository.fetchSettings(familyId, child.childId)
        isLoading = false
    }

    fun save(updated: FamilySettings) {
        settings = updated
        isSaving = true
        scope.launch {
            runCatching { FirebaseRepository.pushSettings(familyId, child.childId, updated) }
            isSaving = false
        }
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (!settings.appEnabled)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Control aplicație — ${child.childName}",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                // Activare/dezactivare app
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Aplicație activă", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (settings.appEnabled) "Copilul poate folosi aplicația"
                            else "Aplicația este blocată pe telefonul copilului",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (settings.appEnabled) MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.error
                        )
                    }
                    Switch(
                        checked         = settings.appEnabled,
                        onCheckedChange = { save(settings.copy(appEnabled = it)) },
                        enabled         = !isSaving,
                        colors          = SwitchDefaults.colors(checkedTrackColor = SfDark)
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color    = MaterialTheme.colorScheme.outline.copy(0.2f)
                )

                // Blocare cu cod PIN
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text("Blocare cu cod", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = settings.lockEnabled,
                        onCheckedChange = { enabled ->
                            val newCode = if (enabled) (100000..999999).random().toString() else ""
                            save(settings.copy(lockEnabled = enabled, lockCode = newCode))
                        },
                        enabled = !isSaving,
                        colors  = SwitchDefaults.colors(checkedTrackColor = SfDark)
                    )
                }

                // Card proeminent cu codul
                if (settings.lockEnabled && settings.lockCode.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = CardDefaults.cardColors(containerColor = SfBg2),
                        border   = BorderStroke(1.dp, SfBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "Cod de deblocare",
                                style = MaterialTheme.typography.labelSmall,
                                color = SfGray
                            )
                            Text(
                                settings.lockCode,
                                style      = MaterialTheme.typography.displaySmall.copy(
                                    fontFamily    = FontFamily.Monospace,
                                    letterSpacing = 8.sp
                                ),
                                fontWeight = FontWeight.Bold,
                                color      = SfDark
                            )
                            Text(
                                "Spune-i copilului acest cod pentru a debloca aplicația.",
                                style     = MaterialTheme.typography.labelSmall,
                                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick  = {
                            val newCode = (100000..999999).random().toString()
                            save(settings.copy(lockCode = newCode))
                        },
                        enabled  = !isSaving,
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(10.dp)
                    ) {
                        Text("🔄  Generează cod nou", style = MaterialTheme.typography.labelMedium)
                    }
                }

                if (isSaving) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun FamilieTab(
    familyId: String,
    children: List<ChildInfo>,
    onChildrenChanged: (List<ChildInfo>) -> Unit
) {
    val scope = rememberCoroutineScope()

    // ── State ștergere ────────────────────────────────────────────────────────
    var childToDelete by remember { mutableStateOf<ChildInfo?>(null) }
    var isDeleting    by remember { mutableStateOf(false) }

    // ── State adăugare copil ──────────────────────────────────────────────────
    var showAddDialog   by remember { mutableStateOf(false) }
    var addCode         by remember { mutableStateOf("") }
    var isAdding        by remember { mutableStateOf(false) }
    var addError        by remember { mutableStateOf<String?>(null) }
    var failedAttempts  by remember { mutableStateOf(0) }
    var cooldownUntil   by remember { mutableStateOf(0L) }

    // Dialog confirmare ștergere
    childToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { if (!isDeleting) childToDelete = null },
            title   = { Text("Elimini ${target.childName}?") },
            text    = { Text("Toate alertele și datele pentru ${target.childName} vor fi șterse permanent.") },
            confirmButton = {
                TextButton(onClick = {
                    isDeleting = true
                    scope.launch {
                        runCatching {
                            FirebaseRepository.removeChild(familyId, target.childId)
                            onChildrenChanged(children.filter { it.childId != target.childId })
                        }
                        isDeleting = false
                        childToDelete = null
                    }
                }) {
                    if (isDeleting)
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else
                        Text("Elimină", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { childToDelete = null }) { Text("Anulează") }
            }
        )
    }

    // Dialog adăugare copil nou
    if (showAddDialog) {
        val inCooldown = System.currentTimeMillis() < cooldownUntil
        AlertDialog(
            onDismissRequest = { if (!isAdding) { showAddDialog = false; addCode = ""; addError = null } },
            title = { Text("Adaugă copil nou") },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Introdu codul de 6 cifre afișat pe telefonul copilului.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value         = addCode,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) addCode = it },
                        label         = { Text("Cod 6 cifre") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine    = true,
                        enabled       = !isAdding && !inCooldown,
                        textStyle     = LocalTextStyle.current.copy(
                            fontFamily    = FontFamily.Monospace,
                            letterSpacing = 6.sp
                        )
                    )
                    addError?.let { err ->
                        Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }
                    if (inCooldown) {
                        Text(
                            "Prea multe încercări. Așteaptă 60 de secunde.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = addCode.length == 6 && !isAdding && !inCooldown,
                    onClick = {
                        isAdding  = true
                        addError  = null
                        scope.launch {
                            runCatching {
                                val newChild = FirebaseRepository.addChildFromPairingCode(familyId, addCode)
                                onChildrenChanged(children + newChild)
                                showAddDialog = false
                                addCode = ""
                                failedAttempts = 0
                            }.onFailure { e ->
                                failedAttempts++
                                addError = e.message ?: "Cod invalid."
                                if (failedAttempts >= 3) {
                                    cooldownUntil  = System.currentTimeMillis() + 60_000L
                                    failedAttempts = 0
                                }
                            }
                            isAdding = false
                        }
                    }
                ) {
                    if (isAdding) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Conectează", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false; addCode = ""; addError = null }) {
                    Text("Anulează")
                }
            }
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text("Familia mea", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Button(
                onClick        = { showAddDialog = true },
                shape          = RoundedCornerShape(28.dp),
                colors         = ButtonDefaults.buttonColors(containerColor = SfDark, contentColor = SfCream),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("+ Adaugă copil", style = MaterialTheme.typography.labelSmall)
            }
        }

        // ── Control per copil ────────────────────────────────────────────────
        children.forEach { child ->
            ChildControlCard(familyId = familyId, child = child)
        }

        // ── Lista copii ───────────────────────────────────────────────────────
        if (children.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().padding(top = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("👨‍👧", fontSize = 44.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Niciun copil conectat încă.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Apasă '+ Adaugă copil' și introdu codul de pe telefonul copilului.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Copii conectați (${children.size})",
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    children.forEachIndexed { index, child ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(SfDark.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("👤", fontSize = 18.sp)
                                }
                                Column {
                                    Text(
                                        child.childName,
                                        style      = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "Conectat",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StatusGreen
                                    )
                                }
                            }
                            IconButton(onClick = { childToDelete = child }) {
                                Text("🗑️", fontSize = 18.sp)
                            }
                        }
                        if (index < children.size - 1) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.3f))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ── Tab 5: Suport ─────────────────────────────────────────────────────────────

@Composable
private fun SupportTab(familyId: String, childId: String) {
    val scope = rememberCoroutineScope()
    val categories = listOf("Bug", "Sugestie", "Întrebare")
    var selectedCategory by remember { mutableStateOf("Sugestie") }
    var message          by remember { mutableStateOf("") }
    var isSending        by remember { mutableStateOf(false) }
    var result           by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Contactează suportul",
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Ai o problemă sau o sugestie? Trimite-ne un mesaj și te contactăm în cel mai scurt timp.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Categorie",
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick  = { selectedCategory = cat },
                            label    = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SfDark,
                                selectedLabelColor     = Color.White
                            )
                        )
                    }
                }
            }
        }

        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Mesaj",
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value         = message,
                    onValueChange = { message = it },
                    placeholder   = {
                        Text(
                            "Descrie problema sau sugestia ta...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(12.dp),
                    minLines  = 4,
                    maxLines  = 8
                )
            }
        }

        Button(
            onClick = {
                if (message.isBlank()) return@Button
                isSending = true
                result    = null
                scope.launch {
                    runCatching {
                        FirebaseRepository.submitFeedback(familyId, childId, selectedCategory, message)
                        message = ""
                        result  = "Mesaj trimis! Te contactăm în curând."
                    }.onFailure {
                        result = "Eroare la trimitere. Încearcă din nou."
                    }
                    isSending = false
                }
            },
            enabled  = !isSending && message.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = SfDark)
        ) {
            if (isSending)
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
            else
                Text("Trimite mesaj", fontWeight = FontWeight.SemiBold)
        }

        result?.let { msg ->
            Text(
                msg,
                style    = MaterialTheme.typography.bodySmall,
                color    = if (msg.startsWith("Eroare")) MaterialTheme.colorScheme.error else StatusGreen,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ── Card toggleuri funcționalități ───────────────────────────────────────────

@Composable
private fun FeatureTogglesCard(
    settings: FamilySettings,
    onSettingsChange: (FamilySettings) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                            .background(SfDark.copy(0.12f)),
                        contentAlignment = Alignment.Center
                    ) { Text("⚙️", fontSize = 18.sp) }
                    Column {
                        Text(
                            "Funcționalități",
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (expanded) "Apasă pentru a ascunde" else "Monitorizare, rapoarte, alerte",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        if (expanded) "▲" else "▼",
                        style    = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color    = MaterialTheme.colorScheme.outline.copy(0.2f)
                    )
                    Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        ToggleRow("🔍", "Detecție conținut AI",     "Analizează videoclipuri și mesaje",           settings.contentDetectionEnabled)  { onSettingsChange(settings.copy(contentDetectionEnabled = it)) }
                        ToggleRow("💬", "Monitor mesagerie",        "WhatsApp, Telegram, Signal…",                 settings.messagingMonitorEnabled)   { onSettingsChange(settings.copy(messagingMonitorEnabled = it)) }
                        ToggleRow("📱", "Monitor SMS",              "Mesaje text primite și trimise",              settings.smsMonitorEnabled)         { onSettingsChange(settings.copy(smsMonitorEnabled = it)) }
                        ToggleRow("📊", "Rapoarte săptămânale",    "Sumar de activitate trimis automat",           settings.weeklyReportEnabled)       { onSettingsChange(settings.copy(weeklyReportEnabled = it)) }
                        ToggleRow("🔔", "Notificări alerte",       "Primești push notification la alerte critice", settings.alertNotificationsEnabled) { onSettingsChange(settings.copy(alertNotificationsEnabled = it)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(
    emoji: String,
    label: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            modifier              = Modifier.weight(1f)
        ) {
            Text(emoji, fontSize = 20.sp)
            Column {
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(
            checked         = checked,
            onCheckedChange = onToggle,
            colors          = SwitchDefaults.colors(checkedTrackColor = SfDark)
        )
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
                            color      = SfDark
                        )
                    }
                    Slider(
                        value         = settings.burstSizeKb.toFloat(),
                        onValueChange = { onSettingsChange(settings.copy(burstSizeKb = it.roundToInt())) },
                        valueRange    = 16f..256f,
                        steps         = 14,
                        colors        = SliderDefaults.colors(thumbColor = SfDark, activeTrackColor = SfDark)
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
                            color      = SfDark
                        )
                    }
                    Slider(
                        value         = settings.pauseDurationMs / 1000f,
                        onValueChange = { onSettingsChange(settings.copy(pauseDurationMs = (it * 1000).toLong())) },
                        valueRange    = 1f..10f,
                        steps         = 8,
                        colors        = SliderDefaults.colors(thumbColor = SfDark, activeTrackColor = SfDark)
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

// ── Card per aplicație (toggle + slider limită) ──────────────────────────────

@Composable
private fun AppLimitCard(
    label: String,
    emoji: String,
    accentColor: Color,
    enabled: Boolean,
    limitMinutes: Int,
    onToggle: (Boolean) -> Unit,
    onLimitChange: (Int) -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(if (enabled) 3.dp else 1.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (enabled)
                MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        // Accent bar stânga
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(
                        if (enabled) accentColor else accentColor.copy(alpha = 0.25f)
                    )
            )
            Column(Modifier.weight(1f)) {
                // Rând principal: icon + titlu + toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = if (enabled) 4.dp else 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (enabled) accentColor.copy(0.12f)
                                    else MaterialTheme.colorScheme.outline.copy(0.08f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 20.sp)
                        }
                        Column {
                            Text(
                                label,
                                style      = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color      = if (enabled)
                                    MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                when {
                                    !enabled         -> "Dezactivat"
                                    limitMinutes == 0 -> "Fără limită"
                                    else              -> "${limitMinutes} min/zi"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = when {
                                    !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                                    limitMinutes == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
                                    else -> accentColor
                                }
                            )
                        }
                    }
                    Switch(
                        checked         = enabled,
                        onCheckedChange = onToggle,
                        colors          = SwitchDefaults.colors(checkedTrackColor = accentColor)
                    )
                }

                // Slider limită zilnică (animat)
                AnimatedVisibility(
                    visible = enabled,
                    enter   = expandVertically(),
                    exit    = shrinkVertically()
                ) {
                    Column(
                        Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Limită zilnică",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                if (limitMinutes == 0) "Nelimitată" else "${limitMinutes} min",
                                style      = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color      = accentColor
                            )
                        }
                        Slider(
                            value         = limitMinutes.toFloat(),
                            onValueChange = {
                                onLimitChange(((it / 5).roundToInt() * 5).coerceIn(0, 120))
                            },
                            valueRange = 0f..120f,
                            steps      = 23,
                            colors     = SliderDefaults.colors(
                                thumbColor       = accentColor,
                                activeTrackColor = accentColor,
                                inactiveTrackColor = accentColor.copy(0.2f)
                            )
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Nelimitată", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f))
                            Text("2h", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f))
                        }
                    }
                }
            }
        }
    }
}
