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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.PaiseJarUiState
import com.example.model.RoundUpCalculator
import com.example.ui.components.AccountClosureDialog
import com.example.ui.components.AsyncDrawableImage
import com.example.ui.components.StatusNoticeBanner

@Composable
fun ProfileScreen(
    state: PaiseJarUiState,
    userEmail: String?,
    onSavePreferences: (
        displayName: String,
        notificationsEnabled: Boolean,
        weeklyDigestEnabled: Boolean,
        dataConsentAccepted: Boolean,
    ) -> Unit,
    onSubmitSupportTicket: (topic: String, message: String) -> Unit,
    onOpenMonthlySummary: () -> Unit,
    onReplayDisclosures: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val account = state.userAccount
    var displayName by remember { mutableStateOf(account?.displayName ?: "") }
    var notificationsEnabled by remember { mutableStateOf(account?.notificationsEnabled ?: true) }
    var weeklyDigestEnabled by remember { mutableStateOf(account?.weeklyDigestEnabled ?: false) }
    var dataConsentAccepted by remember { mutableStateOf(account?.dataConsentAccepted ?: true) }

    var showSupportDialog by remember { mutableStateOf(false) }
    var showReadinessDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    LaunchedEffect(account) {
        if (account != null) {
            displayName = account.displayName
            notificationsEnabled = account.notificationsEnabled
            weeklyDigestEnabled = account.weeklyDigestEnabled
            dataConsentAccepted = account.dataConsentAccepted
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Google Account Profile Card
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            AsyncDrawableImage(
                                resId = R.drawable.img_profile_avatar,
                                contentDescription = "Account avatar",
                                contentScale = ContentScale.Crop,
                                inSampleSize = 4,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = displayName.ifBlank { "PaiseJar Saver" },
                                    style = MaterialTheme.typography.titleLarge,
                                )
                                Text(
                                    text = userEmail ?: "Google Account connected",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            OutlinedButton(
                                onClick = onSignOut,
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("profile_sign_out_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Sign out")
                            }
                        }

                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { if (it.length <= 60) displayName = it },
                            label = { Text("Display name (optional)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_display_name_input"),
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Rule & limit alerts", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "Notify when weekly or monthly caps are reached",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { notificationsEnabled = it },
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Weekly preview digest", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "Recap round-up rules and goal targets each week",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = weeklyDigestEnabled,
                                onCheckedChange = { weeklyDigestEnabled = it },
                            )
                        }

                        Button(
                            onClick = {
                                onSavePreferences(
                                    displayName,
                                    notificationsEnabled,
                                    weeklyDigestEnabled,
                                    dataConsentAccepted,
                                )
                                onShowMessage("Profile preferences saved.")
                            },
                            enabled = !state.isBusy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("profile_save_preferences_button"),
                        ) {
                            Text("Save preferences")
                        }
                    }
                }
            }

            // Partner Connection & Consent Management
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountBalance,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "Connected Providers & Consent",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }

                        StatusNoticeBanner(
                            title = "Account Aggregator & UPI Autopay: Not connected",
                            subtitle = "RBI Account Aggregator (FIU) provides consent-based read-only bank transaction visibility and cannot move money. Automated round-up debits require a separate NPCI UPI Autopay / bank transfer partner.",
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "App data & rule storage consent",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    text = "Version ${account?.consentVersion ?: "v1.0-2026"} • Toggle to revoke",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = dataConsentAccepted,
                                onCheckedChange = { next ->
                                    dataConsentAccepted = next
                                    onSavePreferences(
                                        displayName,
                                        notificationsEnabled,
                                        weeklyDigestEnabled,
                                        next,
                                    )
                                    onShowMessage(
                                        if (next) "Consent recorded." else "Consent withdrawn; partner connection stays disabled.",
                                    )
                                },
                                modifier = Modifier.testTag("profile_consent_switch"),
                            )
                        }

                        Text(
                            text = "Fees & Timing: ₹0 PaiseJar fee • Instant rule preview • ₹0 withdrawal penalty • Recovery via your signed-in Google Account.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Actions Row: Summary, Disclosures, Support, Launch Checklist
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
                        Text(
                            text = "Support, Disclosures & Readiness",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = { showSupportDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("profile_support_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SupportAgent,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Get help")
                            }
                            OutlinedButton(
                                onClick = { showReadinessDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("profile_readiness_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Checklist,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Launch checklist")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = onOpenMonthlySummary,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp),
                            ) {
                                Text("Monthly summary")
                            }
                            OutlinedButton(
                                onClick = onReplayDisclosures,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp),
                            ) {
                                Text("Review disclosures")
                            }
                        }
                    }
                }
            }

            // Recent Security & Activity Audit Log
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Account Audit Log",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            if (state.auditLogs.isEmpty()) {
                item {
                    Text(
                        text = "Audit events will appear here as rules, goals, and preferences are saved.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(state.auditLogs.take(10), key = { it.id }) { log ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = log.action,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = log.createdAt?.toDate()?.time?.let {
                                        RoundUpCalculator.formatDateTime(it)
                                    } ?: "Pending timestamp",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = log.detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            // Permanent Account Deletion
            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = { showDeleteAccountDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("profile_delete_account_button"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteForever,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Close account & delete all data")
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showSupportDialog) {
        SupportTicketDialog(
            onDismiss = { showSupportDialog = false },
            onSubmit = { topic, msg ->
                showSupportDialog = false
                onSubmitSupportTicket(topic, msg)
            },
        )
    }

    if (showReadinessDialog) {
        LaunchReadinessDialog(onDismiss = { showReadinessDialog = false })
    }

    if (showDeleteAccountDialog) {
        AccountClosureDialog(
            onConfirmDelete = {
                showDeleteAccountDialog = false
                onDeleteAccount()
            },
            onDismiss = { showDeleteAccountDialog = false },
        )
    }
}

@Composable
private fun SupportTicketDialog(
    onDismiss: () -> Unit,
    onSubmit: (topic: String, message: String) -> Unit,
) {
    var topic by remember { mutableStateOf("Rule / Calculation Question") }
    var message by remember { mutableStateOf("") }
    val canSubmit = topic.trim().isNotEmpty() && message.trim().length >= 4

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.SupportAgent, contentDescription = null) },
        title = { Text("Submit Support / Grievance Ticket") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Tickets are saved to your authenticated Firestore account (`supportTickets`) for support review. Never include bank passwords or UPI PINs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = topic,
                    onValueChange = { if (it.length <= 60) topic = it },
                    label = { Text("Topic") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { if (it.length <= 1000) message = it },
                    label = { Text("How can we help? (min 4 characters)") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("support_message_input"),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(topic.trim(), message.trim()) },
                enabled = canSubmit,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("support_submit_button"),
            ) {
                Text("Submit ticket")
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
private fun LaunchReadinessDialog(
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Info, contentDescription = null) },
        title = { Text("Store & Production Readiness") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Connected & Verified",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "• Google Sign-In via Jetpack Credential Manager + Firebase Auth\n• Cloud Firestore per-user ledger, rules, goals, audit logs & support tickets\n• Zero-trust deployed security rules & emulator verification",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "Missing Partner & Regulatory Integrations",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text = "• RBI-licensed Account Aggregator (FIU) consent & bank feed webhook\n• NPCI UPI Autopay / RBI-authorized escrow or regulated partner transfer API\n• Real-time balance lookup for Low-Balance Guard",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "Before Play Store Submission",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "• Enable Firebase App Check (Play Integrity) for release builds\n• Publish Privacy Policy, Terms, Fee Schedule & Grievance Officer URLs\n• Complete Play Console Financial Features Declaration & Data Safety form\n• Sign release AAB with Play App Signing key & add release SHA-1/SHA-256 to Firebase",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text("Close")
            }
        },
    )
}
