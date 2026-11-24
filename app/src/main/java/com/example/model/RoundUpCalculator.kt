package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToLong

object RoundUpCalculator {

    private val dateFormatThreadLocal = ThreadLocal.withInitial {
        val enIn = Locale.Builder().setLanguage("en").setRegion("IN").build()
        SimpleDateFormat("dd MMM, hh:mm a", enIn)
    }

    /**
     * Uses exact integer paise math so rounding never suffers binary floating-point drift.
     * Example:
     * - ₹180 to ₹100 step -> target ₹200, diff ₹20
     * - ₹470 to ₹100 step -> target ₹500, diff ₹30
     * - ₹500 to ₹100 step -> target ₹500, diff ₹0
     */
    fun computeRawRoundUp(amountRupees: Double, targetStep: Int): Pair<Double, Double> {
        if (amountRupees <= 0.0 || targetStep <= 0) return 0.0 to 0.0
        val amountPaise = (amountRupees * 100.0).roundToLong()
        val stepPaise = targetStep.toLong() * 100L
        val remainder = amountPaise % stepPaise
        if (remainder == 0L) {
            return (amountPaise / 100.0) to 0.0
        }
        val diffPaise = stepPaise - remainder
        val targetPaise = amountPaise + diffPaise
        return (targetPaise / 100.0) to (diffPaise / 100.0)
    }

    fun formatDateTime(epochMillis: Long): String {
        if (epochMillis <= 0L) return "Timestamp unavailable"
        val formatter = dateFormatThreadLocal.get()
        return formatter?.format(Date(epochMillis)) ?: "Unknown date"
    }

    fun evaluateTransactions(
        transactions: List<AuditableTransactionDoc>,
        rules: SavingRules,
    ): List<EvaluatedTransaction> {
        // Chronological order (oldest -> newest) ensures weekly and monthly caps apply sequentially.
        val chronological = transactions.sortedBy { it.purchaseEpochMillis }
        val seenIdempotencyKeys = mutableSetOf<String>()
        val weeklyTotals = mutableMapOf<Int, Double>()
        var currentMonthTotal = 0.0

        val results = chronological.map { tx ->
            val category = TransactionCategory.fromKey(tx.category)
            val eventKind = TransactionEventKind.fromKey(tx.eventKind)
            val instructionStatus = TransferInstructionStatus.fromKey(tx.transferInstructionStatus)
            val confirmationStatus = TransferConfirmationStatus.fromKey(tx.transferConfirmationStatus)
            val dateBucket = DateRangeBucket.fromKey(tx.dateRangeBucket)
            val dateLabel = formatDateTime(tx.purchaseEpochMillis)

            val (targetAmount, rawDiff) = computeRawRoundUp(
                amountRupees = tx.purchaseAmountRupees,
                targetStep = rules.roundUpTargetRupees,
            )

            val isDuplicate = tx.idempotencyKey.isNotBlank() && !seenIdempotencyKeys.add(tx.idempotencyKey)

            val (finalSaved, status, reason) = when {
                isDuplicate -> Triple(
                    0.0,
                    RoundUpStatus.DUPLICATE_BLOCKED,
                    "Duplicate event blocked by idempotency key (${tx.idempotencyKey})",
                )
                eventKind == TransactionEventKind.REFUND -> Triple(
                    0.0,
                    RoundUpStatus.REFUND_NO_DEBIT,
                    "Refund / credit entry — no round-up created",
                )
                eventKind == TransactionEventKind.REVERSAL -> Triple(
                    0.0,
                    RoundUpStatus.REVERSAL_NO_DEBIT,
                    "Transaction reversed — pending preview instruction cancelled",
                )
                eventKind == TransactionEventKind.DELAYED_FEED -> Triple(
                    0.0,
                    RoundUpStatus.DELAYED_PENDING,
                    "Delayed transaction feed — waiting for settled timestamp from partner",
                )
                instructionStatus == TransferInstructionStatus.FAILED_TEST -> Triple(
                    0.0,
                    RoundUpStatus.FAILED_INSTRUCTION,
                    "Simulated debit instruction failed — ₹0 deducted or saved",
                )
                tx.userExcluded -> Triple(
                    0.0,
                    RoundUpStatus.EXCLUDED_BY_USER,
                    "Excluded by you (preview instruction cancelled)",
                )
                rules.paused -> Triple(
                    0.0,
                    RoundUpStatus.PAUSED,
                    "Round-up rules are paused",
                )
                category in rules.excludedCategories -> Triple(
                    0.0,
                    RoundUpStatus.EXCLUDED_CATEGORY,
                    "Excluded category (${category.label})",
                )
                tx.merchantName in rules.excludedMerchants -> Triple(
                    0.0,
                    RoundUpStatus.EXCLUDED_MERCHANT,
                    "Merchant excluded in your rules",
                )
                tx.purchaseAmountRupees < rules.minTransactionAmountRupees -> Triple(
                    0.0,
                    RoundUpStatus.BELOW_MINIMUM,
                    "Below ${CurrencyFormatter.formatInr(rules.minTransactionAmountRupees)} minimum purchase rule",
                )
                rawDiff <= 0.0 -> Triple(
                    0.0,
                    RoundUpStatus.EXACT_MULTIPLE,
                    "Already a multiple of ${CurrencyFormatter.formatInrInt(rules.roundUpTargetRupees)}",
                )
                rawDiff > rules.maxPerPurchaseRupees -> Triple(
                    0.0,
                    RoundUpStatus.OVER_PURCHASE_LIMIT,
                    "Round-up ${CurrencyFormatter.formatInr(rawDiff)} exceeds ${CurrencyFormatter.formatInr(rules.maxPerPurchaseRupees)}/purchase limit",
                )
                tx.currentMonth -> {
                    val weekUsed = weeklyTotals[tx.weekOfMonth] ?: 0.0
                    val remainingWeek = (rules.weeklyCapRupees - weekUsed).coerceAtLeast(0.0)
                    val remainingMonth = (rules.monthlyCapRupees - currentMonthTotal).coerceAtLeast(0.0)

                    when {
                        remainingWeek <= 0.0 -> Triple(
                            0.0,
                            RoundUpStatus.WEEKLY_CAP_REACHED,
                            "Weekly cap of ${CurrencyFormatter.formatInr(rules.weeklyCapRupees)} reached",
                        )
                        remainingMonth <= 0.0 -> Triple(
                            0.0,
                            RoundUpStatus.MONTHLY_CAP_REACHED,
                            "Monthly cap of ${CurrencyFormatter.formatInr(rules.monthlyCapRupees)} reached",
                        )
                        else -> {
                            val capped = min(rawDiff, min(remainingWeek, remainingMonth))
                            val note = when {
                                capped < rawDiff && remainingWeek <= remainingMonth ->
                                    "Preview capped by remaining weekly limit (${CurrencyFormatter.formatInr(capped)} of ${CurrencyFormatter.formatInr(rawDiff)})"
                                capped < rawDiff ->
                                    "Preview capped by remaining monthly limit (${CurrencyFormatter.formatInr(capped)} of ${CurrencyFormatter.formatInr(rawDiff)})"
                                else ->
                                    "Rounded ${CurrencyFormatter.formatInr(tx.purchaseAmountRupees)} → ${CurrencyFormatter.formatInr(targetAmount)} (sandbox preview)"
                            }
                            weeklyTotals[tx.weekOfMonth] = weekUsed + capped
                            currentMonthTotal += capped
                            Triple(capped, RoundUpStatus.PREVIEW_ELIGIBLE, note)
                        }
                    }
                }
                else -> Triple(
                    rawDiff,
                    RoundUpStatus.PREVIEW_ELIGIBLE,
                    "Previous month: ${CurrencyFormatter.formatInr(tx.purchaseAmountRupees)} → ${CurrencyFormatter.formatInr(targetAmount)} (sandbox preview)",
                )
            }

            EvaluatedTransaction(
                transaction = tx,
                categoryEnum = category,
                eventKindEnum = eventKind,
                transferInstructionStatusEnum = instructionStatus,
                transferConfirmationStatusEnum = confirmationStatus,
                dateRangeBucketEnum = dateBucket,
                roundUpTargetRupees = targetAmount,
                rawDifferenceRupees = rawDiff,
                previewRoundUpRupees = finalSaved,
                status = status,
                reasonText = reason,
                dateDisplayLabel = dateLabel,
            )
        }

        // Return newest-first for timeline displays
        return results.sortedByDescending { it.transaction.purchaseEpochMillis }
    }

