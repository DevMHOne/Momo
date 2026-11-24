package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import com.example.data.BotLookCatalog
import com.example.data.MomoQuota
import com.example.data.QuotaFormatters
import com.example.data.RateLimitInfo
import com.example.data.ReasoningEffort
import com.example.ui.theme.LocalMomoExtraColors

@Composable
fun BotLookAvatar(
    look: String,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val preset = remember(look) { BotLookCatalog.byId(look) }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.32f))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        preset.accentColor.copy(alpha = 0.24f),
                        preset.accentColor.copy(alpha = 0.10f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = preset.accentColor.copy(alpha = 0.42f),
                shape = RoundedCornerShape(size * 0.32f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = preset.icon,
            contentDescription = preset.label,
            tint = preset.accentColor,
            modifier = Modifier.size(size * 0.52f)
        )
    }
}

@Composable
fun LockedLookBadge(
    look: String,
    showLockIcon: Boolean = false,
    modifier: Modifier = Modifier
) {
    val preset = remember(look) { BotLookCatalog.byId(look) }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = preset.accentColor.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, preset.accentColor.copy(alpha = 0.32f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (showLockIcon) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = "Tipe terkunci",
                    tint = preset.accentColor,
                    modifier = Modifier.size(11.dp)
                )
            }
            Text(
                text = preset.label,
                style = MaterialTheme.typography.labelSmall,
                color = preset.accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Quota display strictly following Momo AI rules:
 * - ZERO raw token numbers (no 40K, 200K, 1M, 2.5M, etc.)
 * - 5-hour session & Weekly show ONLY percentage + progress bar + reset countdown
 * - Image quota shows remaining/limit (+ optional bonus)
 */
@Composable
fun QuotaOverviewCard(
    quota: MomoQuota,
    nowMs: Long,
    compact: Boolean = false,
    onPlanClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val extra = LocalMomoExtraColors.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extra.cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 14.dp else 18.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (onPlanClick != null) {
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(onClick = onPlanClick)
                                .padding(vertical = 4.dp)
                        } else {
                            Modifier
                        }
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Paket ${quota.planName}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "Bot ${quota.botCount}/${quota.maxBots}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            QuotaPercentageBar(
                title = "Sesi 5 Jam",
                remainingPct = quota.chat5hRemainingPct,
                countdownText = QuotaFormatters.formatCountdown(quota.chat5hResetAt, "Reset dalam", nowMs),
                isBlocked = quota.chatBlockedReason == "window_5h"
            )

            QuotaPercentageBar(
                title = "Mingguan (Akumulatif)",
                remainingPct = quota.chatWeeklyRemainingPct,
                countdownText = QuotaFormatters.formatCountdown(quota.chatWeeklyResetAt, "Full reset", nowMs),
                isBlocked = quota.chatBlockedReason == "weekly"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = "Saldo Kuota Gambar",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (quota.imageBonusQuota > 0) {
                                "Tidak reset · Termasuk +${quota.imageBonusQuota} bonus"
                            } else {
                                "Tidak reset berkala"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = "${quota.imageRemaining}/${quota.imageLimit}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun QuotaPercentageBar(
    title: String,
    remainingPct: Int,
    countdownText: String,
    isBlocked: Boolean
) {
    val extra = LocalMomoExtraColors.current
    val clamped = remainingPct.coerceIn(0, 100)
    val animatedProgress by animateFloatAsState(
        targetValue = clamped / 100f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "quota_bar"
    )
    val barColor = when {
        isBlocked || clamped <= 10 -> MaterialTheme.colorScheme.error
        clamped <= 30 -> extra.warning
        else -> MaterialTheme.colorScheme.primary
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$clamped%",
                style = MaterialTheme.typography.labelLarge,
                color = barColor
            )
        }
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(999.dp)),
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = if (isBlocked) "Batas sementara · $countdownText" else countdownText,
                style = MaterialTheme.typography.labelSmall,
                color = if (isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RateLimitAlertCard(
    info: RateLimitInfo,
    nowMs: Long,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val countdown = QuotaFormatters.formatCountdown(info.resetAt, "Reset dalam", nowMs)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Batas Kuota Tercapai",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Text(
                text = info.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            if (!info.resetAt.isNullOrBlank()) {
                Text(
                    text = countdown,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Button(
                onClick = onUpgradeClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("rate_limit_upgrade_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Lihat Paket")
            }
        }
    }
}

@Composable
fun ReasoningCollapsiblePanel(
    reasoning: String,
    effort: ReasoningEffort,
    isStreaming: Boolean,
    modifier: Modifier = Modifier
) {
    val extra = LocalMomoExtraColors.current
    var expanded by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, extra.cardBorder)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .minimumInteractiveComponentSize()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("reasoning_toggle"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = if (isStreaming) "${effort.panelTitle}…" else effort.panelTitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandMore else Icons.Rounded.ChevronRight,
                    contentDescription = if (expanded) "Lipat penalaran" else "Buka penalaran",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(10.dp)
                ) {
                    Text(
                        text = reasoning.trim(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private sealed interface MarkdownBlock {
    data class Paragraph(val text: String) : MarkdownBlock
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock
    data class ImageBlock(val alt: String, val url: String) : MarkdownBlock
    data class BulletItem(val prefix: String, val text: String) : MarkdownBlock
}

private val markdownImageRegex = Regex("""!\[([^]]*)]\(([^)\s]+)\)""")

private fun parseMarkdownBlocks(raw: String): List<MarkdownBlock> {
    if (raw.isBlank()) return emptyList()
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = raw.lines()
    var i = 0
    val paragraphBuffer = StringBuilder()

    fun flushParagraph() {
        val text = paragraphBuffer.toString().trim()
        paragraphBuffer.setLength(0)
        if (text.isNotEmpty()) {
            blocks.add(MarkdownBlock.Paragraph(text))
        }
    }

    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()

        if (trimmed.startsWith("```")) {
            flushParagraph()
            val lang = trimmed.removePrefix("```").trim()
            val codeSb = StringBuilder()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                if (codeSb.isNotEmpty()) codeSb.append('\n')
                codeSb.append(lines[i])
                i++
            }
            blocks.add(MarkdownBlock.CodeBlock(lang, codeSb.toString()))
            i++
            continue
        }

        val imgMatch = markdownImageRegex.find(trimmed)
        if (imgMatch != null && imgMatch.value == trimmed) {
            flushParagraph()
            val alt = imgMatch.groupValues[1]
            val url = QuotaFormatters.resolveMomoUrl(imgMatch.groupValues[2])
            blocks.add(MarkdownBlock.ImageBlock(alt, url))
            i++
            continue
        }

        when {
            trimmed.startsWith("### ") -> {
                flushParagraph()
                blocks.add(MarkdownBlock.Heading(3, trimmed.removePrefix("### ").trim()))
            }
            trimmed.startsWith("## ") -> {
                flushParagraph()
                blocks.add(MarkdownBlock.Heading(2, trimmed.removePrefix("## ").trim()))
            }
            trimmed.startsWith("# ") -> {
                flushParagraph()
                blocks.add(MarkdownBlock.Heading(1, trimmed.removePrefix("# ").trim()))
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                flushParagraph()
                blocks.add(MarkdownBlock.BulletItem("•", trimmed.drop(2).trim()))
            }
            Regex("""^\d+\.\s+.*""").matches(trimmed) -> {
                flushParagraph()
                val idx = trimmed.indexOf('.')
                val numPrefix = trimmed.substring(0, idx + 1)
                val body = trimmed.substring(idx + 1).trim()
                blocks.add(MarkdownBlock.BulletItem(numPrefix, body))
            }
            trimmed.isEmpty() -> {
                flushParagraph()
            }
            else -> {
                if (markdownImageRegex.containsMatchIn(line)) {
                    flushParagraph()
                    var cursor = 0
                    markdownImageRegex.findAll(line).forEach { m ->
                        val before = line.substring(cursor, m.range.first).trim()
                        if (before.isNotEmpty()) blocks.add(MarkdownBlock.Paragraph(before))
                        blocks.add(
                            MarkdownBlock.ImageBlock(
                                alt = m.groupValues[1],
                                url = QuotaFormatters.resolveMomoUrl(m.groupValues[2])
                            )
                        )
                        cursor = m.range.last + 1
                    }
                    val after = line.substring(cursor).trim()
                    if (after.isNotEmpty()) blocks.add(MarkdownBlock.Paragraph(after))
                } else {
                    if (paragraphBuffer.isNotEmpty()) paragraphBuffer.append('\n')
                    paragraphBuffer.append(line)
                }
            }
        }
        i++
    }
    flushParagraph()
    return blocks
}

@Composable
fun MomoMarkdownContent(
    markdown: String,
    imageLoader: ImageLoader,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onImageClick: (String) -> Unit,
    onDownloadImage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }
    val codeBg = MaterialTheme.colorScheme.surfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val extra = LocalMomoExtraColors.current
    val context = LocalContext.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> {
                    val style = when (block.level) {
                        1 -> MaterialTheme.typography.titleLarge
                        2 -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.titleSmall
                    }
                    Text(
                        text = buildInlineMarkdown(block.text, codeBg, primaryColor),
                        style = style,
                        color = textColor
                    )
                }
                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = buildInlineMarkdown(block.text, codeBg, primaryColor),
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor
                    )
                }
                is MarkdownBlock.BulletItem -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = block.prefix,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = primaryColor
                        )
                        Text(
                            text = buildInlineMarkdown(block.text, codeBg, primaryColor),
                            style = MaterialTheme.typography.bodyLarge,
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                is MarkdownBlock.CodeBlock -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = extra.elevatedSurface,
                        border = BorderStroke(1.dp, extra.cardBorder)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = block.language.ifBlank { "kode" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            cm.setPrimaryClip(ClipData.newPlainText("Momo Code", block.code))
                                            Toast.makeText(context, "Kode disalin", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ContentCopy,
                                        contentDescription = "Salin kode",
                                        tint = primaryColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Salin",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = primaryColor
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = block.code,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 19.sp
                                    ),
                                    color = textColor
                                )
                            }
                        }
                    }
                }
                is MarkdownBlock.ImageBlock -> {
                    GeneratedInlineImageCard(
                        imageUrl = block.url,
                        alt = block.alt,
                        imageLoader = imageLoader,
                        onPreview = { onImageClick(block.url) },
                        onDownload = { onDownloadImage(block.url) }
                    )
                }
            }
        }
    }
}

