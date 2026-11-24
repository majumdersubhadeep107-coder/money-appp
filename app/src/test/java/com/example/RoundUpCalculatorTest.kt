package com.example

import com.example.model.AuditableTransactionDoc
import com.example.model.CurrencyFormatter
import com.example.model.DateRangeBucket
import com.example.model.RoundUpCalculator
import com.example.model.RoundUpStatus
import com.example.model.SandboxScenarios
import com.example.model.SavingRules
import com.example.model.TransactionCategory
import com.example.model.TransactionEventKind
import com.example.model.TransferInstructionStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class RoundUpCalculatorTest {

    @Test
    fun `formatInr omits paise for whole rupees and keeps 2 decimals when fractional`() {
        assertEquals("₹1,250", CurrencyFormatter.formatInr(1250.0))
        assertEquals("₹1,250.50", CurrencyFormatter.formatInr(1250.5))
        assertEquals("₹1,25,000", CurrencyFormatter.formatInr(125000.0))
        assertEquals("₹20", CurrencyFormatter.formatInr(20.0))
        assertEquals("₹17.50", CurrencyFormatter.formatInr(17.5))
    }

    @Test
    fun `computeRawRoundUp handles standard, paise, and exact multiple cases`() {
        val (t1, d1) = RoundUpCalculator.computeRawRoundUp(180.0, 100)
        assertEquals(200.0, t1, 0.001)
        assertEquals(20.0, d1, 0.001)

        val (t2, d2) = RoundUpCalculator.computeRawRoundUp(470.0, 100)
        assertEquals(500.0, t2, 0.001)
        assertEquals(30.0, d2, 0.001)

        val (t3, d3) = RoundUpCalculator.computeRawRoundUp(500.0, 100)
        assertEquals(500.0, t3, 0.001)
        assertEquals(0.0, d3, 0.001)

        val (t4, d4) = RoundUpCalculator.computeRawRoundUp(182.5, 100)
        assertEquals(200.0, t4, 0.001)
        assertEquals(17.5, d4, 0.001)
    }

    @Test
    fun `evaluateTransactions enforces weekly cap, monthly cap, and per-purchase cap`() {
        val txs = listOf(
            AuditableTransactionDoc(
                id = "1",
                idempotencyKey = "k1",
                merchantName = "Cafe",
                category = TransactionCategory.CAFE.name,
                purchaseAmountRupees = 180.0, // raw 20
                purchaseEpochMillis = 1_000L,
                weekOfMonth = 1,
                currentMonth = true,
            ),
            AuditableTransactionDoc(
                id = "2",
                idempotencyKey = "k2",
                merchantName = "Metro",
                category = TransactionCategory.TRANSPORT.name,
                purchaseAmountRupees = 135.0, // raw 65 -> exceeds 50 limit
                purchaseEpochMillis = 2_000L,
                weekOfMonth = 1,
                currentMonth = true,
            ),
            AuditableTransactionDoc(
                id = "3",
                idempotencyKey = "k3",
                merchantName = "Swiggy",
                category = TransactionCategory.FOOD_DELIVERY.name,
                purchaseAmountRupees = 470.0, // raw 30 -> capped at remaining 10 of 30 weekly cap
                purchaseEpochMillis = 3_000L,
                weekOfMonth = 1,
                currentMonth = true,
            ),
            AuditableTransactionDoc(
                id = "4",
                idempotencyKey = "k4",
                merchantName = "Bookstore",
                category = TransactionCategory.SHOPPING.name,
                purchaseAmountRupees = 460.0, // raw 40 -> capped at remaining 20 of 50 monthly cap
                purchaseEpochMillis = 4_000L,
                weekOfMonth = 2,
                currentMonth = true,
            ),
        )
        val rules = SavingRules(
            roundUpTargetRupees = 100,
            maxPerPurchaseRupees = 50.0,
            weeklyCapRupees = 30.0,
            monthlyCapRupees = 50.0,
            minTransactionAmountRupees = 0.0,
            excludedCategories = emptySet(),
        )
        val byId = RoundUpCalculator.evaluateTransactions(txs, rules).associateBy { it.transaction.id }

        assertEquals(20.0, byId.getValue("1").previewRoundUpRupees, 0.001)
        assertEquals(0.0, byId.getValue("2").previewRoundUpRupees, 0.001)
        assertEquals(RoundUpStatus.OVER_PURCHASE_LIMIT, byId.getValue("2").status)
        assertEquals(10.0, byId.getValue("3").previewRoundUpRupees, 0.001)
        assertEquals(20.0, byId.getValue("4").previewRoundUpRupees, 0.001)
    }

    @Test
    fun `refund, reversal, delayed feed, failed instruction, and duplicates produce zero round-up`() {
        val txs = listOf(
            AuditableTransactionDoc(
                id = "r1",
                idempotencyKey = "dup_key",
                merchantName = "Blue Tokai",
                purchaseAmountRupees = 180.0,
                purchaseEpochMillis = 1_000L,
                currentMonth = true,
            ),
            AuditableTransactionDoc(
                id = "r2",
                idempotencyKey = "dup_key", // duplicate
                merchantName = "Blue Tokai",
                purchaseAmountRupees = 180.0,
                purchaseEpochMillis = 1_500L,
                currentMonth = true,
            ),
            AuditableTransactionDoc(
                id = "r3",
                idempotencyKey = "k_refund",
                merchantName = "Zomato",
                purchaseAmountRupees = 320.0,
                purchaseEpochMillis = 2_000L,
                eventKind = TransactionEventKind.REFUND.name,
            ),
            AuditableTransactionDoc(
                id = "r4",
                idempotencyKey = "k_reversal",
                merchantName = "Nykaa",
                purchaseAmountRupees = 410.0,
                purchaseEpochMillis = 3_000L,
                eventKind = TransactionEventKind.REVERSAL.name,
            ),
            AuditableTransactionDoc(
                id = "r5",
                idempotencyKey = "k_delayed",
                merchantName = "Zepto",
                purchaseAmountRupees = 225.0,
                purchaseEpochMillis = 4_000L,
                eventKind = TransactionEventKind.DELAYED_FEED.name,
            ),
            AuditableTransactionDoc(
                id = "r6",
                idempotencyKey = "k_failed",
                merchantName = "Cafe Test",
                purchaseAmountRupees = 285.0,
                purchaseEpochMillis = 5_000L,
                transferInstructionStatus = TransferInstructionStatus.FAILED_TEST.name,
            ),
        )
        val byId = RoundUpCalculator.evaluateTransactions(txs, SavingRules()).associateBy { it.transaction.id }

        assertEquals(20.0, byId.getValue("r1").previewRoundUpRupees, 0.001)
        assertEquals(RoundUpStatus.DUPLICATE_BLOCKED, byId.getValue("r2").status)
        assertEquals(0.0, byId.getValue("r2").previewRoundUpRupees, 0.001)
        assertEquals(RoundUpStatus.REFUND_NO_DEBIT, byId.getValue("r3").status)
        assertEquals(RoundUpStatus.REVERSAL_NO_DEBIT, byId.getValue("r4").status)
        assertEquals(RoundUpStatus.DELAYED_PENDING, byId.getValue("r5").status)
        assertEquals(RoundUpStatus.FAILED_INSTRUCTION, byId.getValue("r6").status)
    }

    @Test
    fun `sandbox scenario pack handles mixed real-world edge cases cleanly`() {
        val txs = SandboxScenarios.sandboxScenarioTransactions("user_test", referenceNowMillis = 1_700_000_000_000L)
        val evaluated = RoundUpCalculator.evaluateTransactions(txs, SavingRules())
        val summary = RoundUpCalculator.buildSandboxSummary(evaluated, 1500.0)

        // Eligible this month under default ₹50/purchase limit: Blue Tokai (₹20) + Swiggy (₹30) + POS*99182 (₹35) = ₹85
        assertEquals(85.0, summary.currentMonthPreviewRupees, 0.001)
        assertEquals(3, summary.eligibleRoundUpCount)
        // DMart (₹1,230 -> ₹70 raw) exceeds default ₹50/purchase cap
        assertEquals(0.0, summary.previousMonthPreviewRupees, 0.001)

        // When per-purchase cap is raised to ₹100, DMart (₹70) and Metro (₹65) become eligible
        val relaxedSummary = RoundUpCalculator.buildSandboxSummary(
            RoundUpCalculator.evaluateTransactions(txs, SavingRules(maxPerPurchaseRupees = 100.0)),
            1500.0,
        )
        assertEquals(70.0, relaxedSummary.previousMonthPreviewRupees, 0.001)
    }

    @Test
    fun `confirmed ledger always reflects goal confirmed balances without mixing sandbox previews`() {
        val goals = SandboxScenarios.starterGoals("user_test")
        val ledger = RoundUpCalculator.buildConfirmedLedgerSummary(goals)
        assertEquals(0.0, ledger.availableBalanceRupees, 0.001)
        assertEquals(0.0, ledger.pendingTransfersRupees, 0.001)
        assertEquals(0.0, ledger.completedContributionsRupees, 0.001)
    }

    @Test
    fun `paused rules prevent sandbox round-up accruals`() {
        val txs = SandboxScenarios.sandboxScenarioTransactions("user_test", referenceNowMillis = 1_700_000_000_000L)
        val evaluated = RoundUpCalculator.evaluateTransactions(txs, SavingRules(paused = true))
        val summary = RoundUpCalculator.buildSandboxSummary(evaluated, 1500.0)
        assertEquals(0.0, summary.currentMonthPreviewRupees, 0.001)
        val coffee = evaluated.first { it.transaction.id == "sbx_coffee_180" }
        assertEquals(RoundUpStatus.PAUSED, coffee.status)
    }
}
