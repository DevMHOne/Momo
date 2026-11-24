package com.example.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.ManageSearch
import androidx.compose.material.icons.rounded.MovieFilter
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Sailing
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max

const val MOMO_API_BASE = "https://momo.jev.ai.id"

enum class MomoTab(val route: String) {
    BOT("bot"),
    CHAT("chat"),
    GALERI("galeri"),
    AKUN("akun")
}

data class MomoQuota(
    val chat5hRemainingPct: Int = 100,
    val chat5hResetAt: String? = null,
    val chatWeeklyRemainingPct: Int = 100,
    val chatWeeklyResetAt: String? = null,
    val chatBlockedReason: String = "none",
    val imageRemaining: Int = 0,
    val imageLimit: Int = 0,
    val imageBonusQuota: Int = 0,
    val planName: String = "Gratis",
    val planId: String = "",
    val maxBots: Int = 1,
    val botCount: Int = 0
)

data class MomoUser(
    val id: String,
    val name: String,
    val email: String,
    val role: String = "user",
    val status: String = "active",
    val quota: MomoQuota = MomoQuota()
)

data class MomoBot(
    val id: String,
    val name: String,
    val look: String,
    val job: String,
    val createdAt: String? = null
) {
    val isImageStudio: Boolean get() = look.equals("imagine", ignoreCase = true)
}

data class BotLookPreset(
    val id: String,
    val label: String,
    val defaultJob: String,
    val icon: ImageVector,
    val accentColor: Color
)

object BotLookCatalog {
    val all: List<BotLookPreset> = listOf(
        BotLookPreset("imagine", "Pelukis / Gambar AI", "Studio Pelukis gambar AI", Icons.Rounded.Palette, Color(0xFFEF8C66)),
        BotLookPreset("rest", "Momo", "Asisten serba bisa & ramah", Icons.Rounded.AutoAwesome, Color(0xFFF3A683)),
        BotLookPreset("director", "Kreator", "Ide konten, naskah & arah kreatif", Icons.Rounded.MovieFilter, Color(0xFFE17055)),
        BotLookPreset("builder", "Maker", "Arsitektur kode & solusi teknis", Icons.Rounded.Build, Color(0xFF60A5FA)),
        BotLookPreset("captain", "Kapten", "Koordinasi proyek & kepemimpinan", Icons.Rounded.Sailing, Color(0xFF38BDF8)),
        BotLookPreset("tester", "Analisis", "Analisis data & riset terstruktur", Icons.Rounded.QueryStats, Color(0xFF818CF8)),
        BotLookPreset("bug", "Tester", "QA, debugging & deteksi celah", Icons.Rounded.BugReport, Color(0xFFF87171)),
        BotLookPreset("guard", "Protektor", "Keamanan sistem & audit risiko", Icons.Rounded.Shield, Color(0xFF34D399)),
        BotLookPreset("investigate", "Penyelidik", "Investigasi mendalam & verifikasi", Icons.Rounded.ManageSearch, Color(0xFFA78BFA)),
        BotLookPreset("designer", "Desainer", "UI/UX, tipografi & estetika visual", Icons.Rounded.Draw, Color(0xFFF472B6)),
        BotLookPreset("cook", "Chef", "Resep kuliner & panduan memasak", Icons.Rounded.Restaurant, Color(0xFFFB923C)),
        BotLookPreset("write", "Sastrawan", "Penulisan kreatif, esai & penyuntingan", Icons.Rounded.EditNote, Color(0xFFFBBF24)),
        BotLookPreset("tutor", "Mentor", "Belajar bertahap & penjelasan jernih", Icons.Rounded.School, Color(0xFF2DD4BF)),
        BotLookPreset("music", "Maestro", "Komposisi, lirik & teori musik", Icons.Rounded.MusicNote, Color(0xFFC084FC)),
        BotLookPreset("coach", "Pelatih", "Kebugaran, kebiasaan & motivasi", Icons.Rounded.FitnessCenter, Color(0xFF4ADE80)),
        BotLookPreset("planner", "Strategis", "Perencanaan jadwal & strategi bisnis", Icons.Rounded.Timeline, Color(0xFF67E8F9)),
        BotLookPreset("traveller", "Penjelajah", "Rencana perjalanan & panduan wisata", Icons.Rounded.Explore, Color(0xFFFCA5A5)),
        BotLookPreset("money", "Finansial", "Anggaran, investasi & arus kas", Icons.Rounded.AccountBalanceWallet, Color(0xFF86EFAC)),
        BotLookPreset("garden", "Botanis", "Perawatan tanaman & ekologi rumah", Icons.Rounded.LocalFlorist, Color(0xFFA3E635))
    )