@Composable
fun GeneratedInlineImageCard(
    imageUrl: String,
    alt: String,
    imageLoader: ImageLoader,
    onPreview: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extra = LocalMomoExtraColors.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onPreview),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, extra.cardBorder)
    ) {
        Box {
            AsyncImage(
                model = imageUrl,
                imageLoader = imageLoader,
                contentDescription = alt.ifBlank { "Gambar hasil Momo AI" },
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 340.dp)
                    .aspectRatio(1f, matchHeightConstraintsFirst = false)
            )
            Surface(
                onClick = onDownload,
                shape = CircleShape,
                color = Color(0xCC0C0D11),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .minimumInteractiveComponentSize()
            ) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = "Unduh gambar ke HP Android",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun buildInlineMarkdown(
    text: String,
    codeBg: Color,
    accentColor: Color
) = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end > i + 2) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                } else {
                    append(text[i])
                    i++
                }
            }
            text.startsWith("`", i) -> {
                val end = text.indexOf('`', i + 1)
                if (end > i + 1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = codeBg,
                            fontWeight = FontWeight.Medium
                        )
                    ) {
                        append(" ${text.substring(i + 1, end)} ")
                    }
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }
            text.startsWith("*", i) && (i == 0 || text[i - 1] != '*') -> {
                val end = text.indexOf('*', i + 1)
                if (end > i + 1 && !text.startsWith("**", i)) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }
            text.startsWith("[", i) -> {
                val closeBracket = text.indexOf(']', i + 1)
                val openParen = if (closeBracket > i) closeBracket + 1 else -1
                if (openParen < text.length && openParen > 0 && text[openParen] == '(') {
                    val closeParen = text.indexOf(')', openParen + 1)
                    if (closeParen > openParen) {
                        val label = text.substring(i + 1, closeBracket)
                        withStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Medium)) {
                            append(label)
                        }
                        i = closeParen + 1
                        continue
                    }
                }
                append(text[i])
                i++
            }
            else -> {
                append(text[i])
                i++
            }
        }
    }
}
