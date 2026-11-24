package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PaiseJarRepository
import com.example.data.userFriendlyFirebaseMessage
import com.example.model.AccountAuditLogDoc
import com.example.model.ActivityFilter
import com.example.model.AuditableTransactionDoc
import com.example.model.DateRangeFilter
import com.example.model.PaiseJarUiState
import com.example.model.PendingExclusionAction
import com.example.model.RoundUpCalculator
import com.example.model.SavingRules
import com.example.model.SavingsGoalDoc
import com.example.model.TransactionCategory
import com.example.model.TransactionEventKind
import com.example.model.TransferInstructionStatus
import com.example.model.UserAccountDoc
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class LocalViewFilters(
    val searchQuery: String = "",
    val dateRangeFilter: DateRangeFilter = DateRangeFilter.ALL,
    val activityFilter: ActivityFilter = ActivityFilter.ALL,
    val showArchivedGoals: Boolean = false,
    val pendingExclusionUndo: PendingExclusionAction? = null,
    val errorMessage: String? = null,
    val isBusy: Boolean = false,
)

private data class StreamBundle(
    val account: UserAccountDoc?,
    val goals: List<SavingsGoalDoc>,
    val transactions: List<AuditableTransactionDoc>,
    val auditLogs: List<AccountAuditLogDoc>,
)

