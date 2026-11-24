package com.example.data

import com.example.model.AccountAuditLogDoc
import com.example.model.AuditableTransactionDoc
import com.example.model.DateRangeBucket
import com.example.model.SandboxScenarios
import com.example.model.SavingRules
import com.example.model.SavingsGoalDoc
import com.example.model.TransactionCategory
import com.example.model.TransactionEventKind
import com.example.model.TransferConfirmationStatus
import com.example.model.TransferInstructionStatus
import com.example.model.UserAccountDoc
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class PaiseJarRepository(
    private val firestore: FirebaseFirestore,
) {
    private val timestampEstimate = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE

    private fun userDocRef(userId: String) =
        firestore.collection("users").document(userId)

    private fun goalsCol(userId: String) =
        userDocRef(userId).collection("goals")

    private fun transactionsCol(userId: String) =
        userDocRef(userId).collection("transactions")

    private fun auditLogsCol(userId: String) =
        userDocRef(userId).collection("auditLogs")

    private fun supportTicketsCol(userId: String) =
        userDocRef(userId).collection("supportTickets")

    fun observeUserAccount(userId: String): Flow<UserAccountDoc?> {
        val path = "users/$userId"
        return userDocRef(userId)
            .snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) {
                    null
                } else {
                    snapshot.toObject(UserAccountDoc::class.java, timestampEstimate)
                }
            }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.GET, path)
            }
    }

    fun observeGoals(userId: String): Flow<List<SavingsGoalDoc>> {
        val path = "users/$userId/goals"
        return goalsCol(userId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.documents.mapNotNull { doc ->
                    doc.toObject(SavingsGoalDoc::class.java, timestampEstimate)
                }
            }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, path)
            }
    }

    fun observeTransactions(userId: String): Flow<List<AuditableTransactionDoc>> {
        val path = "users/$userId/transactions"
        return transactionsCol(userId)
            .orderBy("purchaseEpochMillis", Query.Direction.DESCENDING)
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.documents.mapNotNull { doc ->
                    doc.toObject(AuditableTransactionDoc::class.java, timestampEstimate)
                }
            }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, path)
            }
    }

    fun observeAuditLogs(userId: String): Flow<List<AccountAuditLogDoc>> {
        val path = "users/$userId/auditLogs"
        return auditLogsCol(userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(40)
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.documents.mapNotNull { doc ->
                    doc.toObject(AccountAuditLogDoc::class.java, timestampEstimate)
                }
            }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, path)
            }
    }

    suspend fun ensureUserAccountInitialized(
        userId: String,
        displayName: String,
        consentAccepted: Boolean,
    ) {
        val path = "users/$userId"
        try {
            val existing = userDocRef(userId).get().await()
            if (!existing.exists()) {
                val sanitizedName = displayName.trim().take(60)
                val batch = firestore.batch()
                val userPayload = mapOf(
                    "userId" to userId,
                    "displayName" to sanitizedName,
                    "dataConsentAccepted" to consentAccepted,
                    "consentVersion" to SandboxScenarios.CONSENT_VERSION,
                    "providerConnectionStatus" to "NOT_CONNECTED",
                    "notificationsEnabled" to true,
                    "weeklyDigestEnabled" to false,
                    "roundUpTargetRupees" to 100,
                    "maxPerPurchaseRupees" to 50.0,
                    "weeklyCapRupees" to 400.0,
                    "monthlyCapRupees" to 1500.0,
                    "minTransactionAmountRupees" to 50.0,
                    "excludedCategories" to listOf(TransactionCategory.SUBSCRIPTIONS.name),
                    "excludedMerchants" to emptyList<String>(),
                    "paused" to false,
                    "selectedGoalId" to "goal_emergency",
                    "monthlyGoalTargetRupees" to 1500.0,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                batch.set(userDocRef(userId), userPayload)

                // Create clean starter goals with ₹0 confirmed balance (no fake transactions added automatically)
                SandboxScenarios.starterGoals(userId).forEach { goal ->
                    val goalPayload = mutableMapOf<String, Any?>(
                        "userId" to userId,
                        "name" to goal.name,
                        "targetAmountRupees" to goal.targetAmountRupees,
                        "confirmedBalanceRupees" to 0.0,
                        "pendingTransferRupees" to 0.0,
                        "failedTransferCount" to 0,
                        "targetDateLabel" to goal.targetDateLabel,
                        "iconKey" to goal.iconKey,
                        "archived" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ).filterValues { it != null }
                    batch.set(goalsCol(userId).document(goal.id), goalPayload)
                }

                val logRef = auditLogsCol(userId).document()
                batch.set(
                    logRef,
                    mapOf(
                        "userId" to userId,
                        "action" to "ACCOUNT_CREATED",
                        "detail" to "Initialized PaiseJar account with consent ${SandboxScenarios.CONSENT_VERSION} and ₹0 confirmed balance.",
                        "createdAt" to FieldValue.serverTimestamp(),
                    ),
                )
                batch.commit().await()
            } else if (consentAccepted) {
                val currentConsent = existing.getBoolean("dataConsentAccepted") ?: false
                if (!currentConsent) {
                    userDocRef(userId).update(
                        mapOf(
                            "dataConsentAccepted" to true,
                            "consentVersion" to SandboxScenarios.CONSENT_VERSION,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        ),
                    ).await()
                    appendAuditLog(
                        userId = userId,
                        action = "CONSENT_RECORDED",
                        detail = "Accepted privacy & consent disclosure (${SandboxScenarios.CONSENT_VERSION}).",
                    )
                }
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
        }
    }

    suspend fun saveRules(
        userId: String,
        rules: SavingRules,
    ) {
        val path = "users/$userId"
        val validatedStep = rules.roundUpTargetRupees.coerceIn(10, 500).let { (it / 10) * 10 }.coerceAtLeast(10)
        val validatedMaxPurchase = rules.maxPerPurchaseRupees.coerceIn(10.0, 500.0)
        val validatedWeeklyCap = rules.weeklyCapRupees.coerceIn(50.0, 10000.0)
        val validatedMonthlyCap = rules.monthlyCapRupees.coerceIn(validatedWeeklyCap, 50000.0)
        val validatedMinPurchase = rules.minTransactionAmountRupees.coerceIn(0.0, 1000.0)

        try {
            userDocRef(userId).update(
                mapOf(
                    "roundUpTargetRupees" to validatedStep,
                    "maxPerPurchaseRupees" to validatedMaxPurchase,
                    "weeklyCapRupees" to validatedWeeklyCap,
                    "monthlyCapRupees" to validatedMonthlyCap,
                    "minTransactionAmountRupees" to validatedMinPurchase,
                    "excludedCategories" to rules.excludedCategories.map { it.name }.take(10),
                    "excludedMerchants" to rules.excludedMerchants.map { it.take(60) }.take(30),
                    "paused" to rules.paused,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
            appendAuditLog(
                userId = userId,
                action = "RULES_UPDATED",
                detail = "Target ₹$validatedStep, max ₹${validatedMaxPurchase.toInt()}/tx, weekly cap ₹${validatedWeeklyCap.toInt()}, monthly cap ₹${validatedMonthlyCap.toInt()}.",
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
        }
    }

    suspend fun setPaused(
        userId: String,
        paused: Boolean,
    ) {
        val path = "users/$userId"
        try {
            userDocRef(userId).update(
                mapOf(
                    "paused" to paused,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
            appendAuditLog(
                userId = userId,
                action = if (paused) "RULES_PAUSED" else "RULES_RESUMED",
                detail = if (paused) "Round-up rule calculation paused by user." else "Round-up rule calculation resumed by user.",
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
        }
    }

    suspend fun updateProfilePreferences(
        userId: String,
        displayName: String,
        notificationsEnabled: Boolean,
        weeklyDigestEnabled: Boolean,
        dataConsentAccepted: Boolean,
    ) {
        val path = "users/$userId"
        try {
            val cleanedName = displayName.trim().take(60)
            userDocRef(userId).update(
                mapOf(
                    "displayName" to cleanedName,
                    "notificationsEnabled" to notificationsEnabled,
                    "weeklyDigestEnabled" to weeklyDigestEnabled,
                    "dataConsentAccepted" to dataConsentAccepted,
                    "consentVersion" to SandboxScenarios.CONSENT_VERSION,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
            appendAuditLog(
                userId = userId,
                action = if (dataConsentAccepted) "PROFILE_UPDATED" else "CONSENT_WITHDRAWN",
                detail = if (dataConsentAccepted) {
                    "Updated display preferences and alert toggles."
                } else {
                    "User revoked consent; real-money partner linkage remains disabled."
                },
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
        }
    }

    suspend fun selectGoalForRoundUps(
        userId: String,
        goalId: String,
    ) {
        val path = "users/$userId"
        try {
            userDocRef(userId).update(
                mapOf(
                    "selectedGoalId" to goalId.take(128),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
            appendAuditLog(
                userId = userId,
                action = "GOAL_SELECTED",
                detail = "Selected goal $goalId for future round-up allocation.",
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
        }
    }

    suspend fun createOrUpdateGoal(
        userId: String,
        existingGoalId: String?,
        name: String,
        targetAmountRupees: Double,
        targetDateLabel: String?,
        iconKey: String,
    ) {
        val cleanedName = name.trim().take(60)
        val clampedTarget = targetAmountRupees.coerceIn(100.0, 10_000_000.0)
        val cleanedDate = targetDateLabel?.trim()?.takeIf { it.isNotEmpty() }?.take(30)
        val cleanedIcon = iconKey.trim().ifEmpty { "emergency" }.take(20)

        val goalDocRef = if (existingGoalId.isNullOrBlank()) {
            goalsCol(userId).document()
        } else {
            goalsCol(userId).document(existingGoalId)
        }
        val path = "users/$userId/goals/${goalDocRef.id}"

        try {
            if (existingGoalId.isNullOrBlank()) {
                val payload = mutableMapOf<String, Any?>(
                    "userId" to userId,
                    "name" to cleanedName,
                    "targetAmountRupees" to clampedTarget,
                    "confirmedBalanceRupees" to 0.0,
                    "pendingTransferRupees" to 0.0,
                    "failedTransferCount" to 0,
                    "targetDateLabel" to cleanedDate,
                    "iconKey" to cleanedIcon,
                    "archived" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ).filterValues { it != null }
                goalDocRef.set(payload).await()
                appendAuditLog(
                    userId = userId,
                    action = "GOAL_CREATED",
                    detail = "Created goal '$cleanedName' (target ₹${clampedTarget.toInt()}).",
                )
            } else {
                val updatePayload = mutableMapOf<String, Any>(
                    "name" to cleanedName,
                    "targetAmountRupees" to clampedTarget,
                    "iconKey" to cleanedIcon,
                    "targetDateLabel" to (cleanedDate ?: FieldValue.delete()),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                goalDocRef.update(updatePayload).await()
                appendAuditLog(
                    userId = userId,
                    action = "GOAL_UPDATED",
                    detail = "Updated goal '$cleanedName' (target ₹${clampedTarget.toInt()}).",
                )
            }
        } catch (e: Exception) {
            handleFirestoreError(
                e,
                if (existingGoalId.isNullOrBlank()) OperationType.CREATE else OperationType.UPDATE,
                path,
            )
        }
    }

    suspend fun setGoalArchived(
        userId: String,
        goalId: String,
        archived: Boolean,
    ) {
        val path = "users/$userId/goals/$goalId"
        try {
            goalsCol(userId).document(goalId).update(
                mapOf(
                    "archived" to archived,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
            appendAuditLog(
                userId = userId,
                action = if (archived) "GOAL_ARCHIVED" else "GOAL_UNARCHIVED",
                detail = if (archived) "Archived goal $goalId." else "Restored goal $goalId to active list.",
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
        }
    }

    /**
     * Adds an explicit sandbox calculation scenario transaction, checking for duplicate idempotencyKey.
     * Returns true if added, false if blocked by duplicate idempotencyKey.
     */
    suspend fun addSandboxTransaction(
        userId: String,
        merchantName: String,
        rawMerchantLabel: String,
        description: String,
        category: TransactionCategory,
        purchaseAmountRupees: Double,
        eventKind: TransactionEventKind,
        instructionStatus: TransferInstructionStatus,
        idempotencyKey: String,
    ): Boolean {
        val path = "users/$userId/transactions"
        try {
            val cleanedKey = idempotencyKey.trim().take(128)
            val duplicateQuery = transactionsCol(userId)
                .whereEqualTo("idempotencyKey", cleanedKey)
                .limit(1)
                .get()
                .await()
            if (!duplicateQuery.isEmpty) {
                appendAuditLog(
                    userId = userId,
                    action = "DUPLICATE_BLOCKED",
                    detail = "Prevented duplicate entry for idempotency key $cleanedKey.",
                )
                return false
            }

            val docRef = transactionsCol(userId).document()
            val nowMs = System.currentTimeMillis()
            val payload = mapOf(
                "userId" to userId,
                "idempotencyKey" to cleanedKey,
                "sourceType" to "SANDBOX_PREVIEW",
                "merchantName" to merchantName.trim().take(80),
                "rawMerchantLabel" to rawMerchantLabel.trim().take(100),
                "description" to description.trim().take(120),
                "category" to category.name,
                "purchaseAmountRupees" to purchaseAmountRupees.coerceIn(0.01, 1_000_000.0),
                "purchaseEpochMillis" to nowMs,
                "weekOfMonth" to 4,
                "currentMonth" to true,
                "dateRangeBucket" to DateRangeBucket.THIS_WEEK.name,
                "eventKind" to eventKind.name,
                "transferInstructionStatus" to instructionStatus.name,
                "transferConfirmationStatus" to if (instructionStatus == TransferInstructionStatus.SIMULATED_PREVIEW) {
                    TransferConfirmationStatus.UNCONFIRMED_NO_PARTNER.name
                } else {
                    TransferConfirmationStatus.NONE.name
                },
                "userExcluded" to false,
                "providerReference" to "SBX-${docRef.id.take(8)}",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            docRef.set(payload).await()
            appendAuditLog(
                userId = userId,
                action = "SANDBOX_TX_ADDED",
                detail = "Added sandbox event '${merchantName.take(30)}' (₹${purchaseAmountRupees.toInt()}).",
            )
            return true
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
        }
    }

    suspend fun loadSandboxScenarioPack(userId: String) {
        val path = "users/$userId/transactions"
        try {
            val scenarios = SandboxScenarios.sandboxScenarioTransactions(userId)
            val batch = firestore.batch()
            scenarios.forEach { tx ->
                val ref = transactionsCol(userId).document(tx.id)
                batch.set(
                    ref,
                    mapOf(
                        "userId" to userId,
                        "idempotencyKey" to tx.idempotencyKey,
                        "sourceType" to tx.sourceType,
                        "merchantName" to tx.merchantName,
                        "rawMerchantLabel" to tx.rawMerchantLabel,
                        "description" to tx.description,
                        "category" to tx.category,
                        "purchaseAmountRupees" to tx.purchaseAmountRupees,
                        "purchaseEpochMillis" to tx.purchaseEpochMillis,
                        "weekOfMonth" to tx.weekOfMonth,
                        "currentMonth" to tx.currentMonth,
                        "dateRangeBucket" to tx.dateRangeBucket,
                        "eventKind" to tx.eventKind,
                        "transferInstructionStatus" to tx.transferInstructionStatus,
                        "transferConfirmationStatus" to tx.transferConfirmationStatus,
                        "userExcluded" to tx.userExcluded,
                        "providerReference" to tx.providerReference,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
            }
            val logRef = auditLogsCol(userId).document()
            batch.set(
                logRef,
                mapOf(
                    "userId" to userId,
                    "action" to "SANDBOX_PACK_LOADED",
                    "detail" to "Loaded ${scenarios.size} sandbox calculation test scenarios (no real money).",
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            )
            batch.commit().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
        }
    }

    suspend fun clearSandboxTransactions(userId: String) {
        val path = "users/$userId/transactions"
        try {
            val snap = transactionsCol(userId).get().await()
            if (!snap.isEmpty) {
                snap.documents.chunked(400).forEach { chunk ->
                    val batch = firestore.batch()
                    chunk.forEach { doc -> batch.delete(doc.reference) }
                    batch.commit().await()
                }
            }
            appendAuditLog(
                userId = userId,
                action = "SANDBOX_TX_CLEARED",
                detail = "Cleared all sandbox calculation preview transactions.",
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
        }
    }

    suspend fun setTransactionExcluded(
        userId: String,
        txId: String,
        excluded: Boolean,
        instructionStatus: String,
    ) {
        val path = "users/$userId/transactions/$txId"
        try {
            transactionsCol(userId).document(txId).update(
                mapOf(
                    "userExcluded" to excluded,
                    "transferInstructionStatus" to instructionStatus,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
            appendAuditLog(
                userId = userId,
                action = if (excluded) "TX_EXCLUDED" else "TX_REINCLUDED",
                detail = if (excluded) {
                    "Excluded sandbox tx $txId (cancelled preview instruction)."
                } else {
                    "Re-included sandbox tx $txId."
                },
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
        }
    }

    suspend fun submitSupportTicket(
        userId: String,
        topic: String,
        message: String,
    ): String {
        val path = "users/$userId/supportTickets"
        try {
            val ref = supportTicketsCol(userId).document()
            ref.set(
                mapOf(
                    "userId" to userId,
                    "topic" to topic.trim().take(60),
                    "message" to message.trim().take(1000),
                    "status" to "OPEN",
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
            appendAuditLog(
                userId = userId,
                action = "SUPPORT_TICKET_OPENED",
                detail = "Submitted support ticket ${ref.id.take(8)} (${topic.take(30)}).",
            )
            return ref.id
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
        }
    }

    suspend fun deleteAccountAndAllData(userId: String) {
        val path = "users/$userId"
        try {
            val subCollections = listOf(
                goalsCol(userId),
                transactionsCol(userId),
                auditLogsCol(userId),
                supportTicketsCol(userId),
            )
            for (col in subCollections) {
                val snap = col.get().await()
                snap.documents.chunked(400).forEach { chunk ->
                    val batch = firestore.batch()
                    chunk.forEach { doc -> batch.delete(doc.reference) }
                    batch.commit().await()
                }
            }
            userDocRef(userId).delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
        }
    }

    private suspend fun appendAuditLog(
        userId: String,
        action: String,
        detail: String,
    ) {
        val ref = auditLogsCol(userId).document()
        ref.set(
            mapOf(
                "userId" to userId,
                "action" to action.take(60),
                "detail" to detail.take(200),
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }
}
