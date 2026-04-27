@file:OptIn(ExperimentalLayoutApi::class)

package com.sol.dopaminetrap.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sol.dopaminetrap.data.WellbeingProfile

@Composable
fun WellbeingProfileCard(profile: WellbeingProfile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            // ── Header gradient ───────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF6C63FF), Color(0xFF3F8EFC))
                        ),
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Column {
                    Text(
                        "Profilul de bunăstare",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        profile.weekLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
                Text(
                    "🧠",
                    fontSize = 32.sp,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }

            // ── Dimensiuni ────────────────────────────────────────────────────
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                WellbeingDimensionRow(
                    emoji = "💭",
                    label = "Stare emoțională",
                    score = profile.emotionalScore
                )
                WellbeingDimensionRow(
                    emoji = "👥",
                    label = "Conexiune socială",
                    score = profile.socialScore
                )
                WellbeingDimensionRow(
                    emoji = "🪞",
                    label = "Imagine de sine",
                    score = profile.selfImageScore
                )
                WellbeingDimensionRow(
                    emoji = "📚",
                    label = "Stres academic",
                    score = profile.academicScore
                )
            }

            // ── Risk flags ────────────────────────────────────────────────────
            if (profile.riskFlags.isNotEmpty()) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "⚠️  Riscuri detectate săptămâna aceasta",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        profile.riskFlags.forEach { flag ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    flag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // ── Footer — total events ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    "${profile.totalEvents} semnale analizate",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WellbeingDimensionRow(emoji: String, label: String, score: Float) {
    val barColor = when {
        score >= 0.65f -> Color(0xFF4CAF50)
        score >= 0.35f -> Color(0xFFFFC107)
        else           -> Color(0xFFF44336)
    }
    val statusText = when {
        score >= 0.65f -> "Bine"
        score >= 0.35f -> "Atenție"
        else           -> "Îngrijorător"
    }

    var started by remember { mutableStateOf(false) }
    val animatedScore by animateFloatAsState(
        targetValue = if (started) score else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "wellbeing_bar"
    )
    LaunchedEffect(score) { started = true }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(emoji, fontSize = 20.sp)
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = barColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(5.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedScore)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(barColor)
                )
            }
        }
    }
}
