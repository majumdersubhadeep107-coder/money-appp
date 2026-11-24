package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.AuthManager
import com.example.data.PaiseJarRepository
import com.example.ui.components.PartnerStatusBadge
import com.example.ui.components.StatusNoticeBanner
import com.example.ui.components.TransactionDetailSheet
import com.example.ui.screens.ActivityScreen
import com.example.ui.screens.AuthAndOnboardingScreen
import com.example.ui.screens.GoalsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MonthlySummaryScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RulesScreen
import com.example.ui.theme.LocalReducedMotion
import com.example.ui.theme.PaiseJarTheme
import com.example.viewmodel.PaiseJarViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val ROUTE_HOME = "home"
const val ROUTE_ACTIVITY = "activity"
const val ROUTE_GOALS = "goals"
const val ROUTE_RULES = "rules"
const val ROUTE_PROFILE = "profile"
const val ROUTE_MONTHLY_SUMMARY = "monthly_summary"

private data class NavDestinationItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

private val MAIN_DESTINATIONS = listOf(
    NavDestinationItem(
        route = ROUTE_HOME,
        label = "Home",
        selectedIcon = Icons.Rounded.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_home",
    ),
    NavDestinationItem(
        route = ROUTE_ACTIVITY,
        label = "Activity",
        selectedIcon = Icons.AutoMirrored.Rounded.ReceiptLong,
        unselectedIcon = Icons.AutoMirrored.Outlined.ReceiptLong,
        testTag = "nav_activity",
    ),
    NavDestinationItem(
        route = ROUTE_GOALS,
        label = "Goals",
        selectedIcon = Icons.Rounded.Savings,
        unselectedIcon = Icons.Outlined.Savings,
        testTag = "nav_goals",
    ),
    NavDestinationItem(
        route = ROUTE_RULES,
        label = "Rules",
        selectedIcon = Icons.Rounded.Tune,
        unselectedIcon = Icons.Outlined.Tune,
        testTag = "nav_rules",
    ),
    NavDestinationItem(
        route = ROUTE_PROFILE,
        label = "Profile",
        selectedIcon = Icons.Rounded.AccountCircle,
        unselectedIcon = Icons.Outlined.AccountCircle,
        testTag = "nav_profile",
    ),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            PaiseJarTheme {
                PaiseJarRoot()
            }
        }
    }
}

