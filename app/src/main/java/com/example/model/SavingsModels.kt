package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

enum class TransactionCategory(val label: String) {
    CAFE("Coffee & Cafes"),
    FOOD_DELIVERY("Food Delivery"),
    GROCERY("Groceries"),
    SHOPPING("Shopping"),
    TRANSPORT("Transport"),
    SUBSCRIPTIONS("Subscriptions"),
    UNCLEAR("Unclear / Raw POS");

    companion object {
        fun fromKey(key: String?): TransactionCategory {
            return entries.firstOrNull { it.name == key } ?: UNCLEAR
        }
    }
}

enum class DateRangeBucket {
    THIS_WEEK,
    THIS_MONTH,
    PREVIOUS_MONTH;

    companion object {
        fun fromKey(key: String?): DateRangeBucket {
            return entries.firstOrNull { it.name == key } ?: THIS_MONTH
        }
    }
}

enum class DateRangeFilter(val label: String) {
    ALL("All dates"),
    THIS_WEEK("This week"),
    THIS_MONTH("This month"),
    PREVIOUS_MONTH("Last month"),
}

enum class ActivityFilter(val label: String) {
    ALL("All"),
    ELIGIBLE("Calculated"),
    EXCLUDED("Excluded"),
    NEEDS_REVIEW("Issues & Edge Cases"),
}

enum class TransactionEventKind(val label: String) {
    PURCHASE("Purchase"),
    REFUND("Refund"),
    REVERSAL("Reversal"),
    DELAYED_FEED("Delayed feed");

    companion object {
        fun fromKey(key: String?): TransactionEventKind {
            return entries.firstOrNull { it.name == key } ?: PURCHASE
        }
    }
}

enum class TransferInstructionStatus(val label: String) {
    NOT_CREATED("Not created"),
    SIMULATED_PREVIEW("Sandbox preview only"),
    BLOCKED_NO_PARTNER("Blocked — no bank partner"),
    FAILED_TEST("Failed test instruction"),
    CANCELLED("Cancelled by user");

    companion object {
        fun fromKey(key: String?): TransferInstructionStatus {
            return entries.firstOrNull { it.name == key } ?: NOT_CREATED
        }
    }
}

enum class TransferConfirmationStatus(val label: String) {
    UNCONFIRMED_NO_PARTNER("Unconfirmed (no partner)"),
    NONE("No transfer sent");

    companion object {
        fun fromKey(key: String?): TransferConfirmationStatus {
            return entries.firstOrNull { it.name == key } ?: UNCONFIRMED_NO_PARTNER
        }
    }
}

enum class ProviderConnectionStatus(val label: String) {
    NOT_CONNECTED("Not connected");

    companion object {
        fun fromKey(key: String?): ProviderConnectionStatus {
            return entries.firstOrNull { it.name == key } ?: NOT_CONNECTED
        }
    }
}

enum class RoundUpStatus {
    PREVIEW_ELIGIBLE,
    EXACT_MULTIPLE,
    BELOW_MINIMUM,
    OVER_PURCHASE_LIMIT,
    WEEKLY_CAP_REACHED,
    MONTHLY_CAP_REACHED,
    EXCLUDED_CATEGORY,
    EXCLUDED_MERCHANT,
    EXCLUDED_BY_USER,
    PAUSED,
    REFUND_NO_DEBIT,
    REVERSAL_NO_DEBIT,
    DELAYED_PENDING,
    FAILED_INSTRUCTION,
    DUPLICATE_BLOCKED,
}

@Keep
@IgnoreExtraProperties
data class UserAccountDoc(
    val userId: String = "",
    val displayName: String = "",
    val dataConsentAccepted: Boolean = false,
    val consentVersion: String = "v1.0-2026",
    val providerConnectionStatus: String = "NOT_CONNECTED",
    val notificationsEnabled: Boolean = true,
    val weeklyDigestEnabled: Boolean = false,
    val roundUpTargetRupees: Int = 100,
    val maxPerPurchaseRupees: Double = 50.0,
    val weeklyCapRupees: Double = 400.0,
    val monthlyCapRupees: Double = 1500.0,
    val minTransactionAmountRupees: Double = 50.0,
    val excludedCategories: List<String> = listOf(TransactionCategory.SUBSCRIPTIONS.name),
    val excludedMerchants: List<String> = emptyList(),
    val paused: Boolean = false,
    val selectedGoalId: String = "goal_emergency",
    val monthlyGoalTargetRupees: Double = 1500.0,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class SavingsGoalDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val targetAmountRupees: Double = 5000.0,
    val confirmedBalanceRupees: Double = 0.0,
    val pendingTransferRupees: Double = 0.0,
    val failedTransferCount: Int = 0,
    val targetDateLabel: String? = null,
    val iconKey: String = "emergency",
    val archived: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class AuditableTransactionDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val idempotencyKey: String = "",
    val sourceType: String = "SANDBOX_PREVIEW",
    val merchantName: String = "",
    val rawMerchantLabel: String = "",
    val description: String = "",
    val category: String = TransactionCategory.CAFE.name,
    val purchaseAmountRupees: Double = 0.0,
    val purchaseEpochMillis: Long = 0L,
    val weekOfMonth: Int = 1,
    val currentMonth: Boolean = true,
    val dateRangeBucket: String = DateRangeBucket.THIS_WEEK.name,
    val eventKind: String = TransactionEventKind.PURCHASE.name,
    val transferInstructionStatus: String = TransferInstructionStatus.SIMULATED_PREVIEW.name,
    val transferConfirmationStatus: String = TransferConfirmationStatus.UNCONFIRMED_NO_PARTNER.name,
    val userExcluded: Boolean = false,
    val providerReference: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class AccountAuditLogDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val action: String = "",
    val detail: String = "",
    val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class SupportTicketDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val topic: String = "",
    val message: String = "",
    val status: String = "OPEN",
    val createdAt: Timestamp? = null,
)

