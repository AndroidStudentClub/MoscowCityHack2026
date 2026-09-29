package com.mikhailskiy.finni.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mikhailskiy.finni.domain.PeriodStatus
import com.mikhailskiy.finni.ui.components.MessageBanner
import com.mikhailskiy.finni.ui.screens.AdultGateScreen
import com.mikhailskiy.finni.ui.screens.AdultScreen
import com.mikhailskiy.finni.ui.screens.BudgetScreen
import com.mikhailskiy.finni.ui.screens.CreatePetScreen
import com.mikhailskiy.finni.ui.screens.CreateProfileScreen
import com.mikhailskiy.finni.ui.screens.HelpScreen
import com.mikhailskiy.finni.ui.screens.HomeScreen
import com.mikhailskiy.finni.ui.screens.IntroScreen
import com.mikhailskiy.finni.ui.screens.ProgressScreen
import com.mikhailskiy.finni.ui.screens.SavingsScreen
import com.mikhailskiy.finni.ui.screens.ShopScreen
import com.mikhailskiy.finni.ui.screens.SummaryScreen
import com.mikhailskiy.finni.ui.screens.TaskPlayScreen
import com.mikhailskiy.finni.ui.screens.TasksScreen

private sealed interface FinniScreen {
    data object Intro : FinniScreen
    data object CreateProfile : FinniScreen
    data object CreatePet : FinniScreen
    data object Home : FinniScreen
    data object Budget : FinniScreen
    data object Tasks : FinniScreen
    data class TaskPlay(val taskId: String) : FinniScreen
    data object Shop : FinniScreen
    data object Savings : FinniScreen
    data object Progress : FinniScreen
    data object Summary : FinniScreen
    data object Help : FinniScreen
    data object AdultGate : FinniScreen
    data object Adult : FinniScreen
}

private data class BottomDestination(
    val screen: FinniScreen,
    val emoji: String,
    val label: String,
)

private val bottomDestinations = listOf(
    BottomDestination(FinniScreen.Home, "🏠", "Домой"),
    BottomDestination(FinniScreen.Budget, "🧺", "Бюджет"),
    BottomDestination(FinniScreen.Tasks, "🎯", "Задания"),
    BottomDestination(FinniScreen.Shop, "🛍️", "Магазин"),
    BottomDestination(FinniScreen.Progress, "📈", "Прогресс"),
)