@Composable
fun PaiseJarRoot() {
    val context = LocalContext.current
    var authReady by remember { mutableStateOf(false) }
    var currentUser by remember { mutableStateOf<FirebaseUser?>(null) }
    var lastAcceptedConsent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val existingUser = withContext(Dispatchers.IO) {
            Firebase.auth.currentUser
        }
        currentUser = existingUser
        authReady = true

        if (existingUser == null) {
            // Wait until the first frame transaction settles so Credential Manager IPC never stalls SurfaceSyncGroup.
            withFrameNanos { }
            delay(350)
            AuthManager.attemptAutoSignIn(context)
        }
    }

    DisposableEffect(authReady) {
        if (!authReady) {
            onDispose { }
        } else {
            val listener = FirebaseAuth.AuthStateListener { auth ->
                currentUser = auth.currentUser
            }
            Firebase.auth.addAuthStateListener(listener)
            onDispose {
                Firebase.auth.removeAuthStateListener(listener)
            }
        }
    }

    when {
        !authReady -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            )
        }

        currentUser == null -> {
            AuthAndOnboardingScreen(
                isAlreadySignedIn = false,
                onAuthenticated = { user, consentAccepted ->
                    lastAcceptedConsent = consentAccepted
                    currentUser = user
                },
            )
        }

        else -> {
            val activeUser = currentUser!!
            key(activeUser.uid) {
                AuthenticatedAppShell(
                    user = activeUser,
                    consentAccepted = lastAcceptedConsent,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedAppShell(
    user: FirebaseUser,
    consentAccepted: Boolean,
) {
    val context = LocalContext.current
    val databaseId = remember(context) { context.getString(R.string.firestore_database_id) }
    val repository by produceState<PaiseJarRepository?>(initialValue = null, databaseId) {
        value = withContext(Dispatchers.IO) {
            PaiseJarRepository(FirebaseFirestore.getInstance(databaseId))
        }
    }

    val activeRepo = repository
    if (activeRepo == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        )
        return
    }

    val viewModel: PaiseJarViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                PaiseJarViewModel(
                    repository = activeRepo,
                    currentUserId = user.uid,
                    initialDisplayName = user.displayName ?: "",
                    consentAccepted = consentAccepted,
                )
            }
        },
    )

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val reducedMotion = LocalReducedMotion.current
    var showDisclosuresReplay by remember { mutableStateOf(false) }

    if (showDisclosuresReplay) {
        BackHandler { showDisclosuresReplay = false }
        AuthAndOnboardingScreen(
            isAlreadySignedIn = true,
            onCloseReplay = { showDisclosuresReplay = false },
        )
        return
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_HOME

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var selectedTransactionId by remember { mutableStateOf<String?>(null) }

    val selectedEvaluatedTx = remember(selectedTransactionId, state.evaluatedTransactions) {
        selectedTransactionId?.let { id ->
            state.evaluatedTransactions.firstOrNull { it.transaction.id == id }
        }
    }

    fun showMessage(message: String) {
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
        }
    }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val isMainTab = MAIN_DESTINATIONS.any { it.route == currentRoute }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                if (isMainTab) {
                    TopAppBar(
                        title = {
                            Text(
                                text = "PaiseJar",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        },
                        actions = {
                            PartnerStatusBadge(
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .testTag("top_bar_partner_badge"),
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                        ),
                    )
                }
            },
            bottomBar = {
                if (!useNavigationRail && isMainTab) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        MAIN_DESTINATIONS.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label,
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag(dest.testTag),
                            )
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useNavigationRail && isMainTab) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        MAIN_DESTINATIONS.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label,
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag(dest.testTag),
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                ) {
                    state.errorMessage?.let { err ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            StatusNoticeBanner(
                                title = "Sync / validation notice",
                                subtitle = err,
                                icon = Icons.Rounded.ErrorOutline,
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                actionLabel = "Dismiss",
                                onActionClick = { viewModel.dismissError() },
                            )
                        }
                    }

                    if (state.isLoadingAccount) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        NavHost(
                            navController = navController,
                            startDestination = ROUTE_HOME,
                            enterTransition = {
                                if (reducedMotion) EnterTransition.None else fadeIn(tween(180))
                            },
                            exitTransition = {
                                if (reducedMotion) ExitTransition.None else fadeOut(tween(180))
                            },
                        ) {
                            composable(ROUTE_HOME) {
                                HomeScreen(
                                    state = state,
                                    onTogglePause = {
                                        val nextPaused = !state.rules.paused
                                        viewModel.togglePauseRules()
                                        showMessage(
                                            if (nextPaused) "Saving rules paused." else "Saving rules resumed.",
                                        )
                                    },
                                    onReviewRules = { navigateToTab(ROUTE_RULES) },
                                    onSeeAllActivity = { navigateToTab(ROUTE_ACTIVITY) },
                                    onOpenMonthlySummary = { navController.navigate(ROUTE_MONTHLY_SUMMARY) },
                                    onOpenGoals = { navigateToTab(ROUTE_GOALS) },
                                    onLoadSandboxScenarios = {
                                        viewModel.loadSandboxScenarioPack {
                                            showMessage("Loaded sandbox test scenarios (preview only).")
                                        }
                                    },
                                    onSelectTransaction = { item ->
                                        selectedTransactionId = item.transaction.id
                                    },
                                )
                            }

                            composable(ROUTE_ACTIVITY) {
                                ActivityScreen(
                                    state = state,
                                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                    onSelectDateRange = { viewModel.setDateRangeFilter(it) },
                                    onSelectFilter = { viewModel.setActivityFilter(it) },
                                    onSelectTransaction = { item ->
                                        selectedTransactionId = item.transaction.id
                                    },
                                    onLoadSandboxPack = {
                                        viewModel.loadSandboxScenarioPack {
                                            showMessage("Loaded sandbox scenario pack.")
                                        }
                                    },
                                    onClearSandbox = {
                                        viewModel.clearSandboxTransactions {
                                            showMessage("Cleared sandbox events.")
                                        }
                                    },
                                    onAddSandboxTransaction = { merchant, rawLabel, desc, cat, amount, kind, instruction, key ->
                                        viewModel.addSandboxTransaction(
                                            merchantName = merchant,
                                            rawMerchantLabel = rawLabel,
                                            description = desc,
                                            category = cat,
                                            purchaseAmountRupees = amount,
                                            eventKind = kind,
                                            instructionStatus = instruction,
                                            idempotencyKey = key,
                                            onResult = { added ->
                                                showMessage(
                                                    if (added) {
                                                        "Added sandbox event for $merchant."
                                                    } else {
                                                        "Duplicate idempotency key blocked ($key)."
                                                    },
                                                )
                                            },
                                        )
                                    },
                                    onShowMessage = ::showMessage,
                                )
                            }

                            composable(ROUTE_GOALS) {
                                GoalsScreen(
                                    state = state,
                                    onToggleShowArchived = { viewModel.setShowArchivedGoals(it) },
                                    onSelectGoalForRoundUps = { viewModel.selectGoalForRoundUps(it) },
                                    onSaveGoal = { existingId, name, target, dateLabel, iconKey ->
                                        viewModel.createOrUpdateGoal(
                                            existingGoalId = existingId,
                                            name = name,
                                            targetAmountRupees = target,
                                            targetDateLabel = dateLabel,
                                            iconKey = iconKey,
                                        )
                                    },
                                    onSetGoalArchived = { goalId, archived ->
                                        viewModel.setGoalArchived(goalId, archived)
                                    },
                                    onShowMessage = ::showMessage,
                                )
                            }

                            composable(ROUTE_RULES) {
                                RulesScreen(
                                    state = state,
                                    onSaveRules = { rules, done -> viewModel.saveRules(rules, done) },
                                    onRestoreDefaults = { done -> viewModel.restoreDefaultRules(done) },
                                    onShowMessage = ::showMessage,
                                )
                            }

                            composable(ROUTE_PROFILE) {
                                ProfileScreen(
                                    state = state,
                                    userEmail = user.email,
                                    onSavePreferences = { name, notif, digest, consent ->
                                        viewModel.updateProfilePreferences(
                                            displayName = name,
                                            notificationsEnabled = notif,
                                            weeklyDigestEnabled = digest,
                                            dataConsentAccepted = consent,
                                        )
                                    },
                                    onSubmitSupportTicket = { topic, msg ->
                                        viewModel.submitSupportTicket(topic, msg) { ticketId ->
                                            showMessage("Support ticket ${ticketId.take(8)} submitted.")
                                        }
                                    },
                                    onOpenMonthlySummary = { navController.navigate(ROUTE_MONTHLY_SUMMARY) },
                                    onReplayDisclosures = { showDisclosuresReplay = true },
                                    onSignOut = {
                                        coroutineScope.launch {
                                            AuthManager.signOut(context)
                                        }
                                    },
                                    onDeleteAccount = {
                                        coroutineScope.launch {
                                            val deleteData = viewModel.deleteAllUserDataBeforeAuthDelete()
                                            if (deleteData.isSuccess) {
                                                AuthManager.deleteFirebaseAuthAccount(context)
                                            }
                                        }
                                    },
                                    onShowMessage = ::showMessage,
                                )
                            }

                            composable(ROUTE_MONTHLY_SUMMARY) {
                                MonthlySummaryScreen(
                                    state = state,
                                    onBack = { navController.popBackStack() },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedEvaluatedTx?.let { item ->
        TransactionDetailSheet(
            item = item,
            onDismiss = { selectedTransactionId = null },
            onToggleExclude = { excluded ->
                viewModel.setTransactionExcluded(item.transaction.id, excluded)
                selectedTransactionId = null
                coroutineScope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val msg = if (excluded) {
                        "Excluded ${item.transaction.merchantName} (cancels preview instruction only)."
                    } else {
                        "Re-included ${item.transaction.merchantName} in preview round-ups."
                    }
                    val result = snackbarHostState.showSnackbar(
                        message = msg,
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoLastExclusion()
                    }
                }
            },
        )
    }
}