    fun byId(look: String): BotLookPreset {
        return all.firstOrNull { it.id.equals(look, ignoreCase = true) }
            ?: BotLookPreset(look, look.replaceFirstChar { it.uppercase() }, "Asisten AI", Icons.Rounded.Brush, Color(0xFFEF8C66))
    }
}

data class EffortCosts(
    val low: Double = 1.0,
    val medium: Double = 1.5,
    val high: Double = 2.5
)

data class MomoModel(
    val id: String,
    val label: String,
    val description: String = "",
    val brand: String = "",
    val kind: String = "chat",
    val supportsImageEdit: Boolean = false,
    val creditCost: Int = 1,
    val effortCosts: EffortCosts = EffortCosts()
) {
    val isImage: Boolean get() = kind.equals("image", ignoreCase = true)
    val isChat: Boolean get() = !isImage
}

enum class ReasoningEffort(val apiValue: String, val label: String, val panelTitle: String) {
    LOW("low", "Low", "Penalaran ringkas"),
    MEDIUM("medium", "Medium", "Penalaran terukur"),
    HIGH("high", "High", "Penalaran mendalam");

    companion object {
        fun fromApi(value: String?): ReasoningEffort =
            entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) } ?: MEDIUM
    }
}

enum class ImageSizeOption(val apiValue: String, val shortLabel: String, val fullLabel: String) {
    SQUARE("1024x1024", "1:1", "1:1 Persegi"),
    LANDSCAPE("1536x1024", "Lanskap", "Lanskap (1536×1024)"),
    PORTRAIT("1024x1536", "Potret", "Potret (1024×1536)");

    companion object {
        fun fromApi(value: String?): ImageSizeOption =
            entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) } ?: SQUARE
    }
}

enum class AttachmentKind { PHOTO, NOTE, FILE }

data class ChatAttachment(
    val id: String,
    val kind: AttachmentKind,
    val name: String,
    val mimeType: String,
    val dataUrl: String? = null,
    val textContent: String? = null,
    val previewUri: String? = null
)

data class ChatMessage(
    val id: String,
    val role: String, // "user" or "assistant"
    val text: String,
    val reasoning: String = "",
    val reasoningEffort: ReasoningEffort = ReasoningEffort.MEDIUM,
    val activityLabel: String? = null,
    val imageUrls: List<String> = emptyList(),
    val attachmentLabels: List<String> = emptyList(),
    val isStreaming: Boolean = false,
    val isError: Boolean = false,
    val timestampMs: Long = System.currentTimeMillis()
)

data class GeneratedImageItem(
    val id: String,
    val url: String,
    val prompt: String = "",
    val model: String = "",
    val size: String = "1024x1024",
    val createdAt: String? = null
)

data class MomoPlan(
    val id: String,
    val name: String,
    val description: String = "",
    val priceIdr: Long = 0L,
    val billingCycle: String = "bulan",
    val imageQuota: Int = 0,
    val maxBots: Int = 1,
    val badge: String? = null,
    val features: List<String> = emptyList(),
    val isCurrent: Boolean = false
)

data class QrisCheckoutSession(
    val orderId: String,
    val planId: String,
    val planName: String,
    val amountIdr: Long,
    val qrImageUrl: String? = null,
    val qrDataUrl: String? = null,
    val qrString: String? = null,
    val expiresAt: String? = null,
    val status: String = "pending"
)

