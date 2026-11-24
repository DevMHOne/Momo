package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.ImageLoader
import coil.compose.AsyncImage
import com.example.R
import com.example.data.AndroidMediaHelper
import com.example.data.MomoPlan
import com.example.data.QrisCheckoutSession
import com.example.data.QuotaFormatters
import com.example.ui.components.QuotaOverviewCard
import com.example.ui.theme.LocalMomoExtraColors
import com.example.viewmodel.MomoUiState

@Composable
fun AccountPlansScreen(
    uiState: MomoUiState,
    imageLoader: ImageLoader,
    onRefresh: () -> Unit,
    onToggleTheme: () -> Unit,
    onUpgradeQris: (MomoPlan) -> Unit,
    onDowngradeFree: (MomoPlan) -> Unit,
    onSaveQrImage: (QrisCheckoutSession) -> Unit,
    onCloseQris: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extra = LocalMomoExtraColors.current
    var showPlansPopup by remember { mutableStateOf(false) }
    var downgradeTarget by remember { mutableStateOf<MomoPlan?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.statusBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.account_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("account_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.cd_refresh),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider(color = extra.cardBorder, thickness = 1.dp)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            uiState.user?.let { user ->
                // Profile & Theme Card
                item(key = "profile_card") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, extra.cardBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = user.name.trim().firstOrNull()?.uppercase() ?: "M",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = user.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = user.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Surface(
                                    onClick = { showPlansPopup = true },
                                    shape = RoundedCornerShape(999.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("profile_plan_popup_pill")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = user.quota.planName,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Icon(
                                            imageVector = Icons.Rounded.ChevronRight,
                                            contentDescription = "Lihat pilihan paket",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = extra.cardBorder, thickness = 1.dp)

                            // Theme Toggle Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.theme_label),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (uiState.isDarkTheme) {
                                            stringResource(R.string.theme_dark)
                                        } else {
                                            stringResource(R.string.theme_light)
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OutlinedButton(
                                    onClick = onToggleTheme,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, extra.cardBorder),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("account_theme_toggle")
                                ) {
                                    Icon(
                                        imageVector = if (uiState.isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (uiState.isDarkTheme) "Mode Terang" else "Mode Gelap")
                                }
                            }
                        }
                    }
                }

                // Quota Detail Card (ZERO RAW TOKEN NUMBERS)
                item(key = "quota_full") {
                    QuotaOverviewCard(
                        quota = user.quota,
                        nowMs = uiState.nowMs,
                        compact = false,
                        onPlanClick = { showPlansPopup = true }
                    )
                }

                // Paket Langganan Launcher Card (Pop-up trigger button — NO inline plans list)
                item(key = "plans_launcher_card") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, extra.cardBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.WorkspacePremium,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.plans_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Paket aktif: ${user.quota.planName} · Upgrade via QRIS atau kelola paket",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Button(
                                onClick = { showPlansPopup = true },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("open_plans_popup_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Lihat & Pilih Paket", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }

            // Logout Action
            item(key = "logout_btn") {
                OutlinedButton(
                    onClick = onLogout,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("logout_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.auth_logout), style = MaterialTheme.typography.labelLarge)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (showPlansPopup) {
        PlansPopupDialog(
            plans = uiState.plans,
            currentPlanId = uiState.user?.quota?.planId.orEmpty(),
            currentPlanName = uiState.user?.quota?.planName.orEmpty(),
            isLoading = uiState.isPlansLoading,
            isBusy = uiState.isPlanActionBusy,
            onRefresh = onRefresh,
            onUpgradeQris = { plan ->
                showPlansPopup = false
                onUpgradeQris(plan)
            },
            onRequestDowngrade = { plan ->
                showPlansPopup = false
                downgradeTarget = plan
            },
            onDismiss = { showPlansPopup = false }
        )
    }

    downgradeTarget?.let { plan ->
        AlertDialog(
            onDismissRequest = { downgradeTarget = null },
            title = { Text("Ubah ke ${plan.name}?") },
            text = { Text("Paket Anda akan beralih ke ${plan.name} tanpa pembayaran QRIS.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDowngradeFree(plan)
                        downgradeTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Ya, Aktifkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { downgradeTarget = null }) {
                    Text("Batal")
                }
            }
        )
    }

    uiState.activeQrisSession?.let { session ->
        QrisCheckoutDialog(
            session = session,
            isPolling = uiState.isPollingQris,
            isDownloading = uiState.isDownloadingImage,
            imageLoader = imageLoader,
            onSaveQr = { onSaveQrImage(session) },
            onDismiss = onCloseQris
        )
    }
}

@Composable
private fun PlansPopupDialog(
    plans: List<MomoPlan>,
    currentPlanId: String,
    currentPlanName: String,
    isLoading: Boolean,
    isBusy: Boolean,
    onRefresh: () -> Unit,
    onUpgradeQris: (MomoPlan) -> Unit,
    onRequestDowngrade: (MomoPlan) -> Unit,
    onDismiss: () -> Unit
) {
    val extra = LocalMomoExtraColors.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xC2000000))
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 680.dp)
                    .navigationBarsPadding()
                    .testTag("plans_popup_dialog"),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, extra.cardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.plans_title),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Pilih paket yang sesuai lalu tekan tombol di bawah kartu",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("plans_popup_refresh")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = stringResource(R.string.cd_refresh),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("plans_popup_close")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.cd_close),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    HorizontalDivider(color = extra.cardBorder, thickness = 1.dp)

                    when {
                        isLoading && plans.isEmpty() -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        plans.isEmpty() -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Daftar paket belum tersedia. Ketuk tombol muat ulang di kanan atas.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(plans, key = { it.id }) { plan ->
                                    val isCurrent = plan.isCurrent ||
                                        (currentPlanId.isNotBlank() && plan.id.equals(currentPlanId, ignoreCase = true)) ||
                                        (currentPlanName.isNotBlank() && plan.name.equals(currentPlanName, ignoreCase = true))
                                    PopupPlanCardItem(
                                        plan = plan,
                                        isCurrent = isCurrent,
                                        isBusy = isBusy,
                                        onUpgradeQris = { onUpgradeQris(plan) },
                                        onDowngradeFree = { onRequestDowngrade(plan) }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, extra.cardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("plans_popup_dismiss_button")
                    ) {
                        Text("Tutup")
                    }
                }
            }
        }
    }
}

