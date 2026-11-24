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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.CurrencyFormatter
import com.example.model.PaiseJarUiState
import com.example.model.SavingsGoalDoc
import com.example.ui.components.StatusNoticeBanner
import com.example.ui.components.goalIcon

private val GOAL_ICONS = listOf(
    "emergency" to "Safety",
    "travel" to "Travel",
    "home" to "Home",
    "tech" to "Tech",
    "education" to "Study",
    "festive" to "Festive",
)

@Composable
fun GoalsScreen(
    state: PaiseJarUiState,
    onToggleShowArchived: (Boolean) -> Unit,
    onSelectGoalForRoundUps: (String) -> Unit,
    onSaveGoal: (
        existingGoalId: String?,
        name: String,
        targetAmountRupees: Double,
        targetDateLabel: String?,
        iconKey: String,
    ) -> Unit,
    onSetGoalArchived: (goalId: String, archived: Boolean) -> Unit,
    onShowMessage: (String) -> Unit,
) {
    var editingGoal by remember { mutableStateOf<SavingsGoalDoc?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val displayedGoals = if (state.showArchivedGoals) state.archivedGoals else state.activeGoals

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Savings Goals",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            text = "Plan and organize goal jars. Real rupee deposits stay ₹0 until a transfer partner is connected.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("goals_new_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("New goal")
                    }
                }
            }

            item {
                StatusNoticeBanner(
                    title = "Non-investment target buckets",
                    subtitle = "Goals do not hold funds, earn interest, or imply deposit insurance in this release. Confirmed balances and pending partner transfers are tracked separately at ₹0.",
                    icon = Icons.Rounded.Info,
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !state.showArchivedGoals,
                        onClick = { onToggleShowArchived(false) },
                        label = { Text("Active (${state.activeGoals.size})") },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("goals_filter_active"),
                    )
                    FilterChip(
                        selected = state.showArchivedGoals,
                        onClick = { onToggleShowArchived(true) },
                        label = { Text("Archived (${state.archivedGoals.size})") },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("goals_filter_archived"),
                    )
                }
            }

            if (displayedGoals.isEmpty()) {
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
                                text = if (state.showArchivedGoals) "No archived goals" else "No active goals",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = if (state.showArchivedGoals) {
                                    "Goals you archive appear here and can be restored anytime."
                                } else {
                                    "Tap 'New goal' to set your first target."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                items(displayedGoals, key = { it.id }) { goal ->
                    val isSelected = goal.id == state.selectedGoal?.id && !goal.archived
                    GoalLedgerCard(
                        goal = goal,
                        isSelectedForRoundUps = isSelected,
                        onSelect = {
                            onSelectGoalForRoundUps(goal.id)
                            onShowMessage("Selected '${goal.name}' for future round-up allocation.")
                        },
                        onEdit = { editingGoal = goal },
                        onToggleArchive = {
                            val nextArchive = !goal.archived
                            onSetGoalArchived(goal.id, nextArchive)
                            onShowMessage(
                                if (nextArchive) "Archived '${goal.name}'." else "Restored '${goal.name}' to active goals.",
                            )
                        },
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showCreateDialog) {
        GoalEditorDialog(
            initialGoal = null,
            onDismiss = { showCreateDialog = false },
            onSave = { name, target, dateLabel, iconKey ->
                showCreateDialog = false
                onSaveGoal(null, name, target, dateLabel, iconKey)
                onShowMessage("Created goal '$name'.")
            },
        )
    }

    editingGoal?.let { existing ->
        GoalEditorDialog(
            initialGoal = existing,
            onDismiss = { editingGoal = null },
            onSave = { name, target, dateLabel, iconKey ->
                editingGoal = null
                onSaveGoal(existing.id, name, target, dateLabel, iconKey)
                onShowMessage("Updated goal '$name'.")
            },
        )
    }
}

@Composable
private fun GoalLedgerCard(
    goal: SavingsGoalDoc,
    isSelectedForRoundUps: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onToggleArchive: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("goal_card_${goal.id}"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isSelectedForRoundUps) 2.dp else 1.dp,
            color = if (isSelectedForRoundUps) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
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
                        modifier = Modifier.size(26.dp),
                    )
                    Column {
                        Text(
                            text = goal.name,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        val targetSubtitle = buildString {
                            append("Target ${CurrencyFormatter.formatInr(goal.targetAmountRupees)}")
                            goal.targetDateLabel?.takeIf { it.isNotBlank() }?.let {
                                append(" • By $it")
                            }
                        }
                        Text(
                            text = targetSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (isSelectedForRoundUps) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = "Selected",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Confirmed: ${CurrencyFormatter.formatInr(goal.confirmedBalanceRupees)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Pending: ${CurrencyFormatter.formatInr(goal.pendingTransferRupees)} • Failed: ${goal.failedTransferCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!goal.archived && !isSelectedForRoundUps) {
                    Button(
                        onClick = onSelect,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                    ) {
                        Text("Use for round-ups")
                    }
                }
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Rename or edit ${goal.name}",
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Edit")
                }
                OutlinedButton(
                    onClick = onToggleArchive,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Icon(
                        imageVector = if (goal.archived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (goal.archived) "Restore" else "Archive")
                }
            }
        }
    }
}

@Composable
private fun GoalEditorDialog(
    initialGoal: SavingsGoalDoc?,
    onDismiss: () -> Unit,
    onSave: (name: String, targetAmount: Double, dateLabel: String?, iconKey: String) -> Unit,
) {
    var name by remember { mutableStateOf(initialGoal?.name ?: "") }
    var targetText by remember {
        mutableStateOf(initialGoal?.targetAmountRupees?.toInt()?.toString() ?: "5000")
    }
    var dateLabel by remember { mutableStateOf(initialGoal?.targetDateLabel ?: "") }
    var iconKey by remember { mutableStateOf(initialGoal?.iconKey ?: "emergency") }

    val parsedTarget = targetText.toDoubleOrNull()
    val validName = name.trim().isNotEmpty()
    val validTarget = parsedTarget != null && parsedTarget in 100.0..10_000_000.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialGoal == null) "Create savings goal" else "Edit goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Goal name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it },
                    label = { Text("Target amount (₹100 – ₹1,00,00,000)") },
                    singleLine = true,
                    isError = targetText.isNotEmpty() && !validTarget,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = dateLabel,
                    onValueChange = { dateLabel = it },
                    label = { Text("Target month (optional, e.g. Dec 2026)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Icon", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(GOAL_ICONS) { (key, label) ->
                        FilterChip(
                            selected = iconKey == key,
                            onClick = { iconKey = key },
                            label = { Text(label) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val t = parsedTarget ?: return@Button
                    onSave(
                        name.trim(),
                        t,
                        dateLabel.trim().takeIf { it.isNotEmpty() },
                        iconKey,
                    )
                },
                enabled = validName && validTarget,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text("Save goal")
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
