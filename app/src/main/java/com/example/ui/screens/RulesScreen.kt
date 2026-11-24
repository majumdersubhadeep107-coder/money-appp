package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.CurrencyFormatter
import com.example.model.PaiseJarUiState
import com.example.model.RoundUpCalculator
import com.example.model.SavingRules
import com.example.model.TransactionCategory
import com.example.ui.components.StatusNoticeBanner

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RulesScreen(
    state: PaiseJarUiState,
    onSaveRules: (SavingRules, onComplete: () -> Unit) -> Unit,
    onRestoreDefaults: (onComplete: () -> Unit) -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val current = state.rules

    var roundUpStep by remember { mutableIntStateOf(current.roundUpTargetRupees) }
    var maxPerPurchaseText by remember { mutableStateOf(current.maxPerPurchaseRupees.toInt().toString()) }
    var weeklyCapText by remember { mutableStateOf(current.weeklyCapRupees.toInt().toString()) }
    var monthlyCapText by remember { mutableStateOf(current.monthlyCapRupees.toInt().toString()) }
    var minPurchaseText by remember { mutableStateOf(current.minTransactionAmountRupees.toInt().toString()) }
    var excludedCategories by remember { mutableStateOf(current.excludedCategories) }
    var excludedMerchants by remember { mutableStateOf(current.excludedMerchants) }
    var newMerchantText by remember { mutableStateOf("") }
    var paused by remember { mutableStateOf(current.paused) }

    LaunchedEffect(current) {
        roundUpStep = current.roundUpTargetRupees
        maxPerPurchaseText = current.maxPerPurchaseRupees.toInt().toString()
        weeklyCapText = current.weeklyCapRupees.toInt().toString()
        monthlyCapText = current.monthlyCapRupees.toInt().toString()
        minPurchaseText = current.minTransactionAmountRupees.toInt().toString()
        excludedCategories = current.excludedCategories
        excludedMerchants = current.excludedMerchants
        paused = current.paused
    }

    val maxPerPurchaseVal = maxPerPurchaseText.toDoubleOrNull()
    val weeklyCapVal = weeklyCapText.toDoubleOrNull()
    val monthlyCapVal = monthlyCapText.toDoubleOrNull()
    val minPurchaseVal = minPurchaseText.toDoubleOrNull()

    val validMaxPurchase = maxPerPurchaseVal != null && maxPerPurchaseVal in 10.0..500.0
    val validWeeklyCap = weeklyCapVal != null && weeklyCapVal in 50.0..10000.0
    val validMonthlyCap = monthlyCapVal != null && weeklyCapVal != null &&
        monthlyCapVal >= weeklyCapVal && monthlyCapVal <= 50000.0
    val validMinPurchase = minPurchaseVal != null && minPurchaseVal in 0.0..1000.0
    val allInputsValid = validMaxPurchase && validWeeklyCap && validMonthlyCap && validMinPurchase

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
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Saving Rules & Limits",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = "Customize how round-ups are calculated in sandbox previews and saved to your account profile.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Pause / Active Switch Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (paused) "Round-up rules are paused" else "Round-up rules are active",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "Toggle anytime with zero penalty.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = !paused,
                            onCheckedChange = { paused = !it },
                            modifier = Modifier.testTag("rules_active_switch"),
                        )
                    }
                }
            }

            // Round-Up Target Step (₹10, ₹50, ₹100 + custom ₹10 increments)
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
                        Text(
                            text = "Round up purchases to next",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf(10, 50, 100).forEach { preset ->
                                FilterChip(
                                    selected = roundUpStep == preset,
                                    onClick = { roundUpStep = preset },
                                    label = { Text("₹$preset") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("rules_preset_$preset"),
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Custom ₹10 increment",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                IconButton(
                                    onClick = { if (roundUpStep > 10) roundUpStep -= 10 },
                                    enabled = roundUpStep > 10,
                                    modifier = Modifier.size(48.dp),
                                ) {
                                    Icon(Icons.Rounded.Remove, contentDescription = "Decrease round-up increment")
                                }
                                Text(
                                    text = CurrencyFormatter.formatInrInt(roundUpStep),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                IconButton(
                                    onClick = { if (roundUpStep < 500) roundUpStep += 10 },
                                    enabled = roundUpStep < 500,
                                    modifier = Modifier.size(48.dp),
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Increase round-up increment")
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        val (_, d180) = RoundUpCalculator.computeRawRoundUp(180.0, roundUpStep)
                        val (_, d470) = RoundUpCalculator.computeRawRoundUp(470.0, roundUpStep)
                        val (_, d500) = RoundUpCalculator.computeRawRoundUp(500.0, roundUpStep)
                        Text(
                            text = "Live preview: ₹180 → +${CurrencyFormatter.formatInr(d180)} • ₹470 → +${CurrencyFormatter.formatInr(d470)} • ₹500 → ${CurrencyFormatter.formatInr(d500)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            // Caps & Thresholds (App-Enforced)
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
                        Text(
                            text = "Caps & Minimum Purchase",
                            style = MaterialTheme.typography.titleMedium,
                        )

                        OutlinedTextField(
                            value = maxPerPurchaseText,
                            onValueChange = { maxPerPurchaseText = it },
                            label = { Text("Max round-up per purchase (₹10 – ₹500)") },
                            singleLine = true,
                            isError = !validMaxPurchase,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            supportingText = {
                                if (!validMaxPurchase) Text("Enter between ₹10 and ₹500")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("rules_max_purchase_input"),
                        )

                        OutlinedTextField(
                            value = weeklyCapText,
                            onValueChange = { weeklyCapText = it },
                            label = { Text("Weekly savings cap (₹50 – ₹10,000)") },
                            singleLine = true,
                            isError = !validWeeklyCap,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            supportingText = {
                                if (!validWeeklyCap) Text("Enter between ₹50 and ₹10,000")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("rules_weekly_cap_input"),
                        )

                        OutlinedTextField(
                            value = monthlyCapText,
                            onValueChange = { monthlyCapText = it },
                            label = { Text("Monthly savings cap (≥ Weekly cap, up to ₹50,000)") },
                            singleLine = true,
                            isError = !validMonthlyCap,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            supportingText = {
                                if (!validMonthlyCap) {
                                    Text("Must be ≥ weekly cap and at most ₹50,000")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("rules_monthly_cap_input"),
                        )

                        OutlinedTextField(
                            value = minPurchaseText,
                            onValueChange = { minPurchaseText = it },
                            label = { Text("Minimum eligible purchase (₹0 – ₹1,000)") },
                            singleLine = true,
                            isError = !validMinPurchase,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            supportingText = {
                                if (!validMinPurchase) Text("Enter between ₹0 and ₹1,000")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("rules_min_purchase_input"),
                        )
                    }
                }
            }

            // Partner-Dependent Safeguard (Honestly Disabled)
            item {
                StatusNoticeBanner(
                    title = "Minimum Bank Balance Guard — Unavailable",
                    subtitle = "Low-balance protection cannot be enforced until an authorized RBI Account Aggregator balance-read partner is connected. No bank balance is fetched or assumed.",
                    icon = Icons.Rounded.LinkOff,
                )
            }

            // Excluded Categories & Merchants
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
                        Text(
                            text = "Excluded Categories",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TransactionCategory.entries.forEach { cat ->
                                val selected = cat in excludedCategories
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        excludedCategories = if (selected) {
                                            excludedCategories - cat
                                        } else {
                                            excludedCategories + cat
                                        }
                                    },
                                    label = { Text(cat.label) },
                                    modifier = Modifier.heightIn(min = 48.dp),
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Text(
                            text = "Excluded Merchants",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = newMerchantText,
                                onValueChange = { newMerchantText = it },
                                label = { Text("Add merchant name") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            Button(
                                onClick = {
                                    val trimmed = newMerchantText.trim()
                                    if (trimmed.isNotEmpty() && excludedMerchants.size < 30) {
                                        excludedMerchants = excludedMerchants + trimmed
                                        newMerchantText = ""
                                    }
                                },
                                enabled = newMerchantText.trim().isNotEmpty(),
                                modifier = Modifier.heightIn(min = 52.dp),
                            ) {
                                Text("Add")
                            }
                        }

                        if (excludedMerchants.isEmpty()) {
                            Text(
                                text = "No specific merchants excluded.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                excludedMerchants.forEach { merchant ->
                                    InputChip(
                                        selected = true,
                                        onClick = { excludedMerchants = excludedMerchants - merchant },
                                        label = { Text(merchant) },
                                        trailingIcon = {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = "Remove $merchant exclusion",
                                                modifier = Modifier.size(16.dp),
                                            )
                                        },
                                        modifier = Modifier.heightIn(min = 48.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Save / Restore Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            onRestoreDefaults {
                                onShowMessage("Restored default saving rules.")
                            }
                        },
                        enabled = !state.isBusy,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 50.dp)
                            .testTag("rules_restore_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Restore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Defaults")
                    }

                    Button(
                        onClick = {
                            val updated = SavingRules(
                                roundUpTargetRupees = roundUpStep,
                                maxPerPurchaseRupees = maxPerPurchaseVal ?: 50.0,
                                weeklyCapRupees = weeklyCapVal ?: 400.0,
                                monthlyCapRupees = monthlyCapVal ?: 1500.0,
                                minTransactionAmountRupees = minPurchaseVal ?: 50.0,
                                excludedCategories = excludedCategories,
                                excludedMerchants = excludedMerchants,
                                paused = paused,
                            )
                            onSaveRules(updated) {
                                onShowMessage("Saving rules updated in your account.")
                            }
                        },
                        enabled = allInputsValid && !state.isBusy,
                        modifier = Modifier
                            .weight(1.4f)
                            .heightIn(min = 50.dp)
                            .testTag("rules_save_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Save rules")
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