data class SavingRules(
    val roundUpTargetRupees: Int = 100,
    val maxPerPurchaseRupees: Double = 50.0,
    val weeklyCapRupees: Double = 400.0,
    val monthlyCapRupees: Double = 1500.0,
    val minTransactionAmountRupees: Double = 50.0,
    val excludedCategories: Set<TransactionCategory> = setOf(TransactionCategory.SUBSCRIPTIONS),
    val excludedMerchants: Set<String> = emptySet(),
    val paused: Boolean = false,
) {
    companion object {
        fun fromUserAccount(doc: UserAccountDoc): SavingRules {
            return SavingRules(
                roundUpTargetRupees = doc.roundUpTargetRupees.coerceIn(10, 500),
                maxPerPurchaseRupees = doc.maxPerPurchaseRupees.coerceIn(10.0, 500.0),
                weeklyCapRupees = doc.weeklyCapRupees.coerceIn(50.0, 10000.0),
                monthlyCapRupees = doc.monthlyCapRupees.coerceIn(doc.weeklyCapRupees, 50000.0),
                minTransactionAmountRupees = doc.minTransactionAmountRupees.coerceIn(0.0, 1000.0),
                excludedCategories = doc.excludedCategories
                    .mapNotNull { key -> TransactionCategory.entries.firstOrNull { it.name == key } }
                    .toSet(),
                excludedMerchants = doc.excludedMerchants.toSet(),
                paused = doc.paused,
            )
        }
    }
}

data class EvaluatedTransaction(
    val transaction: AuditableTransactionDoc,
    val categoryEnum: TransactionCategory,
    val eventKindEnum: TransactionEventKind,
    val transferInstructionStatusEnum: TransferInstructionStatus,
    val transferConfirmationStatusEnum: TransferConfirmationStatus,
    val dateRangeBucketEnum: DateRangeBucket,
    val roundUpTargetRupees: Double,
    val rawDifferenceRupees: Double,
    val previewRoundUpRupees: Double,
    val status: RoundUpStatus,
    val reasonText: String,
    val dateDisplayLabel: String,
)

data class WeeklySlice(
    val weekLabel: String,
    val amountRupees: Double,
    val transactionCount: Int,
)

data class CategoryBreakdownItem(
    val category: TransactionCategory,
    val previewSavedRupees: Double,
    val purchaseCount: Int,
)

data class ConfirmedLedgerSummary(
    val availableBalanceRupees: Double = 0.0,
    val pendingTransfersRupees: Double = 0.0,
    val completedContributionsRupees: Double = 0.0,
    val failedTransfersCount: Int = 0,
    val providerStatus: ProviderConnectionStatus = ProviderConnectionStatus.NOT_CONNECTED,
)

data class SandboxPreviewSummary(
    val currentWeekPreviewRupees: Double = 0.0,
    val currentMonthPreviewRupees: Double = 0.0,
    val previousMonthPreviewRupees: Double = 0.0,
    val eligibleRoundUpCount: Int = 0,
    val averageRoundUpRupees: Double = 0.0,
    val weeklyBreakdown: List<WeeklySlice> = emptyList(),
    val categoryBreakdown: List<CategoryBreakdownItem> = emptyList(),
)

data class PendingExclusionAction(
    val transactionId: String,
    val previousExcluded: Boolean,
    val previousInstructionStatus: String,
    val merchantName: String,
)

data class PaiseJarUiState(
    val isLoadingAccount: Boolean = true,
    val userAccount: UserAccountDoc? = null,
    val rules: SavingRules = SavingRules(),
    val goals: List<SavingsGoalDoc> = emptyList(),
    val selectedGoal: SavingsGoalDoc? = null,
    val rawTransactions: List<AuditableTransactionDoc> = emptyList(),
    val evaluatedTransactions: List<EvaluatedTransaction> = emptyList(),
    val auditLogs: List<AccountAuditLogDoc> = emptyList(),
    val confirmedLedger: ConfirmedLedgerSummary = ConfirmedLedgerSummary(),
    val sandboxSummary: SandboxPreviewSummary = SandboxPreviewSummary(),
    val searchQuery: String = "",
    val dateRangeFilter: DateRangeFilter = DateRangeFilter.ALL,
    val activityFilter: ActivityFilter = ActivityFilter.ALL,
    val showArchivedGoals: Boolean = false,
    val pendingExclusionUndo: PendingExclusionAction? = null,
    val errorMessage: String? = null,
    val isBusy: Boolean = false,
) {
    val activeGoals: List<SavingsGoalDoc>
        get() = goals.filter { !it.archived }

    val archivedGoals: List<SavingsGoalDoc>
        get() = goals.filter { it.archived }
}
