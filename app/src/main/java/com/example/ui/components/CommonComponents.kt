package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LaptopMac
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalGroceryStore
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.model.CurrencyFormatter
import com.example.model.EvaluatedTransaction
import com.example.model.RoundUpStatus
import com.example.model.TransactionCategory
import com.example.model.TransactionEventKind
import com.example.model.TransferInstructionStatus

@Composable
fun PartnerStatusBadge(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.LinkOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = "BANK NOT LINKED",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun SandboxPreviewBadge(
    modifier: Modifier = Modifier,
    text: String = "SANDBOX PREVIEW • NOT REAL MONEY",
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun StatusNoticeBanner(
    title: String,
    subtitle: String,
    icon: ImageVector = Icons.Rounded.Info,
    containerColor: Color = MaterialTheme.colorScheme.tertiaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onTertiaryContainer,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.25f)),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(22.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor,
                )
            }
            if (!actionLabel.isNullOrBlank() && onActionClick != null) {
                TextButton(
                    onClick = onActionClick,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = contentColor,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

fun categoryIcon(category: TransactionCategory): ImageVector = when (category) {
    TransactionCategory.CAFE -> Icons.Rounded.LocalCafe
    TransactionCategory.FOOD_DELIVERY -> Icons.Rounded.Restaurant
    TransactionCategory.GROCERY -> Icons.Rounded.LocalGroceryStore
    TransactionCategory.SHOPPING -> Icons.Rounded.ShoppingBag
    TransactionCategory.TRANSPORT -> Icons.Rounded.DirectionsCar
    TransactionCategory.SUBSCRIPTIONS -> Icons.Rounded.Subscriptions
    TransactionCategory.UNCLEAR -> Icons.AutoMirrored.Rounded.HelpOutline
}

fun goalIcon(iconKey: String): ImageVector = when (iconKey) {
    "emergency" -> Icons.Rounded.HealthAndSafety
    "travel" -> Icons.Rounded.FlightTakeoff
    "home" -> Icons.Rounded.Home
    "tech" -> Icons.Rounded.LaptopMac
    "education" -> Icons.Rounded.School
    "festive" -> Icons.Rounded.Celebration
    else -> Icons.Rounded.Savings
}

@Composable
fun TransactionRowCard(
    item: EvaluatedTransaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tx = item.transaction
    val isPreviewPositive = item.previewRoundUpRupees > 0.0
    val isFailureOrReview = item.status in setOf(
        RoundUpStatus.FAILED_INSTRUCTION,
        RoundUpStatus.DELAYED_PENDING,
        RoundUpStatus.REFUND_NO_DEBIT,
        RoundUpStatus.REVERSAL_NO_DEBIT,
        RoundUpStatus.DUPLICATE_BLOCKED,
    ) || item.categoryEnum == TransactionCategory.UNCLEAR

    val statusText = when (item.status) {
        RoundUpStatus.PREVIEW_ELIGIBLE ->
            "→ ${CurrencyFormatter.formatInr(item.roundUpTargetRupees)} preview"
        RoundUpStatus.EXACT_MULTIPLE -> "Exact multiple (₹0)"
        RoundUpStatus.BELOW_MINIMUM -> "Below minimum"
        RoundUpStatus.OVER_PURCHASE_LIMIT -> "Over purchase limit"
        RoundUpStatus.WEEKLY_CAP_REACHED -> "Weekly cap reached"
        RoundUpStatus.MONTHLY_CAP_REACHED -> "Monthly cap reached"
        RoundUpStatus.EXCLUDED_CATEGORY -> "Category excluded"
        RoundUpStatus.EXCLUDED_MERCHANT -> "Merchant excluded"
        RoundUpStatus.EXCLUDED_BY_USER -> "Excluded by you"
        RoundUpStatus.PAUSED -> "Rules paused"
        RoundUpStatus.REFUND_NO_DEBIT -> "Refund • ₹0"
        RoundUpStatus.REVERSAL_NO_DEBIT -> "Reversed • ₹0"
        RoundUpStatus.DELAYED_PENDING -> "Delayed feed"
        RoundUpStatus.FAILED_INSTRUCTION -> "Failed instruction"
        RoundUpStatus.DUPLICATE_BLOCKED -> "Duplicate blocked"
    }

    val a11yDescription = buildString {
        append("${tx.merchantName}, ${item.categoryEnum.label}, ")
        append("Purchase ${CurrencyFormatter.formatInr(tx.purchaseAmountRupees)}, ")
        if (isPreviewPositive) {
            append("Sandbox round-up preview plus ${CurrencyFormatter.formatInr(item.previewRoundUpRupees)}. ")
        } else {
            append("No round-up saved. ")
        }
        append("$statusText. ${item.dateDisplayLabel}")
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = a11yDescription
            }
            .testTag("tx_row_${tx.id}"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 76.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isPreviewPositive -> MaterialTheme.colorScheme.primaryContainer
                            isFailureOrReview -> MaterialTheme.colorScheme.tertiaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = categoryIcon(item.categoryEnum),
                    contentDescription = null,
                    tint = when {
                        isPreviewPositive -> MaterialTheme.colorScheme.onPrimaryContainer
                        isFailureOrReview -> MaterialTheme.colorScheme.onTertiaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(21.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = tx.merchantName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (item.eventKindEnum != TransactionEventKind.PURCHASE) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                        ) {
                            Text(
                                text = item.eventKindEnum.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${item.dateDisplayLabel} • ${item.categoryEnum.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Purchase: ${CurrencyFormatter.formatInr(tx.purchaseAmountRupees)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = if (isPreviewPositive) {
                        "+${CurrencyFormatter.formatInr(item.previewRoundUpRupees)}"
                    } else {
                        "₹0"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isPreviewPositive -> MaterialTheme.colorScheme.primary
                        item.status == RoundUpStatus.FAILED_INSTRUCTION -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = when {
                        isPreviewPositive -> MaterialTheme.colorScheme.primaryContainer
                        item.status == RoundUpStatus.FAILED_INSTRUCTION -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            isPreviewPositive -> MaterialTheme.colorScheme.onPrimaryContainer
                            item.status == RoundUpStatus.FAILED_INSTRUCTION -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailSheet(
    item: EvaluatedTransaction,
    onDismiss: () -> Unit,
    onToggleExclude: (excluded: Boolean) -> Unit,
) {
    val tx = item.transaction
    val canExcludeToggle = item.eventKindEnum == TransactionEventKind.PURCHASE &&
        item.transferInstructionStatusEnum != TransferInstructionStatus.FAILED_TEST

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SandboxPreviewBadge()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = categoryIcon(item.categoryEnum),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tx.merchantName,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = tx.description.ifBlank { item.categoryEnum.label },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Section 1: Purchase Record
            DetailSectionCard(title = "1. Purchase Record") {
                DetailLine("Purchase Amount", CurrencyFormatter.formatInr(tx.purchaseAmountRupees))
                DetailLine("Event Type", item.eventKindEnum.label)
                DetailLine("Category", item.categoryEnum.label)
                DetailLine("Raw Feed Label", tx.rawMerchantLabel.ifBlank { tx.merchantName })
                DetailLine("Timestamp", item.dateDisplayLabel)
                DetailLine("Idempotency Key", tx.idempotencyKey)
            }

            // Section 2: Round-Up Calculation
            DetailSectionCard(title = "2. Round-Up Calculation") {
                DetailLine("Rounded Target", CurrencyFormatter.formatInr(item.roundUpTargetRupees))
                DetailLine("Raw Difference", CurrencyFormatter.formatInr(item.rawDifferenceRupees))
                DetailLine(
                    label = "Preview Round-Up",
                    value = CurrencyFormatter.formatInr(item.previewRoundUpRupees),
                    highlight = item.previewRoundUpRupees > 0.0,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = when (item.status) {
                            RoundUpStatus.PREVIEW_ELIGIBLE -> Icons.Rounded.CheckCircle
                            RoundUpStatus.PAUSED -> Icons.Rounded.PauseCircle
                            RoundUpStatus.DELAYED_PENDING -> Icons.Rounded.Schedule
                            RoundUpStatus.FAILED_INSTRUCTION -> Icons.Rounded.ErrorOutline
                            else -> Icons.Rounded.Block
                        },
                        contentDescription = null,
                        tint = if (item.previewRoundUpRupees > 0.0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = item.reasonText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            // Section 3: Transfer Instruction & Confirmation Status
            DetailSectionCard(title = "3. Transfer Instruction & Settlement") {
                DetailLine("Instruction State", item.transferInstructionStatusEnum.label)
                DetailLine("Settlement Status", item.transferConfirmationStatusEnum.label)
                DetailLine("Reference", tx.providerReference.ifBlank { "Sandbox only" })
            }

            if (canExcludeToggle) {
                if (tx.userExcluded) {
                    Button(
                        onClick = { onToggleExclude(false) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .testTag("sheet_reinclude_button"),
                    ) {
                        Text("Re-include in preview round-ups")
                    }
                } else {
                    OutlinedButton(
                        onClick = { onToggleExclude(true) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .testTag("sheet_exclude_button"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Text("Exclude from preview round-ups")
                    }
                }
                Text(
                    text = "Only cancels preview instructions. Settled partner transfers cannot be reversed from this screen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DetailSectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            content()
        }
    }
}

@Composable
private fun DetailLine(
    label: String,
    value: String,
    highlight: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.45f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.SemiBold,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.55f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

@Composable
fun AccountClosureDialog(
    onConfirmDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.DeleteForever,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = {
            Text("Delete account & all saved data?")
        },
        text = {
            Text(
                "This permanently deletes your PaiseJar account profile, saving rules, goals, sandbox transactions, audit logs, and support tickets from Firestore, then removes your sign-in session. This cannot be undone.",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("confirm_delete_account_button"),
            ) {
                Text("Delete permanently")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun AsyncDrawableImage(
    @DrawableRes resId: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    inSampleSize: Int = 2,
) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, resId, inSampleSize) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val options = BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize.coerceAtLeast(1)
                }
                BitmapFactory.decodeResource(context.resources, resId, options)?.asImageBitmap()
            }.getOrNull()
        }
    }

    val loaded = bitmap
    if (loaded != null) {
        Image(
            bitmap = loaded,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
        )
    }
}
