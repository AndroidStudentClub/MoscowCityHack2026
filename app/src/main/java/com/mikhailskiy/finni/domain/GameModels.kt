package com.mikhailskiy.finni.domain

enum class PeriodStatus { ACTIVE, COMPLETED }

enum class PlanStatus { DRAFT, CONFIRMED }

enum class BudgetCategory { REQUIRED, OPTIONAL }

enum class OperationType {
    PERIOD_INCOME,
    TASK_REWARD,
    PURCHASE,
    SAVINGS_DEPOSIT,
    SAVINGS_WITHDRAWAL,
}

enum class GrowthStage(val title: String, val minPoints: Int) {
    BABY("Малыш", 0),
    EXPLORER("Исследователь", 30),
    EXPERT("Знаток", 70);

    companion object {
        fun fromPoints(points: Int): GrowthStage = when {
            points >= EXPERT.minPoints -> EXPERT
            points >= EXPLORER.minPoints -> EXPLORER
            else -> BABY
        }
    }
}

enum class GoalStatus { ACTIVE, PAUSED, COMPLETED }

enum class TaskStatus { COMPLETED }

enum class TaskTopic(val title: String) {
    BUDGET("Планирование"),
    SAVINGS("Сбережения"),
    PURCHASES("Покупки"),
}

enum class TaskKind { BUDGET, BASKET, SAVINGS }

data class Wallet(
    val available: Int = 0,
    val savings: Int = 0,
    val revision: Long = 0,
)

data class GameSnapshot(
    val profile: ProfileData? = null,
    val pet: PetData? = null,
    val period: PeriodData? = null,
    val plan: BudgetPlanData? = null,
    val wallet: Wallet = Wallet(),
    val goal: GoalProgressData? = null,
    val completedTaskIds: Set<String> = emptySet(),
    val purchases: List<PurchaseData> = emptyList(),
    val summaries: List<PeriodSummaryData> = emptyList(),
    val requiredActual: Int = 0,
    val optionalActual: Int = 0,
    val savingsDeposited: Int = 0,
    val stepProgress: StepProgressData = StepProgressData(),
)

data class StepProgressData(
    val trackedSteps: Long = 0,
    val stepsSinceLastSatietyPoint: Int = 0,
    val stepsPerSatietyPoint: Int = StepSatietyRules.BASE_STEPS_PER_SATIETY_POINT,
) {
    val stepsUntilNextSatietyPoint: Int
        get() = stepsPerSatietyPoint - stepsSinceLastSatietyPoint
}

data class ProfileData(
    val id: String,
    val nickname: String,
    val avatarId: String,
    val isDemo: Boolean,
)

data class PetData(
    val id: String,
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
)

data class PeriodData(
    val id: String,
    val number: Int,
    val status: PeriodStatus,
)

data class BudgetPlanData(
    val periodId: String,
    val baseAvailable: Int,
    val required: Int,
    val optional: Int,
    val savings: Int,
    val buffer: Int,
    val status: PlanStatus,
)

data class GoalProgressData(
    val id: String,
    val goalId: String,
    val target: Int,
    val status: GoalStatus,
)

data class PurchaseData(
    val id: String,
    val itemId: String,
    val category: BudgetCategory,
    val price: Int,
)

data class PeriodSummaryData(
    val periodId: String,
    val periodNumber: Int,
    val income: Int,
    val requiredActual: Int,
    val optionalActual: Int,
    val savingsDeposited: Int,
    val closingAvailable: Int,
    val closingSavings: Int,
    val growthAwarded: Int,
    val feedback: String,
)

sealed interface GameResult {
    data class Success(val message: String) : GameResult
    data class Error(val message: String) : GameResult
}

object EconomyRules {
    fun validatePlan(base: Int, required: Int, optional: Int, savings: Int): String? {
        if (required < 0 || optional < 0 || savings < 0) return "Суммы не могут быть отрицательными"
        if (required + optional + savings > base) return "Распределено больше, чем есть в кошельке"
        return null
    }

    fun growthAward(
        requiredPurchases: Int,
        requiredActual: Int,
        optionalActual: Int,
        savingsDeposited: Int,
        plan: BudgetPlanData,
    ): Int {
        val essentials = if (requiredPurchases > 0) 10 else 0
        val followsPlan = if (
            requiredActual <= plan.required && optionalActual <= plan.optional
        ) 5 else 0
        val savesRegularly = if (savingsDeposited >= plan.savings) 5 else 0
        return essentials + followsPlan + savesRegularly
    }
}

data class StepSatietyImpact(
    val satietyLoss: Int,
    val remainingSteps: Int,
)

object StepSatietyRules {
    const val BASE_STEPS_PER_SATIETY_POINT = 500
    const val SHOES_STEPS_PER_SATIETY_POINT = 1_000
    const val JETPACK_STEPS_PER_SATIETY_POINT = 1_500
    const val SHOES_AND_JETPACK_STEPS_PER_SATIETY_POINT = 2_000

    fun threshold(hasShoes: Boolean, hasJetpack: Boolean): Int =
        BASE_STEPS_PER_SATIETY_POINT +
            (if (hasShoes) 500 else 0) +
            (if (hasJetpack) 1_000 else 0)

    fun calculate(
        pendingSteps: Int,
        newSteps: Long,
        stepsPerSatietyPoint: Int = BASE_STEPS_PER_SATIETY_POINT,
    ): StepSatietyImpact {
        val safeThreshold = stepsPerSatietyPoint.coerceAtLeast(1)
        val safePending = pendingSteps.coerceIn(0, safeThreshold - 1)
        val accumulated = safePending.toLong() + newSteps.coerceAtLeast(0)
        return StepSatietyImpact(
            satietyLoss = (accumulated / safeThreshold)
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt(),
            remainingSteps = (accumulated % safeThreshold).toInt(),
        )
    }
}
