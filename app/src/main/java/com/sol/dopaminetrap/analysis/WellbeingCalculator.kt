package com.sol.dopaminetrap.analysis

import com.sol.dopaminetrap.data.ContentCategory
import com.sol.dopaminetrap.data.ContentEvent
import com.sol.dopaminetrap.data.WellbeingProfile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WellbeingCalculator {

    // ── Mapare categorii → dimensiuni ─────────────────────────────────────────

    private val EMOTIONAL_NEGATIVE = setOf(
        ContentCategory.TRISTETE_MELANCOLIE,
        ContentCategory.ANXIETATE_FRICA,
        ContentCategory.FURIE_FRUSTRARE,
        ContentCategory.SINGURATATE
    )
    private val EMOTIONAL_POSITIVE = setOf(
        ContentCategory.FERICIRE_POZITIV
    )

    private val SOCIAL_NEGATIVE = setOf(
        ContentCategory.IZOLARE_SOCIALA,
        ContentCategory.BULLYING_VICTIMA,
        ContentCategory.PRESIUNE_GRUP
    )
    private val SOCIAL_POSITIVE = setOf(
        ContentCategory.SOCIAL_PRIETENII,
        ContentCategory.RELATII_ROMANTICE
    )

    private val SELF_IMAGE_NEGATIVE = setOf(
        ContentCategory.STIMA_SINE_SCAZUTA,
        ContentCategory.BODY_IMAGE_NEGATIV,
        ContentCategory.TULBURARI_ALIMENTARE
    )

    private val ACADEMIC_NEGATIVE = setOf(
        ContentCategory.STRES_SCOLAR
    )

    private val RISK_CATEGORIES = setOf(
        ContentCategory.CONTINUT_SUICIDAR,
        ContentCategory.AUTOMUTILARE,
        ContentCategory.RISC_GROOMING,
        ContentCategory.ALCOOL_DROGURI,
        ContentCategory.VIOLENTA_EXTREMA,
        ContentCategory.CONTINUT_ADULT
    )

    // Surse de mesagerie — evenimentele de acolo contează 1.5x (conversații reale)
    private val MESSAGING_SOURCES = setOf(
        "SMS", "WhatsApp", "WhatsApp Business",
        "Telegram", "Messenger", "Signal", "Viber", "Snapchat", "Instagram DM"
    )

    // ── API public ────────────────────────────────────────────────────────────

    fun calculate(events: List<ContentEvent>, weekLabel: String? = null): WellbeingProfile {
        data class CatCount(var negW: Float = 0f, var posW: Float = 0f)

        val emotional   = CatCount()
        val social      = CatCount()
        val selfImage   = CatCount()
        val academic    = CatCount()
        val riskSet     = mutableSetOf<String>()

        events.forEach { event ->
            val weight = if (event.sourceApp in MESSAGING_SOURCES) 1.5f else 1f
            val categories = event.categories.split("|")
                .mapNotNull { name -> runCatching { ContentCategory.valueOf(name) }.getOrNull() }

            categories.forEach { cat ->
                when {
                    cat in EMOTIONAL_NEGATIVE -> emotional.negW  += weight
                    cat in EMOTIONAL_POSITIVE -> emotional.posW  += weight
                    cat in SOCIAL_NEGATIVE    -> social.negW     += weight
                    cat in SOCIAL_POSITIVE    -> social.posW     += weight
                    cat in SELF_IMAGE_NEGATIVE-> selfImage.negW  += weight
                    cat in ACADEMIC_NEGATIVE  -> academic.negW   += weight
                }
                if (cat in RISK_CATEGORIES) riskSet.add(cat.displayName)
            }
        }

        val label = weekLabel ?: buildWeekLabel()

        return WellbeingProfile(
            emotionalScore  = scoreBalanced(emotional.negW, emotional.posW),
            socialScore     = scoreBalanced(social.negW, social.posW),
            selfImageScore  = scoreNegative(selfImage.negW),
            academicScore   = scoreNegative(academic.negW),
            riskFlags       = riskSet.toList(),
            totalEvents     = events.size,
            weekLabel       = label
        )
    }

    // ── Conversie la Map<String,Any> pentru Firestore ─────────────────────────

    fun toMap(profile: WellbeingProfile): Map<String, Any> = mapOf(
        "emotionalScore"  to profile.emotionalScore,
        "socialScore"     to profile.socialScore,
        "selfImageScore"  to profile.selfImageScore,
        "academicScore"   to profile.academicScore,
        "riskFlags"       to profile.riskFlags,
        "totalEvents"     to profile.totalEvents,
        "weekLabel"       to profile.weekLabel,
        "timestamp"       to System.currentTimeMillis()
    )

    fun fromMap(map: Map<String, Any>): WellbeingProfile = WellbeingProfile(
        emotionalScore  = (map["emotionalScore"]  as? Double)?.toFloat() ?: 0.6f,
        socialScore     = (map["socialScore"]     as? Double)?.toFloat() ?: 0.6f,
        selfImageScore  = (map["selfImageScore"]  as? Double)?.toFloat() ?: 0.6f,
        academicScore   = (map["academicScore"]   as? Double)?.toFloat() ?: 0.9f,
        riskFlags       = (map["riskFlags"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
        totalEvents     = (map["totalEvents"] as? Long)?.toInt() ?: 0,
        weekLabel       = map["weekLabel"] as? String ?: ""
    )

    // ── Scoring ───────────────────────────────────────────────────────────────

    private fun scoreBalanced(negative: Float, positive: Float): Float {
        val base    = 0.60f
        val penalty = minOf(negative * 0.08f, 0.55f)
        val bonus   = minOf(positive * 0.06f, 0.35f)
        return (base - penalty + bonus).coerceIn(0f, 1f)
    }

    private fun scoreNegative(negative: Float): Float = when {
        negative == 0f  -> 0.90f
        negative < 2f   -> 0.70f
        negative < 5f   -> 0.45f
        else            -> 0.15f
    }

    private fun buildWeekLabel(): String {
        val sdf = SimpleDateFormat("d MMM", Locale("ro"))
        val now = System.currentTimeMillis()
        val weekAgo = now - 7 * 24 * 60 * 60 * 1000L
        return "${sdf.format(Date(weekAgo))} – ${sdf.format(Date(now))}"
    }
}
