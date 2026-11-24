package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.ActivityFilter
import com.example.model.DateRangeBucket
import com.example.model.DateRangeFilter
import com.example.model.EvaluatedTransaction
import com.example.model.PaiseJarUiState
import com.example.model.RoundUpStatus
import com.example.model.TransactionCategory
import com.example.model.TransactionEventKind
import com.example.model.TransferInstructionStatus
import com.example.ui.components.SandboxPreviewBadge
import com.example.ui.components.TransactionRowCard
import java.util.UUID

@Composable
fun ActivityScreen(
    state: PaiseJarUiState,
    onSearchQueryChange: (String) -> Unit,
    onSelectDateRange: (DateRangeFilter) -> Unit,
    onSelectFilter: (ActivityFilter) -> Unit,
    onSelectTransaction: (EvaluatedTransaction) -> Unit,
    onLoadSandboxPack: () -> Unit,
    onClearSandbox: () -> Unit,
    onAddSandboxTransaction: (
        merchantName: String,
        rawMerchantLabel: String,
        description: String,
        category: TransactionCategory,
        purchaseAmountRupees: Double,
        eventKind: TransactionEventKind,
        instructionStatus: TransferInstructionStatus,
        idempotencyKey: String,
    ) -> Unit,
    onShowMessage: (String) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredItems = remember(
        state.evaluatedTransactions,
        state.searchQuery,
        state.dateRangeFilter,
        state.activityFilter,
    ) {
        val query = state.searchQuery.trim().lowercase()
        state.evaluatedTransactions.filter { item ->
            val matchesQuery = query.isEmpty() ||
                item.transaction.merchantName.lowercase().contains(query) ||
                item.transaction.rawMerchantLabel.lowercase().contains(query) ||
                item.transaction.description.lowercase().contains(query) ||
                item.categoryEnum.label.lowercase().contains(query)

            val matchesDate = when (state.dateRangeFilter) {
                DateRangeFilter.ALL -> true
                DateRangeFilter.THIS_WEEK -> item.dateRangeBucketEnum == DateRangeBucket.THIS_WEEK
                DateRangeFilter.THIS_MONTH -> item.transaction.currentMonth
                DateRangeFilter.PREVIOUS_MONTH -> item.dateRangeBucketEnum == DateRangeBucket.PREVIOUS_MONTH
            }

            val matchesStatus = when (state.activityFilter) {
                ActivityFilter.ALL -> true
                ActivityFilter.ELIGIBLE -> item.previewRoundUpRupees > 0.0
                ActivityFilter.EXCLUDED -> item.status in setOf(
                    RoundUpStatus.EXCLUDED_BY_USER,
                    RoundUpStatus.EXCLUDED_CATEGORY,
                    RoundUpStatus.EXCLUDED_MERCHANT,
                )
                ActivityFilter.NEEDS_REVIEW -> item.status in setOf(
                    RoundUpStatus.FAILED_INSTRUCTION,
                    RoundUpStatus.DELAYED_PENDING,
                    RoundUpStatus.REFUND_NO_DEBIT,
                    RoundUpStatus.REVERSAL_NO_DEBIT,
                    RoundUpStatus.DUPLICATE_BLOCKED,
                ) || item.categoryEnum == TransactionCategory.UNCLEAR
            }

            matchesQuery && matchesDate && matchesStatus
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Activity Ledger",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        SandboxPreviewBadge(text = "SANDBOX PREVIEW")
                    }

                    Text(
                        text = "All entries below are sandbox calculation events. Real bank feeds appear here once an authorized RBI Account Aggregator is linked.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { showAddDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("activity_add_test_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Add test event")
                        }

                        if (state.rawTransactions.isEmpty()) {
                            OutlinedButton(
                                onClick = {
                                    onLoadSandboxPack()
                                    onShowMessage("Loaded sandbox scenario pack.")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("activity_load_pack_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Science,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Load scenarios")
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    onClearSandbox()
                                    onShowMessage("Cleared sandbox events.")
                                },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("activity_clear_pack_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteSweep,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Clear")
                            }
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("activity_search_input"),
                    singleLine = true,
                    placeholder = { Text("Search merchant, POS code, or category") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search activity",
                        )
                    },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Rounded.Clear,
                                    contentDescription = "Clear search",
                                )
                            }
                        }
                    },
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(DateRangeFilter.entries) { range ->
                            FilterChip(
                                selected = state.dateRangeFilter == range,
                                onClick = { onSelectDateRange(range) },
                                label = { Text(range.label) },
                                modifier = Modifier.heightIn(min = 48.dp),
                            )
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ActivityFilter.entries) { filter ->
                            FilterChip(
                                selected = state.activityFilter == filter,
                                onClick = { onSelectFilter(filter) },
                                label = { Text(filter.label) },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("activity_filter_${filter.name.lowercase()}"),
                            )
                        }
                    }
                }
            }

            if (filteredItems.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "No matching events",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = if (state.rawTransactions.isEmpty()) {
                                    "Load sandbox test scenarios or add a custom test spend to inspect how rules, caps, refunds, and unclear merchants behave."
                                } else {
                                    "Try clearing your search query or selecting All dates."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                items(filteredItems, key = { it.transaction.id }) { item ->
                    TransactionRowCard(
                        item = item,
                        onClick = { onSelectTransaction(item) },
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showAddDialog) {
        AddSandboxEventDialog(
            onDismiss = { showAddDialog = false },
            onSave = { merchant, rawLabel, desc, cat, amount, eventKind, instructionStatus, idempotencyKey ->
                showAddDialog = false
                onAddSandboxTransaction(
                    merchant,
                    rawLabel,
                    desc,
                    cat,
                    amount,
                    eventKind,
                    instructionStatus,
                    idempotencyKey,
                )
            },
        )
    }
}

@Composable
private fun AddSandboxEventDialog(
    onDismiss: () -> Unit,
    onSave: (
        merchantName: String,
        rawMerchantLabel: String,
        description: String,
        category: TransactionCategory,
        purchaseAmountRupees: Double,
        eventKind: TransactionEventKind,
        instructionStatus: TransferInstructionStatus,
        idempotencyKey: String,
    ) -> Unit,
) {
    var merchant by remember { mutableStateOf("Chai Point") }
    var rawLabel by remember { mutableStateOf("CHAIPOINT*UPI-TEST") }
    var amountText by remember { mutableStateOf("180") }
    var idempotencyKey by remember {
        mutableStateOf("tx_${UUID.randomUUID().toString().take(8)}")
    }
    var selectedCategory by remember { mutableStateOf(TransactionCategory.CAFE) }
    var selectedEventKind by remember { mutableStateOf(TransactionEventKind.PURCHASE) }
    var simulateFailure by remember { mutableStateOf(false) }

    val parsedAmount = amountText.toDoubleOrNull()
    val validAmount = parsedAmount != null && parsedAmount > 0.0 && parsedAmount <= 1_000_000.0
    val validMerchant = merchant.trim().isNotEmpty()
    val validKey = idempotencyKey.trim().isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add sandbox test event") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Creates an isolated SANDBOX_PREVIEW entry to verify round-up math, caps, refunds, or duplicate blocking.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = rawLabel,
                    onValueChange = { rawLabel = it },
                    label = { Text("Raw feed label (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹)") },
                    singleLine = true,
                    isError = amountText.isNotEmpty() && !validAmount,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = idempotencyKey,
                    onValueChange = { idempotencyKey = it },
                    label = { Text("Idempotency key (reuse to test duplicate guard)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Category", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(TransactionCategory.entries) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.label) },
                        )
                    }
                }
                Text("Event type", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(TransactionEventKind.entries) { kind ->
                        FilterChip(
                            selected = selectedEventKind == kind,
                            onClick = { selectedEventKind = kind },
                            label = { Text(kind.label) },
                        )
                    }
                }
                FilterChip(
                    selected = simulateFailure,
                    onClick = { simulateFailure = !simulateFailure },
                    label = { Text("Simulate failed transfer test") },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = parsedAmount ?: return@Button
                    val instruction = when {
                        simulateFailure -> TransferInstructionStatus.FAILED_TEST
                        selectedEventKind == TransactionEventKind.REFUND ||
                            selectedEventKind == TransactionEventKind.REVERSAL ->
                            TransferInstructionStatus.CANCELLED
                        selectedEventKind == TransactionEventKind.DELAYED_FEED ->
                            TransferInstructionStatus.BLOCKED_NO_PARTNER
                        else -> TransferInstructionStatus.SIMULATED_PREVIEW
                    }
                    onSave(
                        merchant.trim(),
                        rawLabel.trim().ifEmpty { merchant.trim() },
                        "User-triggered ${selectedEventKind.label.lowercase()} sandbox test",
                        selectedCategory,
                        amount,
                        selectedEventKind,
                        instruction,
                        idempotencyKey.trim(),
                    )
                },
                enabled = validAmount && validMerchant && validKey,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text("Add event")
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
