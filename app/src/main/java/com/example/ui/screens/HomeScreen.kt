package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.CurrencyFormatter
import com.example.model.EvaluatedTransaction
import com.example.model.PaiseJarUiState
import com.example.ui.components.SandboxPreviewBadge
import com.example.ui.components.StatusNoticeBanner
import com.example.ui.components.TransactionRowCard
import com.example.ui.components.goalIcon
import com.example.ui.theme.LocalReducedMotion

@Composable
fun HomeScreen(
    state: PaiseJarUiState,
    onTogglePause: () -> Unit,
    onReviewRules: () -> Unit,
    onSeeAllActivity: () -> Unit,
    onOpenMonthlySummary: () -> Unit,
    onOpenGoals: () -> Unit,
    onLoadSandboxScenarios: () -> Unit,
    onSelectTransaction: (EvaluatedTransaction) -> Unit,
) {
    val reducedMotion = LocalReducedMotion.current
    val recentTransactions = state.evaluatedTransactions.take(4)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                StatusNoticeBanner(
                    title = "Bank & UPI partner not connected",
                    subtitle = "Authorized RBI Account Aggregator & UPI Autopay partner setup is required before real purchases can be read or rupees transferred. Confirmed ledger balance is ₹0.",
                    icon = Icons.Rounded.LinkOff,
                    actionLabel = "Details",
                    onActionClick = onOpenGoals,
                    modifier = Modifier.testTag("home_partner_status_banner"),
                )
            }

            // Confirmed Real-Money Financial Summary (Honest ₹0)
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_confirmed_ledger_card"),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Confirmed Savings Ledger",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                            ) {
                                Text(
                                    text = "LIVE LEDGER",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            LedgerMetricTile(
                                label = "Available balance",
                                amount = CurrencyFormatter.formatInr(state.confirmedLedger.availableBalanceRupees),
                                modifier = Modifier.weight(1f),
                            )
                            LedgerMetricTile(
                                label = "Pending transfers",
                                amount = CurrencyFormatter.formatInr(state.confirmedLedger.pendingTransfersRupees),
                                modifier = Modifier.weight(1f),
                            )
                            LedgerMetricTile(
                                label = "Completed total",
                                amount = CurrencyFormatter.formatInr(state.confirmedLedger.completedContributionsRupees),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            // Sandbox Calculation Preview Hero Card
            item {
                val rules = state.rules
                val isPaused = rules.paused
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_sandbox_preview_card"),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SandboxPreviewBadge()
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = if (isPaused) {
                                    MaterialTheme.colorScheme.tertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                            ) {
                                Text(
                                    text = if (isPaused) "Rules Paused" else "Rules Active",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isPaused) {
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onPrimary
                                    },
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }
                        }

                        Text(
                            text = "Estimated round-ups this month (sandbox preview)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )

                        Text(
                            text = CurrencyFormatter.formatInr(state.sandboxSummary.currentMonthPreviewRupees),
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.testTag("home_sandbox_month_total"),
                        )

                        Text(
                            text = "This week preview: ${CurrencyFormatter.formatInr(state.sandboxSummary.currentWeekPreviewRupees)} • ${state.sandboxSummary.eligibleRoundUpCount} eligible • Avg ${CurrencyFormatter.formatInr(state.sandboxSummary.averageRoundUpRupees)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )

                        val capTarget = rules.monthlyCapRupees.coerceAtLeast(1.0)
                        val progressRaw = (state.sandboxSummary.currentMonthPreviewRupees / capTarget)
                            .toFloat()
                            .coerceIn(0f, 1f)
                        val animatedProgress by animateFloatAsState(
                            targetValue = progressRaw,
                            animationSpec = tween(durationMillis = if (reducedMotion) 0 else 350),
                            label = "monthly_cap_progress",
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "Monthly cap usage",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    text = "${CurrencyFormatter.formatInr(state.sandboxSummary.currentMonthPreviewRupees)} / ${CurrencyFormatter.formatInr(rules.monthlyCapRupees)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(999.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Button(
                                onClick = onTogglePause,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("home_pause_resume_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPaused) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.secondary
                                    },
                                    contentColor = if (isPaused) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSecondary
                                    },
                                ),
                            ) {
                                Icon(
                                    imageVector = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (isPaused) "Resume rules" else "Pause rules")
                            }

                            OutlinedButton(
                                onClick = onReviewRules,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("home_review_rules_button"),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Tune,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Review rules")
                            }
                        }
                    }
                }
            }

            // Selected Goal Card
            state.selectedGoal?.let { goal ->
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(
                                        imageVector = goalIcon(goal.iconKey),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp),
                                    )
                                    Column {
                                        Text(
                                            text = "Active Round-Up Goal: ${goal.name}",
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                        Text(
                                            text = "Confirmed ${CurrencyFormatter.formatInr(goal.confirmedBalanceRupees)} of ${CurrencyFormatter.formatInr(goal.targetAmountRupees)} target",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                TextButton(
                                    onClick = onOpenGoals,
                                    modifier = Modifier.heightIn(min = 48.dp),
                                ) {
                                    Text("Manage")
                                }
                            }
                            LinearProgressIndicator(
                                progress = {
                                    (goal.confirmedBalanceRupees / goal.targetAmountRupees.coerceAtLeast(1.0))
                                        .toFloat()
                                        .coerceIn(0f, 1f)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(999.dp)),
                            )
                        }
                    }
                }
            }

            // Weekly Chart Preview Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.BarChart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = "Weekly preview pace",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                            TextButton(
                                onClick = onOpenMonthlySummary,
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("home_monthly_summary_button"),
                            ) {
                                Text("Monthly summary")
                            }
                        }

                        val maxSlice = state.sandboxSummary.weeklyBreakdown
                            .maxOfOrNull { it.amountRupees }
                            ?.coerceAtLeast(1.0)
                            ?: 1.0

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            state.sandboxSummary.weeklyBreakdown.forEach { slice ->
                                val ratio = (slice.amountRupees / maxSlice).toFloat().coerceIn(0f, 1f)
                                val barFraction by animateFloatAsState(
                                    targetValue = if (slice.amountRupees > 0.0) ratio.coerceAtLeast(0.14f) else 0.06f,
                                    animationSpec = tween(durationMillis = if (reducedMotion) 0 else 300),
                                    label = "bar_${slice.weekLabel}",
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .semantics(mergeDescendants = true) {
                                            contentDescription = "${slice.weekLabel}: ${CurrencyFormatter.formatInr(slice.amountRupees)} preview across ${slice.transactionCount} transactions"
                                        },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                ) {
                                    Text(
                                        text = CurrencyFormatter.formatInr(slice.amountRupees),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.55f)
                                            .height((62f * barFraction).dp)
                                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                            .background(
                                                if (slice.amountRupees > 0.0) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.outlineVariant
                                                },
                                            ),
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = slice.weekLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Recent Activity Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Recent sandbox activity",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    TextButton(
                        onClick = onSeeAllActivity,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("home_see_all_activity"),
                    ) {
                        Text("See all")
                    }
                }
            }

            if (recentTransactions.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Science,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp),
                            )
                            Text(
                                text = "No transactions yet",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "Your live ledger starts clean at ₹0. Load optional sandbox scenarios (₹180 coffee, ₹470 food, refunds, reversals, and edge cases) to test your round-up rules safely.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(
                                onClick = onLoadSandboxScenarios,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("home_load_sandbox_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoGraph,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Load sandbox test scenarios")
                            }
                        }
                    }
                }
            } else {
                items(recentTransactions, key = { it.transaction.id }) { item ->
                    TransactionRowCard(
                        item = item,
                        onClick = { onSelectTransaction(item) },
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun LedgerMetricTile(
    label: String,
    amount: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = amount,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