class PaiseJarViewModel(
    private val repository: PaiseJarRepository,
    private val currentUserId: String,
    initialDisplayName: String,
    consentAccepted: Boolean,
) : ViewModel() {

    private val localFilters = MutableStateFlow(LocalViewFilters())
    private var undoExpiryJob: Job? = null

    init {
        viewModelScope.launch {
            try {
                repository.ensureUserAccountInitialized(
                    userId = currentUserId,
                    displayName = initialDisplayName,
                    consentAccepted = consentAccepted,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                localFilters.update { it.copy(errorMessage = userFriendlyFirebaseMessage(e)) }
            }
        }
    }

    private val firestoreBundleFlow = combine(
        repository.observeUserAccount(currentUserId),
        repository.observeGoals(currentUserId),
        repository.observeTransactions(currentUserId),
        repository.observeAuditLogs(currentUserId),
    ) { account, goals, transactions, auditLogs ->
        StreamBundle(
            account = account,
            goals = goals,
            transactions = transactions,
            auditLogs = auditLogs,
        )
    }.catch { error ->
        if (error is CancellationException) throw error
        localFilters.update { it.copy(errorMessage = userFriendlyFirebaseMessage(error)) }
        emit(
            StreamBundle(
                account = null,
                goals = emptyList(),
                transactions = emptyList(),
                auditLogs = emptyList(),
            ),
        )
    }

    val uiState: StateFlow<PaiseJarUiState> = combine(
        firestoreBundleFlow,
        localFilters,
    ) { bundle, filters ->
        val account = bundle.account
        val rules = account?.let { SavingRules.fromUserAccount(it) } ?: SavingRules()
        val evaluated = RoundUpCalculator.evaluateTransactions(bundle.transactions, rules)
        val confirmed = RoundUpCalculator.buildConfirmedLedgerSummary(bundle.goals)
        val sandbox = RoundUpCalculator.buildSandboxSummary(
            evaluated = evaluated,
            monthlyGoalTargetRupees = account?.monthlyGoalTargetRupees ?: 1500.0,
        )
        val selectedGoal = bundle.goals.firstOrNull {
            it.id == account?.selectedGoalId && !it.archived
        } ?: bundle.goals.firstOrNull { !it.archived }

        PaiseJarUiState(
            isLoadingAccount = account == null && filters.errorMessage == null,
            userAccount = account,
            rules = rules,
            goals = bundle.goals,
            selectedGoal = selectedGoal,
            rawTransactions = bundle.transactions,
            evaluatedTransactions = evaluated,
            auditLogs = bundle.auditLogs,
            confirmedLedger = confirmed,
            sandboxSummary = sandbox,
            searchQuery = filters.searchQuery,
            dateRangeFilter = filters.dateRangeFilter,
            activityFilter = filters.activityFilter,
            showArchivedGoals = filters.showArchivedGoals,
            pendingExclusionUndo = filters.pendingExclusionUndo,
            errorMessage = filters.errorMessage,
            isBusy = filters.isBusy,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PaiseJarUiState(isLoadingAccount = true),
    )

    fun dismissError() {
        localFilters.update { it.copy(errorMessage = null) }
    }

    fun setSearchQuery(query: String) {
        localFilters.update { it.copy(searchQuery = query) }
    }

    fun setDateRangeFilter(filter: DateRangeFilter) {
        localFilters.update { it.copy(dateRangeFilter = filter) }
    }

    fun setActivityFilter(filter: ActivityFilter) {
        localFilters.update { it.copy(activityFilter = filter) }
    }

    fun setShowArchivedGoals(showArchived: Boolean) {
        localFilters.update { it.copy(showArchivedGoals = showArchived) }
    }

    fun togglePauseRules() {
        val currentPaused = uiState.value.rules.paused
        launchMutation {
            repository.setPaused(currentUserId, !currentPaused)
        }
    }

    fun saveRules(newRules: SavingRules, onComplete: () -> Unit = {}) {
        launchMutation(onSuccess = onComplete) {
            repository.saveRules(currentUserId, newRules)
        }
    }

    fun restoreDefaultRules(onComplete: () -> Unit = {}) {
        val defaults = SavingRules(paused = uiState.value.rules.paused)
        saveRules(defaults, onComplete)
    }

    fun selectGoalForRoundUps(goalId: String) {
        launchMutation {
            repository.selectGoalForRoundUps(currentUserId, goalId)
        }
    }

    fun createOrUpdateGoal(
        existingGoalId: String?,
        name: String,
        targetAmountRupees: Double,
        targetDateLabel: String?,
        iconKey: String,
        onSuccess: () -> Unit = {},
    ) {
        launchMutation(onSuccess = onSuccess) {
            repository.createOrUpdateGoal(
                userId = currentUserId,
                existingGoalId = existingGoalId,
                name = name,
                targetAmountRupees = targetAmountRupees,
                targetDateLabel = targetDateLabel,
                iconKey = iconKey,
            )
        }
    }

    fun setGoalArchived(goalId: String, archived: Boolean, onSuccess: () -> Unit = {}) {
        launchMutation(onSuccess = onSuccess) {
            repository.setGoalArchived(currentUserId, goalId, archived)
        }
    }

    fun loadSandboxScenarioPack(onSuccess: () -> Unit = {}) {
        launchMutation(onSuccess = onSuccess) {
            repository.loadSandboxScenarioPack(currentUserId)
        }
    }

    fun clearSandboxTransactions(onSuccess: () -> Unit = {}) {
        launchMutation(onSuccess = onSuccess) {
            repository.clearSandboxTransactions(currentUserId)
        }
    }

    fun addSandboxTransaction(
        merchantName: String,
        rawMerchantLabel: String,
        description: String,
        category: TransactionCategory,
        purchaseAmountRupees: Double,
        eventKind: TransactionEventKind,
        instructionStatus: TransferInstructionStatus,
        idempotencyKey: String,
        onResult: (added: Boolean) -> Unit = {},
    ) {
        viewModelScope.launch {
            localFilters.update { it.copy(isBusy = true, errorMessage = null) }
            try {
                val added = repository.addSandboxTransaction(
                    userId = currentUserId,
                    merchantName = merchantName,
                    rawMerchantLabel = rawMerchantLabel,
                    description = description,
                    category = category,
                    purchaseAmountRupees = purchaseAmountRupees,
                    eventKind = eventKind,
                    instructionStatus = instructionStatus,
                    idempotencyKey = idempotencyKey,
                )
                localFilters.update { it.copy(isBusy = false) }
                onResult(added)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                localFilters.update {
                    it.copy(isBusy = false, errorMessage = userFriendlyFirebaseMessage(e))
                }
            }
        }
    }

    fun setTransactionExcluded(transactionId: String, excluded: Boolean) {
        val currentTx = uiState.value.rawTransactions.firstOrNull { it.id == transactionId } ?: return
        val newStatus = if (excluded) {
            TransferInstructionStatus.CANCELLED.name
        } else {
            TransferInstructionStatus.SIMULATED_PREVIEW.name
        }

        undoExpiryJob?.cancel()
        localFilters.update {
            it.copy(
                pendingExclusionUndo = PendingExclusionAction(
                    transactionId = currentTx.id,
                    previousExcluded = currentTx.userExcluded,
                    previousInstructionStatus = currentTx.transferInstructionStatus,
                    merchantName = currentTx.merchantName,
                ),
            )
        }
        undoExpiryJob = viewModelScope.launch {
            delay(8_000L)
            localFilters.update { it.copy(pendingExclusionUndo = null) }
        }

        launchMutation {
            repository.setTransactionExcluded(
                userId = currentUserId,
                txId = transactionId,
                excluded = excluded,
                instructionStatus = newStatus,
            )
        }
    }

    fun undoLastExclusion() {
        val pending = localFilters.value.pendingExclusionUndo ?: return
        undoExpiryJob?.cancel()
        localFilters.update { it.copy(pendingExclusionUndo = null) }
        launchMutation {
            repository.setTransactionExcluded(
                userId = currentUserId,
                txId = pending.transactionId,
                excluded = pending.previousExcluded,
                instructionStatus = pending.previousInstructionStatus,
            )
        }
    }

    fun updateProfilePreferences(
        displayName: String,
        notificationsEnabled: Boolean,
        weeklyDigestEnabled: Boolean,
        dataConsentAccepted: Boolean,
        onSuccess: () -> Unit = {},
    ) {
        launchMutation(onSuccess = onSuccess) {
            repository.updateProfilePreferences(
                userId = currentUserId,
                displayName = displayName,
                notificationsEnabled = notificationsEnabled,
                weeklyDigestEnabled = weeklyDigestEnabled,
                dataConsentAccepted = dataConsentAccepted,
            )
        }
    }

    fun submitSupportTicket(
        topic: String,
        message: String,
        onSubmitted: (ticketId: String) -> Unit,
    ) {
        viewModelScope.launch {
            localFilters.update { it.copy(isBusy = true, errorMessage = null) }
            try {
                val id = repository.submitSupportTicket(currentUserId, topic, message)
                localFilters.update { it.copy(isBusy = false) }
                onSubmitted(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                localFilters.update {
                    it.copy(isBusy = false, errorMessage = userFriendlyFirebaseMessage(e))
                }
            }
        }
    }

    suspend fun deleteAllUserDataBeforeAuthDelete(): Result<Unit> {
        return try {
            localFilters.update { it.copy(isBusy = true, errorMessage = null) }
            repository.deleteAccountAndAllData(currentUserId)
            localFilters.update { it.copy(isBusy = false) }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            localFilters.update {
                it.copy(isBusy = false, errorMessage = userFriendlyFirebaseMessage(e))
            }
            Result.failure(e)
        }
    }

    private fun launchMutation(
        onSuccess: () -> Unit = {},
        block: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            localFilters.update { it.copy(isBusy = true, errorMessage = null) }
            try {
                block()
                localFilters.update { it.copy(isBusy = false) }
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                localFilters.update {
                    it.copy(isBusy = false, errorMessage = userFriendlyFirebaseMessage(e))
                }
            }
        }
    }
}