data class RateLimitInfo(
    val message: String,
    val resetAt: String? = null,
    val reason: String? = null
)

sealed interface SseEvent {
    data class TurnMeta(val rawJson: String) : SseEvent
    data class ActivityUpdate(val label: String) : SseEvent
    data class ReasoningDelta(val delta: String) : SseEvent
    data class PartStart(val partType: String, val imageUrl: String? = null) : SseEvent
    data class TextDelta(val delta: String) : SseEvent
    data class PartEnd(val partType: String, val imageUrl: String? = null) : SseEvent
    data class SessionTitle(val title: String) : SseEvent
    data object TurnDone : SseEvent
    data class TurnError(val message: String, val code: String? = null) : SseEvent
}

class UnauthorizedException(message: String = "Sesi berakhir. Silakan masuk kembali.") : Exception(message)
class RateLimitedException(
    message: String,
    val resetAt: String? = null,
    val reason: String? = null
) : Exception(message)

object QuotaFormatters {
    private val rawTokenRegex = Regex(
        pattern = """(?i)(\b\d+([.,]\d+)?\s*[kmb]\b(\s*(token|tokens|ctx|context))?|\b\d{1,3}([.,]\d{3})+\s*(token|tokens)\b)""",
    )

    fun resolveMomoUrl(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return trimmed
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("data:")) {
            return trimmed
        }
        return if (trimmed.startsWith("/")) {
            "$MOMO_API_BASE$trimmed"
        } else {
            "$MOMO_API_BASE/$trimmed"
        }
    }

    fun appendDownloadParam(url: String): String {
        val resolved = resolveMomoUrl(url)
        if (resolved.contains("download=1")) return resolved
        val sep = if (resolved.contains("?")) "&" else "?"
        return "$resolved${sep}download=1"
    }

    fun sanitizeNoRawTokens(input: String): String {
        if (input.isBlank()) return ""
        val cleaned = rawTokenRegex.replace(input, "").replace(Regex("""\s{2,}"""), " ").trim()
        return cleaned.trimEnd('·', '-', ',', '/')
    }

    fun containsRawTokenNumber(input: String): Boolean {
        return rawTokenRegex.containsMatchIn(input)
    }

    fun formatMultiplier(value: Double): String {
        val rounded = if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            String.format(Locale.forLanguageTag("id-ID"), "%.1f", value)
        }
        return "${rounded}x"
    }

    fun formatEffortLegend(costs: EffortCosts): String {
        return "Low: ${formatMultiplier(costs.low)} · Med: ${formatMultiplier(costs.medium)} · High: ${formatMultiplier(costs.high)} token"
    }

    fun formatIdr(amount: Long): String {
        if (amount <= 0L) return "Gratis"
        val nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID"))
        return "Rp ${nf.format(amount)}"
    }

    fun parseEpochMillis(isoOrEpoch: String?): Long? {
        if (isoOrEpoch.isNullOrBlank()) return null
        val trimmed = isoOrEpoch.trim()
        trimmed.toLongOrNull()?.let { num ->
            return if (num < 1_000_000_000_000L) num * 1000L else num
        }
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd HH:mm:ss"
        )
        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val parsed = sdf.parse(trimmed)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {
                // Try next ISO pattern
            }
        }
        return null
    }

    fun formatCountdown(resetAt: String?, prefix: String = "Reset dalam", nowMs: Long = System.currentTimeMillis()): String {
        val targetMs = parseEpochMillis(resetAt) ?: return "Siap dipakai"
        val diffMs = targetMs - nowMs
        if (diffMs <= 0L) return "Segera reset"
        val totalMinutes = max(1L, diffMs / 60_000L)
        val days = totalMinutes / (60L * 24L)
        val hours = (totalMinutes % (60L * 24L)) / 60L
        val minutes = totalMinutes % 60L
        return when {
            days > 0L -> "$prefix ${days}h ${hours}j"
            hours > 0L -> "$prefix ${hours}j ${minutes}m"
            else -> "$prefix ${minutes}m"
        }
    }
}