@Composable
fun FinniApp(
    viewModel: FinniViewModel = viewModel(),
    stepSensorAvailable: Boolean = false,
    stepPermissionGranted: Boolean = false,
    onRequestStepPermission: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val backStack = remember { mutableStateListOf<FinniScreen>() }

    fun replaceRoot(screen: FinniScreen) {
        backStack.clear()
        backStack.add(screen)
    }

    fun push(screen: FinniScreen) {
        if (backStack.lastOrNull() != screen) backStack.add(screen)
    }

    fun pop() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
        else if (backStack.lastOrNull() != FinniScreen.Home && uiState.snapshot.pet != null) replaceRoot(FinniScreen.Home)
    }

    fun selectTab(screen: FinniScreen) {
        if (screen == FinniScreen.Home) {
            replaceRoot(FinniScreen.Home)
        } else {
            backStack.clear()
            backStack.add(FinniScreen.Home)
            backStack.add(screen)
        }
    }

    LaunchedEffect(uiState.isLoading, uiState.snapshot.profile?.id, uiState.snapshot.pet?.id) {
        if (!uiState.isLoading && backStack.isEmpty()) {
            val destination = when {
                uiState.snapshot.profile == null -> FinniScreen.Intro
                uiState.snapshot.pet == null -> FinniScreen.CreatePet
                uiState.snapshot.period?.status == PeriodStatus.COMPLETED -> FinniScreen.Summary
                else -> FinniScreen.Home
            }
            backStack.add(destination)
        }
    }

    BackHandler(enabled = backStack.size > 1) { pop() }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        color = MaterialTheme.colorScheme.background,
    ) {
        if (uiState.isLoading && backStack.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Surface
        }

        val current = backStack.lastOrNull() ?: return@Surface
        val showBottomBar = bottomDestinations.any { it.screen == current }
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        bottomDestinations.forEach { destination ->
                            NavigationBarItem(
                                selected = current == destination.screen,
                                onClick = { selectTab(destination.screen) },
                                icon = { Text(destination.emoji) },
                                label = { Text(destination.label) },
                            )
                        }
                    }
                }
            },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                when (current) {
                    FinniScreen.Intro -> IntroScreen(
                        isBusy = uiState.isBusy,
                        onStart = { push(FinniScreen.CreateProfile) },
                        onDemo = { viewModel.createDemo { replaceRoot(FinniScreen.Home) } },
                    )
                    FinniScreen.CreateProfile -> CreateProfileScreen(
                        isBusy = uiState.isBusy,
                        onBack = ::pop,
                        onContinue = { name -> viewModel.createProfile(name) { push(FinniScreen.CreatePet) } },
                    )
                    FinniScreen.CreatePet -> CreatePetScreen(
                        isBusy = uiState.isBusy,
                        onBack = ::pop,
                        onCreate = { name, species, color, accessory ->
                            viewModel.createPet(name, species, color, accessory) { replaceRoot(FinniScreen.Home) }
                        },
                    )
                    FinniScreen.Home -> HomeScreen(
                        snapshot = uiState.snapshot,
                        stepSensorAvailable = stepSensorAvailable,
                        stepPermissionGranted = stepPermissionGranted,
                        onRequestStepPermission = onRequestStepPermission,
                        onBudget = { selectTab(FinniScreen.Budget) },
                        onTasks = { selectTab(FinniScreen.Tasks) },
                        onShop = { selectTab(FinniScreen.Shop) },
                        onSavings = { push(FinniScreen.Savings) },
                        onProgress = { selectTab(FinniScreen.Progress) },
                        onHelp = { push(FinniScreen.Help) },
                        onAdult = { push(FinniScreen.AdultGate) },
                        onSummary = { push(FinniScreen.Summary) },
                    )
                    FinniScreen.Budget -> BudgetScreen(
                        snapshot = uiState.snapshot,
                        isBusy = uiState.isBusy,
                        onBack = ::pop,
                        onConfirm = { required, optional, savings ->
                            viewModel.confirmBudget(required, optional, savings)
                        },
                        onCompletePeriod = { viewModel.completePeriod { replaceRoot(FinniScreen.Summary) } },
                        onShowSummary = { push(FinniScreen.Summary) },
                    )
                    FinniScreen.Tasks -> TasksScreen(
                        completedTaskIds = uiState.snapshot.completedTaskIds,
                        onBack = ::pop,
                        onOpenTask = { push(FinniScreen.TaskPlay(it)) },
                    )
                    is FinniScreen.TaskPlay -> TaskPlayScreen(
                        taskId = current.taskId,
                        isBusy = uiState.isBusy,
                        onBack = ::pop,
                        onComplete = { actions -> viewModel.completeTask(current.taskId, actions) { pop() } },
                    )
                    FinniScreen.Shop -> ShopScreen(
                        snapshot = uiState.snapshot,
                        isBusy = uiState.isBusy,
                        onBack = ::pop,
                        onPurchase = viewModel::purchase,
                    )
                    FinniScreen.Savings -> SavingsScreen(
                        snapshot = uiState.snapshot,
                        isBusy = uiState.isBusy,
                        onBack = ::pop,
                        onSelectGoal = viewModel::selectGoal,
                        onDeposit = viewModel::deposit,
                        onWithdraw = viewModel::withdraw,
                    )
                    FinniScreen.Progress -> ProgressScreen(uiState.snapshot, ::pop)
                    FinniScreen.Summary -> SummaryScreen(
                        snapshot = uiState.snapshot,
                        isBusy = uiState.isBusy,
                        onNextPeriod = { viewModel.startNextPeriod { replaceRoot(FinniScreen.Home) } },
                        onBack = ::pop,
                    )
                    FinniScreen.Help -> HelpScreen(::pop)
                    FinniScreen.AdultGate -> AdultGateScreen(
                        onBack = ::pop,
                        onUnlocked = { push(FinniScreen.Adult) },
                    )
                    FinniScreen.Adult -> AdultScreen(
                        snapshot = uiState.snapshot,
                        settings = uiState.settings,
                        isBusy = uiState.isBusy,
                        onBack = ::pop,
                        onSoundChange = viewModel::setSoundEnabled,
                        onMotionChange = viewModel::setReducedMotion,
                        onResetDemo = { viewModel.resetDemo { replaceRoot(FinniScreen.Home) } },
                        onDeleteProfile = { viewModel.deleteProfile { replaceRoot(FinniScreen.Intro) } },
                    )
                }

                uiState.message?.let { message ->
                    MessageBanner(
                        message = message,
                        onDismiss = viewModel::dismissMessage,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(12.dp),
                    )
                }
            }
        }
    }
}
