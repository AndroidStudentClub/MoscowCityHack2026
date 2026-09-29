package com.mikhailskiy.finni.data

import com.mikhailskiy.finni.domain.BudgetCategory
import com.mikhailskiy.finni.domain.GoalStatus
import com.mikhailskiy.finni.domain.GrowthStage
import com.mikhailskiy.finni.domain.OperationType
import com.mikhailskiy.finni.domain.PeriodStatus
import com.mikhailskiy.finni.domain.PlanStatus

data class ProfileEntity(
    val profileId: String,
    val nickname: String,
    val avatarId: String,
    val isDemo: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

data class PetEntity(
    val petId: String,
    val profileId: String,
    val name: String,
    val speciesId: String,
    val colorId: String,
    val accessoryId: String?,
    val hasShoes: Boolean,
    val hasGlasses: Boolean,
    val hasJetpack: Boolean,
    val stage: GrowthStage,
    val growthPoints: Int,
    val satiety: Int,
    val mood: Int,
    val comfort: Int,
    val updatedAt: Long,
)

data class StepTrackingEntity(
    val profileId: String,
    val lastSensorTotal: Long?,
    val trackedSteps: Long,
    val pendingSteps: Int,
    val updatedAt: Long,
)

data class GamePeriodEntity(
    val periodId: String,
    val profileId: String,
    val periodNumber: Int,
    val templateId: String,
    val contentVersion: String,
    val status: PeriodStatus,
    val openingAvailable: Int,
    val openingSavings: Int,
    val startedAt: Long,
    val completedAt: Long?,
)

data class BudgetPlanEntity(
    val periodId: String,
    val baseAvailable: Int,
    val requiredPlanned: Int,
    val optionalPlanned: Int,
    val savingsPlanned: Int,
    val bufferPlanned: Int,
    val status: PlanStatus,
    val confirmedAt: Long?,
    val updatedAt: Long,
)

data class LedgerEntryEntity(
    val rowId: Long = 0,
    val entryId: String,
    val operationKey: String,
    val profileId: String,
    val periodId: String?,
    val operationType: OperationType,
    val budgetCategory: BudgetCategory?,
    val availableDelta: Int,
    val savingsDelta: Int,
    val availableAfter: Int,
    val savingsAfter: Int,
    val sourceId: String?,
    val explanation: String,
    val createdAt: Long,
)

data class PurchaseEntity(
    val purchaseId: String,
    val profileId: String,
    val periodId: String,
    val ledgerRowId: Long,
    val catalogItemId: String,
    val titleSnapshot: String,
    val categorySnapshot: BudgetCategory,
    val priceSnapshot: Int,
    val satietyDelta: Int,
    val moodDelta: Int,
    val comfortDelta: Int,
    val purchasedAt: Long,
)

data class GoalProgressEntity(
    val goalProgressId: String,
    val profileId: String,
    val goalId: String,
    val titleSnapshot: String,
    val targetSnapshot: Int,
    val status: GoalStatus,
    val selectedAt: Long,
    val completedAt: Long?,
    val savingsAtFinish: Int?,
)

data class TaskAttemptEntity(
    val attemptId: String,
    val profileId: String,
    val periodId: String?,
    val taskId: String,
    val contentVersion: String,
    val resultCode: String,
    val actionsSnapshot: String,
    val reward: Int,
    val rewardLedgerRowId: Long?,
    val startedAt: Long,
    val completedAt: Long,
)

data class PetStateEventEntity(
    val eventId: String,
    val petId: String,
    val periodId: String?,
    val reasonType: String,
    val reasonId: String?,
    val satietyDelta: Int,
    val moodDelta: Int,
    val comfortDelta: Int,
    val growthDelta: Int,
    val satietyAfter: Int,
    val moodAfter: Int,
    val comfortAfter: Int,
    val growthAfter: Int,
    val message: String,
    val createdAt: Long,
)

data class PeriodSummaryEntity(
    val periodId: String,
    val periodNumber: Int,
    val incomeTotal: Int,
    val requiredActual: Int,
    val optionalActual: Int,
    val savingsDeposited: Int,
    val savingsWithdrawn: Int,
    val closingAvailable: Int,
    val closingSavings: Int,
    val essentialsScore: Int,
    val planScore: Int,
    val savingsScore: Int,
    val growthAwarded: Int,
    val feedback: String,
    val createdAt: Long,
)