@Composable
private fun PopupPlanCardItem(
    plan: MomoPlan,
    isCurrent: Boolean,
    isBusy: Boolean,
    onUpgradeQris: () -> Unit,
    onDowngradeFree: () -> Unit
) {
    val extra = LocalMomoExtraColors.current
    val isPaid = plan.priceIdr > 0L

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("plan_card_${plan.id}"),
        shape = RoundedCornerShape(20.dp),
        color = if (isCurrent) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
        } else {
            MaterialTheme.colorScheme.background
        },
        border = BorderStroke(
            width = if (isCurrent) 1.5.dp else 1.dp,
            color = if (isCurrent) MaterialTheme.colorScheme.primary else extra.cardBorder
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = plan.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        plan.badge?.let { badge ->
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                    if (plan.description.isNotBlank() && !QuotaFormatters.containsRawTokenNumber(plan.description)) {
                        Text(
                            text = plan.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = QuotaFormatters.formatIdr(plan.priceIdr),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (isPaid && plan.billingCycle.isNotBlank()) {
                        Text(
                            text = "/ ${plan.billingCycle}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (plan.imageQuota > 0) {
                    PlanMetricChip(
                        label = "${plan.imageQuota} kuota gambar",
                        modifier = Modifier.weight(1f)
                    )
                }
                PlanMetricChip(
                    label = "Maks ${plan.maxBots} bot",
                    modifier = Modifier.weight(1f)
                )
            }

            val safeFeatures = remember(plan.features) {
                plan.features.filterNot { QuotaFormatters.containsRawTokenNumber(it) }
            }
            if (safeFeatures.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    safeFeatures.forEach { feature ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = feature,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            when {
                isCurrent -> {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = extra.success.copy(alpha = 0.14f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Verified,
                                contentDescription = null,
                                tint = extra.success,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Paket Aktif Saat Ini",
                                style = MaterialTheme.typography.labelLarge,
                                color = extra.success
                            )
                        }
                    }
                }
                isPaid -> {
                    Button(
                        onClick = onUpgradeQris,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("upgrade_qris_${plan.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QrCode2,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upgrade via QRIS", style = MaterialTheme.typography.labelLarge)
                    }
                }
                else -> {
                    OutlinedButton(
                        onClick = onDowngradeFree,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, extra.cardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("downgrade_free_${plan.id}")
                    ) {
                        Text("Pilih Paket ${plan.name}", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanMetricChip(
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun QrisCheckoutDialog(
    session: QrisCheckoutSession,
    isPolling: Boolean,
    isDownloading: Boolean,
    imageLoader: ImageLoader,
    onSaveQr: () -> Unit,
    onDismiss: () -> Unit
) {
    val extra = LocalMomoExtraColors.current
    val context = LocalContext.current
    val isPaid = session.status in setOf("paid", "settlement", "completed", "success")

    val decodedQrBitmap = remember(session.qrDataUrl) {
        val raw = session.qrDataUrl
        if (!raw.isNullOrBlank() && raw.startsWith("data:")) {
            runCatching {
                val (bytes, _) = AndroidMediaHelper.decodeDataUrl(raw)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        } else null
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xB8000000))
                .padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, extra.cardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.qris_title),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Paket ${session.planName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("qris_close_button")
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cd_close))
                        }
                    }

                    Text(
                        text = QuotaFormatters.formatIdr(session.amountIdr),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (session.orderId.isNotBlank()) {
                        Text(
                            text = "Order ID: ${session.orderId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // QR Preview Container
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, extra.cardBorder),
                        modifier = Modifier.size(236.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                decodedQrBitmap != null -> {
                                    Image(
                                        bitmap = decodedQrBitmap,
                                        contentDescription = "Kode QRIS Pembayaran",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                !session.qrImageUrl.isNullOrBlank() -> {
                                    AsyncImage(
                                        model = session.qrImageUrl,
                                        imageLoader = imageLoader,
                                        contentDescription = "Kode QRIS Pembayaran",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                !session.qrString.isNullOrBlank() -> {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.QrCode2,
                                            contentDescription = null,
                                            tint = Color(0xFF0C0D11),
                                            modifier = Modifier.size(56.dp)
                                        )
                                        Text(
                                            text = "Salin payload QRIS di bawah untuk membayar.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF333645)
                                        )
                                    }
                                }
                                else -> {
                                    Text(
                                        text = "Gambar QR belum tersedia.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF333645)
                                    )
                                }
                            }
                        }
                    }

                    // Status Banner
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isPaid) extra.success.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (isPaid) {
                                Icon(
                                    imageVector = Icons.Rounded.Verified,
                                    contentDescription = null,
                                    tint = extra.success
                                )
                                Text(
                                    text = stringResource(R.string.qris_paid),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = extra.success
                                )
                            } else {
                                if (isPolling) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.qris_waiting),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (!session.qrImageUrl.isNullOrBlank() || !session.qrDataUrl.isNullOrBlank()) {
                        Button(
                            onClick = onSaveQr,
                            enabled = !isDownloading,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("qris_save_button")
                        ) {
                            if (isDownloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.qris_save_btn))
                            }
                        }
                    }

                    if (!session.qrString.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Momo QRIS", session.qrString))
                                Toast.makeText(context, "Kode QRIS disalin", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, extra.cardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Salin Kode QRIS")
                        }
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Text(if (isPaid) "Selesai" else "Tutup")
                    }
                }
            }
        }
    }
}
