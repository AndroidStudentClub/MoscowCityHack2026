package com.mikhailskiy.finni.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mikhailskiy.finni.data.AppSettings
import com.mikhailskiy.finni.data.FinniRepository
import com.mikhailskiy.finni.data.SettingsRepository
import com.mikhailskiy.finni.data.StepUpdateBus
import com.mikhailskiy.finni.domain.GameResult
import com.mikhailskiy.finni.domain.GameSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FinniUiState(
    val isLoading: Boolean = true,
    val isBusy: Boolean = false,
    val snapshot: GameSnapshot = GameSnapshot(),
    val message: UiMessage? = null,
    val settings: AppSettings = AppSettings(),
)

data class UiMessage(
    val text: String,
    val isError: Boolean,
    val nonce: Long = System.nanoTime(),
)

class FinniViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FinniRepository(application)
    private val settingsRepository = SettingsRepository(application)

    private val _uiState = MutableStateFlow(FinniUiState(settings = settingsRepository.load()))
    val uiState: StateFlow<FinniUiState> = _uiState.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            StepUpdateBus.updates.collect {
                val snapshot = repository.loadSnapshot()
                _uiState.update { state -> state.copy(snapshot = snapshot) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val snapshot = repository.loadSnapshot()
            _uiState.update { it.copy(isLoading = false, snapshot = snapshot) }
        }
    }

    fun createProfile(nickname: String, onSuccess: () -> Unit) = runAction(
        action = { repository.createProfile(nickname) },
        onSuccess = onSuccess,
    )

    fun createPet(
        name: String,
        speciesId: String,
        colorId: String,
        accessoryId: String?,
        onSuccess: () -> Unit,
    ) = runAction(
        action = { repository.createPet(name, speciesId, colorId, accessoryId) },
        onSuccess = onSuccess,
    )

    fun createDemo(onSuccess: () -> Unit) = runAction(
        action = { repository.createDemoProfile() },
        onSuccess = onSuccess,
    )

    fun confirmBudget(required: Int, optional: Int, savings: Int, onSuccess: () -> Unit = {}) = runAction(
        action = { repository.confirmBudget(required, optional, savings) },
        onSuccess = onSuccess,
    )

    fun completeTask(taskId: String, actions: String, onSuccess: () -> Unit) = runAction(
        action = { repository.completeTask(taskId, actions) },
        onSuccess = onSuccess,
    )

    fun purchase(itemId: String) = runAction(action = { repository.purchase(itemId) })

    fun selectGoal(goalId: String) = runAction(action = { repository.selectGoal(goalId) })

    fun deposit(amount: Int) = runAction(action = { repository.depositSavings(amount) })

    fun withdraw(amount: Int) = runAction(action = { repository.withdrawSavings(amount) })

    fun completePeriod(onSuccess: () -> Unit) = runAction(
        action = { repository.completePeriod() },
        onSuccess = onSuccess,
    )

    fun startNextPeriod(onSuccess: () -> Unit) = runAction(
        action = { repository.startNextPeriod() },
        onSuccess = onSuccess,
    )

    fun resetDemo(onSuccess: () -> Unit) = runAction(
        action = { repository.resetDemoProfile() },
        onSuccess = onSuccess,
    )

    fun deleteProfile(onSuccess: () -> Unit) = runAction(
        action = { repository.deleteProfile() },
        onSuccess = onSuccess,
    )

    fun setSoundEnabled(enabled: Boolean) {
        settingsRepository.setSoundEnabled(enabled)
        _uiState.update { it.copy(settings = it.settings.copy(soundEnabled = enabled)) }
    }

    fun setReducedMotion(enabled: Boolean) {
        settingsRepository.setReducedMotion(enabled)
        _uiState.update { it.copy(settings = it.settings.copy(reducedMotion = enabled)) }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun runAction(
        action: suspend () -> GameResult,
        onSuccess: () -> Unit = {},
    ) {
        if (_uiState.value.isBusy) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val result = runCatching { action() }.getOrElse {
                GameResult.Error("Не получилось выполнить действие. Попробуй ещё раз.")
            }
            val snapshot = repository.loadSnapshot()
            when (result) {
                is GameResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            isLoading = false,
                            snapshot = snapshot,
                            message = UiMessage(result.message, isError = false),
                        )
                    }
                    onSuccess()
                }
                is GameResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            isLoading = false,
                            snapshot = snapshot,
                            message = UiMessage(result.message, isError = true),
                        )
                    }
                }
            }
        }
    }
}