    fun buildConfirmedLedgerSummary(goals: List<SavingsGoalDoc>): ConfirmedLedgerSummary {
        val active = goals.filter { !it.archived }
        return ConfirmedLedgerSummary(
            availableBalanceRupees = active.sumOf { it.confirmedBalanceRupees },
            pendingTransfersRupees = active.sumOf { it.pendingTransferRupees },
            completedContributionsRupees = goals.sumOf { it.confirmedBalanceRupees },
            failedTransfersCount = goals.sumOf { it.failedTransferCount },
            providerStatus = ProviderConnectionStatus.NOT_CONNECTED,
        )
    }

    fun buildSandboxSummary(
        evaluated: List<EvaluatedTransaction>,
        monthlyGoalTargetRupees: Double,
    ): SandboxPreviewSummary {
        val currentMonthItems = evaluated.filter { it.transaction.currentMonth }
        val currentWeekItems = currentMonthItems.filter { it.dateRangeBucketEnum == DateRangeBucket.THIS_WEEK }
        val previousMonthItems = evaluated.filter { !it.transaction.currentMonth }

        val eligibleCurrentMonth = currentMonthItems.filter { it.previewRoundUpRupees > 0.0 }

        val currentWeekTotal = currentWeekItems.sumOf { it.previewRoundUpRupees }
        val currentMonthTotal = eligibleCurrentMonth.sumOf { it.previewRoundUpRupees }
        val previousMonthTotal = previousMonthItems.sumOf { it.previewRoundUpRupees }
        val count = eligibleCurrentMonth.size
        val average = if (count > 0) currentMonthTotal / count else 0.0

        val weeklySlices = (1..4).map { week ->
            val items = currentMonthItems.filter { it.transaction.weekOfMonth == week }
            WeeklySlice(
                weekLabel = "Week $week",
                amountRupees = items.sumOf { it.previewRoundUpRupees },
                transactionCount = items.count { it.previewRoundUpRupees > 0.0 },
            )
        }

        val categoryBreakdown = TransactionCategory.entries.mapNotNull { cat ->
            val catItems = eligibleCurrentMonth.filter { it.categoryEnum == cat }
            val sum = catItems.sumOf { it.previewRoundUpRupees }
            if (sum > 0.0) {
                CategoryBreakdownItem(
                    category = cat,
                    previewSavedRupees = sum,
                    purchaseCount = catItems.size,
                )
            } else {
                null
            }
        }.sortedByDescending { it.previewSavedRupees }

        return SandboxPreviewSummary(
            currentWeekPreviewRupees = currentWeekTotal,
            currentMonthPreviewRupees = currentMonthTotal,
            previousMonthPreviewRupees = previousMonthTotal,
            eligibleRoundUpCount = count,
            averageRoundUpRupees = average,
            weeklyBreakdown = weeklySlices,
            categoryBreakdown = categoryBreakdown,
        )
    }
}
