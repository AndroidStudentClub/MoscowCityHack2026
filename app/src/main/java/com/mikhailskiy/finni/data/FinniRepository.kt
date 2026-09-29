package com.mikhailskiy.finni.data

import android.content.Context
import com.mikhailskiy.finni.domain.BudgetCategory
import com.mikhailskiy.finni.domain.BudgetPlanData
import com.mikhailskiy.finni.domain.EconomyRules
import com.mikhailskiy.finni.domain.GameCatalog
import com.mikhailskiy.finni.domain.GameResult
import com.mikhailskiy.finni.domain.GameSnapshot
import com.mikhailskiy.finni.domain.GoalProgressData
import com.mikhailskiy.finni.domain.GoalStatus
import com.mikhailskiy.finni.domain.GrowthStage
import com.mikhailskiy.finni.domain.OperationType
import com.mikhailskiy.finni.domain.PeriodData
import com.mikhailskiy.finni.domain.PeriodStatus
import com.mikhailskiy.finni.domain.PeriodSummaryData
import com.mikhailskiy.finni.domain.PetData
import com.mikhailskiy.finni.domain.PlanStatus
import com.mikhailskiy.finni.domain.ProfileData
import com.mikhailskiy.finni.domain.PurchaseData
import com.mikhailskiy.finni.domain.StepProgressData
import com.mikhailskiy.finni.domain.StepSatietyRules
import com.mikhailskiy.finni.domain.Wallet
import java.util.UUID

class FinniRepository(context: Context) {
    private val database = FinniDatabase.getInstance(context)
    private val dao = database.dao

    suspend fun loadSnapshot(): GameSnapshot = database.read {
        val profile = dao.getProfile() ?: return@read GameSnapshot()
        val pet = dao.getPet(profile.profileId)
        val stepTracking = dao.getStepTracking(profile.profileId)
        val period = dao.getActivePeriod(profile.profileId) ?: dao.getLatestPeriod(profile.profileId)
        val plan = period?.let { dao.getBudgetPlan(it.periodId) }
        val ledger = dao.getLatestLedgerEntry(profile.profileId)
        val goal = dao.getActiveGoal(profile.profileId)
        val taskIds = dao.getCompletedTaskIds(profile.profileId).toSet()
        val purchases = period?.let { dao.getPurchases(it.periodId) }.orEmpty()
        val summaries = dao.getPeriodSummaries()
        val requiredActual = period?.let { dao.actualSpent(it.periodId, BudgetCategory.REQUIRED) } ?: 0
        val optionalActual = period?.let { dao.actualSpent(it.periodId, BudgetCategory.OPTIONAL) } ?: 0
        val savingsDeposited = period?.let {
            dao.savingsDelta(it.periodId, OperationType.SAVINGS_DEPOSIT)
        } ?: 0

        GameSnapshot(
            profile = ProfileData(profile.profileId, profile.nickname, profile.avatarId, profile.isDemo),
            pet = pet?.let {
                PetData(
                    id = it.petId,
                    name = it.name,
                    speciesId = it.speciesId,
                    colorId = it.colorId,
                    accessoryId = it.accessoryId,
                    hasShoes = it.hasShoes,
                    hasGlasses = it.hasGlasses,
                    hasJetpack = it.hasJetpack,
                    stage = it.stage,
                    growthPoints = it.growthPoints,
                    satiety = it.satiety,
                    mood = it.mood,
                    comfort = it.comfort,
                )
            },
            period = period?.let { PeriodData(it.periodId, it.periodNumber, it.status) },
            plan = plan?.let {
                BudgetPlanData(
                    periodId = it.periodId,
                    baseAvailable = it.baseAvailable,
                    required = it.requiredPlanned,
                    optional = it.optionalPlanned,
                    savings = it.savingsPlanned,
                    buffer = it.bufferPlanned,
                    status = it.status,
                )
            },
            wallet = Wallet(
                available = ledger?.availableAfter ?: 0,
                savings = ledger?.savingsAfter ?: 0,
                revision = ledger?.rowId ?: 0,
            ),
            goal = goal?.let {
                GoalProgressData(
                    id = it.goalProgressId,
                    goalId = it.goalId,
                    target = it.targetSnapshot,
                    status = it.status,
                )
            },
            completedTaskIds = taskIds,
            purchases = purchases.map {
                PurchaseData(
                    id = it.purchaseId,
                    itemId = it.catalogItemId,
                    category = it.categorySnapshot,
                    price = it.priceSnapshot,
                )
            },
            summaries = summaries.map { it.toDomain() },
            requiredActual = requiredActual,
            optionalActual = optionalActual,
            savingsDeposited = savingsDeposited,
            stepProgress = StepProgressData(
                trackedSteps = stepTracking?.trackedSteps ?: 0,
                stepsSinceLastSatietyPoint = stepTracking?.pendingSteps ?: 0,
                stepsPerSatietyPoint = StepSatietyRules.threshold(
                    hasShoes = pet?.hasShoes == true,
                    hasJetpack = pet?.hasJetpack == true,
                ),
            ),
        )
    }

    suspend fun recordStepCounter(sensorTotal: Long): Boolean = database.transaction {
        if (sensorTotal < 0) return@transaction false
        val profile = dao.getProfile() ?: return@transaction false
        val pet = dao.getPet(profile.profileId) ?: return@transaction false
        val now = System.currentTimeMillis()
        val current = dao.getStepTracking(profile.profileId)

        if (current?.lastSensorTotal == null) {
            dao.upsertStepTracking(
                StepTrackingEntity(
                    profileId = profile.profileId,
                    lastSensorTotal = sensorTotal,
                    trackedSteps = current?.trackedSteps ?: 0,
                    pendingSteps = current?.pendingSteps ?: 0,
                    updatedAt = now,
                ),
            )
            return@transaction false
        }

        if (sensorTotal < current.lastSensorTotal) {
            dao.upsertStepTracking(current.copy(lastSensorTotal = sensorTotal, updatedAt = now))
            return@transaction false
        }

        val newSteps = sensorTotal - current.lastSensorTotal
        if (newSteps == 0L) return@transaction false

        val stepsPerSatietyPoint = StepSatietyRules.threshold(
            hasShoes = pet.hasShoes,
            hasJetpack = pet.hasJetpack,
        )
        val impact = StepSatietyRules.calculate(
            pendingSteps = current.pendingSteps,
            newSteps = newSteps,
            stepsPerSatietyPoint = stepsPerSatietyPoint,
        )
        val updatedPet = pet.copy(
            satiety = (pet.satiety - impact.satietyLoss).coerceAtLeast(0),
            updatedAt = now,
        )
        dao.updatePet(updatedPet)
        dao.upsertStepTracking(
            current.copy(
                lastSensorTotal = sensorTotal,
                trackedSteps = current.trackedSteps + newSteps,
                pendingSteps = impact.remainingSteps,
                updatedAt = now,
            ),
        )

        val actualLoss = pet.satiety - updatedPet.satiety
        if (actualLoss > 0) {
            dao.insertPetStateEvent(
                PetStateEventEntity(
                    eventId = newId(),
                    petId = pet.petId,
                    periodId = dao.getActivePeriod(profile.profileId)?.periodId,
                    reasonType = "STEPS",
                    reasonId = sensorTotal.toString(),
                    satietyDelta = -actualLoss,
                    moodDelta = 0,
                    comfortDelta = 0,
                    growthDelta = 0,
                    satietyAfter = updatedPet.satiety,
                    moodAfter = updatedPet.mood,
                    comfortAfter = updatedPet.comfort,
                    growthAfter = updatedPet.growthPoints,
                    message = "$newSteps шагов: сытость −$actualLoss",
                    createdAt = now,
                ),
            )
        }
        true
    }

    suspend fun createProfile(nickname: String): GameResult = database.transaction {
        val cleanName = nickname.trim().take(16)
        if (cleanName.isBlank()) return@transaction GameResult.Error("Придумай игровое имя")
        dao.getProfile()?.let { dao.deleteProfile(it.profileId) }
        val now = System.currentTimeMillis()
        dao.insertProfile(
            ProfileEntity(
                profileId = newId(),
                nickname = cleanName,
                avatarId = "star",
                isDemo = false,
                createdAt = now,
                updatedAt = now,
            ),
        )
        GameResult.Success("Профиль готов")
    }

    suspend fun createPet(
        name: String,
        speciesId: String,
        colorId: String,
        accessoryId: String?,
    ): GameResult = database.transaction {
        val profile = dao.getProfile() ?: return@transaction GameResult.Error("Сначала создай профиль")
        val cleanName = name.trim().take(16)
        if (cleanName.isBlank()) return@transaction GameResult.Error("Дай питомцу имя")
        val now = System.currentTimeMillis()
        dao.insertPet(
            PetEntity(
                petId = newId(),
                profileId = profile.profileId,
                name = cleanName,
                speciesId = speciesId,
                colorId = colorId,
                accessoryId = accessoryId?.takeUnless { it == "none" },
                hasShoes = false,
                hasGlasses = false,
                hasJetpack = false,
                stage = GrowthStage.BABY,
                growthPoints = 0,
                satiety = 70,
                mood = 70,
                comfort = 70,
                updatedAt = now,
            ),
        )
        dao.upsertStepTracking(
            StepTrackingEntity(profile.profileId, null, 0, 0, now),
        )
        if (dao.getActivePeriod(profile.profileId) == null) {
            startPeriodLocked(profile, 1)
        }
        GameResult.Success("${cleanName} готов к приключениям!")
    }

    suspend fun createDemoProfile(): GameResult = database.transaction {
        dao.getProfile()?.let { dao.deleteProfile(it.profileId) }
        val now = System.currentTimeMillis()
        val profile = ProfileEntity(
            profileId = "demo-profile",
            nickname = "Эксперт",
            avatarId = "star",
            isDemo = true,
            createdAt = now,
            updatedAt = now,
        )
        dao.insertProfile(profile)
        dao.insertPet(
            PetEntity(
                petId = "demo-pet",
                profileId = profile.profileId,
                name = "Плюш",
                speciesId = "fox",
                colorId = "mint",
                accessoryId = "helmet",
                hasShoes = false,
                hasGlasses = false,
                hasJetpack = false,
                stage = GrowthStage.BABY,
                growthPoints = 0,
                satiety = 70,
                mood = 70,
                comfort = 70,
                updatedAt = now,
            ),
        )
        dao.upsertStepTracking(
            StepTrackingEntity(profile.profileId, null, 0, 0, now),
        )
        startPeriodLocked(profile, 1)
        GameResult.Success("Демопрофиль готов")
    }

    suspend fun confirmBudget(required: Int, optional: Int, savings: Int): GameResult = database.transaction {
        val profile = dao.getProfile() ?: return@transaction GameResult.Error("Профиль не найден")
        val period = dao.getActivePeriod(profile.profileId) ?: return@transaction GameResult.Error("Нет активного периода")
        val current = dao.getBudgetPlan(period.periodId) ?: return@transaction GameResult.Error("План не найден")
        EconomyRules.validatePlan(current.baseAvailable, required, optional, savings)?.let {
            return@transaction GameResult.Error(it)
        }
        val total = required + optional + savings
        val now = System.currentTimeMillis()
        val changed = dao.confirmPlan(
            periodId = period.periodId,
            required = required,
            optional = optional,
            savings = savings,
            buffer = current.baseAvailable - total,
            status = PlanStatus.CONFIRMED,
            confirmedAt = now,
            updatedAt = now,
        )
        if (changed == 0) GameResult.Error("План уже подтверждён")
        else GameResult.Success("План сохранён. Теперь проверим его в деле!")
    }

    suspend fun completeTask(taskId: String, actionsSnapshot: String): GameResult = database.transaction {
        val task = GameCatalog.task(taskId) ?: return@transaction GameResult.Error("Задание не найдено")
        val profile = dao.getProfile() ?: return@transaction GameResult.Error("Профиль не найден")
        if (dao.isTaskCompleted(profile.profileId, taskId) > 0) {
            return@transaction GameResult.Error("Это задание уже выполнено")
        }
        val period = dao.getActivePeriod(profile.profileId)
            ?: return@transaction GameResult.Error("Сначала начни игровой период")
        val plan = dao.getBudgetPlan(period.periodId)
        if (plan?.status != PlanStatus.CONFIRMED) {
            return@transaction GameResult.Error("Сначала подтверди план бюджета")
        }
        val wallet = currentWallet(profile.profileId)
        val attemptId = newId()
        val operationKey = "task-reward:$attemptId"
        val rowId = dao.insertLedgerEntry(
            LedgerEntryEntity(
                entryId = newId(),
                operationKey = operationKey,
                profileId = profile.profileId,
                periodId = period.periodId,
                operationType = OperationType.TASK_REWARD,
                budgetCategory = null,
                availableDelta = task.reward,
                savingsDelta = 0,
                availableAfter = wallet.available + task.reward,
                savingsAfter = wallet.savings,
                sourceId = task.id,
                explanation = "Награда за задание «${task.title}»: +${task.reward}",
                createdAt = System.currentTimeMillis(),
            ),
        )
        val now = System.currentTimeMillis()
        dao.insertTaskAttempt(
            TaskAttemptEntity(
                attemptId = attemptId,
                profileId = profile.profileId,
                periodId = period.periodId,
                taskId = task.id,
                contentVersion = CONTENT_VERSION,
                resultCode = "SUCCESS",
                actionsSnapshot = actionsSnapshot,
                reward = task.reward,
                rewardLedgerRowId = rowId,
                startedAt = now,
                completedAt = now,
            ),
        )
        GameResult.Success("${task.successText} Награда: ${task.reward} монет")
    }

    suspend fun purchase(itemId: String): GameResult = database.transaction {
        val item = GameCatalog.shopItem(itemId) ?: return@transaction GameResult.Error("Товар не найден")
        val profile = dao.getProfile() ?: return@transaction GameResult.Error("Профиль не найден")
        val pet = dao.getPet(profile.profileId) ?: return@transaction GameResult.Error("Питомец не найден")
        if (item.id == "helmet" && pet.accessoryId in setOf("helmet", "scarf")) {
            return@transaction GameResult.Error("Шлем уже надет на героя")
        }
        if (item.id == "shoes" && pet.hasShoes) {
            return@transaction GameResult.Error("Энергообувь уже надета на героя")
        }
        if (item.id == "glasses" && pet.hasGlasses) {
            return@transaction GameResult.Error("Очки уже надеты на героя")
        }
        if (item.id == "jetpack" && pet.hasJetpack) {
            return@transaction GameResult.Error("Реактивный рюкзак уже надет на героя")
        }
        val period = dao.getActivePeriod(profile.profileId) ?: return@transaction GameResult.Error("Нет активного периода")
        val plan = dao.getBudgetPlan(period.periodId)
        if (plan?.status != PlanStatus.CONFIRMED) {
            return@transaction GameResult.Error("Сначала подтверди план бюджета")
        }
        val wallet = currentWallet(profile.profileId)
        if (wallet.available < item.price) {
            val missing = item.price - wallet.available
            return@transaction GameResult.Error(
                "Не хватает $missing монет. Выбери дешевле, выполни задание или перенеси покупку.",
            )
        }
        val purchaseId = newId()
        val now = System.currentTimeMillis()
        val rowId = dao.insertLedgerEntry(
            LedgerEntryEntity(
                entryId = newId(),
                operationKey = "purchase:$purchaseId",
                profileId = profile.profileId,
                periodId = period.periodId,
                operationType = OperationType.PURCHASE,
                budgetCategory = item.category,
                availableDelta = -item.price,
                savingsDelta = 0,
                availableAfter = wallet.available - item.price,
                savingsAfter = wallet.savings,
                sourceId = item.id,
                explanation = "Покупка «${item.title}»: −${item.price}",
                createdAt = now,
            ),
        )
        dao.insertPurchase(
            PurchaseEntity(
                purchaseId = purchaseId,
                profileId = profile.profileId,
                periodId = period.periodId,
                ledgerRowId = rowId,
                catalogItemId = item.id,
                titleSnapshot = item.title,
                categorySnapshot = item.category,
                priceSnapshot = item.price,
                satietyDelta = item.satietyDelta,
                moodDelta = item.moodDelta,
                comfortDelta = item.comfortDelta,
                purchasedAt = now,
            ),
        )
        val updatedPet = pet.copy(
            accessoryId = if (item.id == "helmet") "helmet" else pet.accessoryId,
            hasShoes = pet.hasShoes || item.id == "shoes",
            hasGlasses = pet.hasGlasses || item.id == "glasses",
            hasJetpack = pet.hasJetpack || item.id == "jetpack",
            satiety = (pet.satiety + item.satietyDelta).coerceIn(0, 100),
            mood = (pet.mood + item.moodDelta).coerceIn(0, 100),
            comfort = (pet.comfort + item.comfortDelta).coerceIn(0, 100),
            updatedAt = now,
        )
        dao.updatePet(updatedPet)
        dao.insertPetStateEvent(
            PetStateEventEntity(
                eventId = newId(),
                petId = pet.petId,
                periodId = period.periodId,
                reasonType = "PURCHASE",
                reasonId = purchaseId,
                satietyDelta = item.satietyDelta,
                moodDelta = item.moodDelta,
                comfortDelta = item.comfortDelta,
                growthDelta = 0,
                satietyAfter = updatedPet.satiety,
                moodAfter = updatedPet.mood,
                comfortAfter = updatedPet.comfort,
                growthAfter = updatedPet.growthPoints,
                message = "${item.title} помогает питомцу: ${item.description.lowercase()}",
                createdAt = now,
            ),
        )
        GameResult.Success("${item.emoji} ${item.title} куплен. Баланс −${item.price}")
    }

    suspend fun selectGoal(goalId: String): GameResult = database.transaction {
        val goal = GameCatalog.goal(goalId) ?: return@transaction GameResult.Error("Цель не найдена")
        val profile = dao.getProfile() ?: return@transaction GameResult.Error("Профиль не найден")
        dao.pauseActiveGoals(profile.profileId)
        dao.insertGoalProgress(
            GoalProgressEntity(
                goalProgressId = newId(),
                profileId = profile.profileId,
                goalId = goal.id,
                titleSnapshot = goal.title,
                targetSnapshot = goal.target,
                status = GoalStatus.ACTIVE,
                selectedAt = System.currentTimeMillis(),
                completedAt = null,
                savingsAtFinish = null,
            ),
        )
        GameResult.Success("Новая мечта — ${goal.title}")
    }

    suspend fun depositSavings(amount: Int): GameResult = database.transaction {
        if (amount <= 0) return@transaction GameResult.Error("Выбери сумму пополнения")
        val profile = dao.getProfile() ?: return@transaction GameResult.Error("Профиль не найден")
        val goal = dao.getActiveGoal(profile.profileId) ?: return@transaction GameResult.Error("Сначала выбери мечту")
        val period = dao.getActivePeriod(profile.profileId) ?: return@transaction GameResult.Error("Нет активного периода")
        val wallet = currentWallet(profile.profileId)
        if (wallet.available < amount) {
            return@transaction GameResult.Error("Для пополнения не хватает ${amount - wallet.available} монет")
        }
        val entryId = newId()
        val newSavings = wallet.savings + amount
        dao.insertLedgerEntry(
            LedgerEntryEntity(
                entryId = entryId,
                operationKey = "savings-deposit:$entryId",
                profileId = profile.profileId,
                periodId = period.periodId,
                operationType = OperationType.SAVINGS_DEPOSIT,
                budgetCategory = null,
                availableDelta = -amount,
                savingsDelta = amount,
                availableAfter = wallet.available - amount,
                savingsAfter = newSavings,
                sourceId = goal.goalProgressId,
                explanation = "Пополнение цели «${goal.titleSnapshot}»: +$amount",
                createdAt = System.currentTimeMillis(),
            ),
        )
        if (newSavings >= goal.targetSnapshot) {
            dao.updateGoalStatus(
                goalProgressId = goal.goalProgressId,
                status = GoalStatus.COMPLETED,
                completedAt = System.currentTimeMillis(),
                saved = newSavings,
            )
            GameResult.Success("Мечта накоплена! ${goal.titleSnapshot} уже доступен")
        } else {
            GameResult.Success("В мечту добавлено $amount. Осталось ${goal.targetSnapshot - newSavings}")
        }
    }

    suspend fun withdrawSavings(amount: Int): GameResult = database.transaction {
        if (amount <= 0) return@transaction GameResult.Error("Выбери сумму")
        val profile = dao.getProfile() ?: return@transaction GameResult.Error("Профиль не найден")
        val goal = dao.getActiveGoal(profile.profileId) ?: return@transaction GameResult.Error("Нет активной цели")
        val period = dao.getActivePeriod(profile.profileId) ?: return@transaction GameResult.Error("Нет активного периода")
        val wallet = currentWallet(profile.profileId)
        if (wallet.savings < amount) return@transaction GameResult.Error("В накоплениях меньше $amount монет")
        val entryId = newId()
        dao.insertLedgerEntry(
            LedgerEntryEntity(
                entryId = entryId,
                operationKey = "savings-withdraw:$entryId",
                profileId = profile.profileId,
                periodId = period.periodId,
                operationType = OperationType.SAVINGS_WITHDRAWAL,
                budgetCategory = null,
                availableDelta = amount,
                savingsDelta = -amount,
                availableAfter = wallet.available + amount,
                savingsAfter = wallet.savings - amount,
                sourceId = goal.goalProgressId,
                explanation = "Снятие с цели «${goal.titleSnapshot}»: −$amount",
                createdAt = System.currentTimeMillis(),
            ),
        )
        GameResult.Success("$amount монет возвращены в доступный баланс")
    }

    suspend fun completePeriod(): GameResult = database.transaction {
        val profile = dao.getProfile() ?: return@transaction GameResult.Error("Профиль не найден")
        val pet = dao.getPet(profile.profileId) ?: return@transaction GameResult.Error("Питомец не найден")
        val period = dao.getActivePeriod(profile.profileId) ?: return@transaction GameResult.Error("Нет активного периода")
        val planEntity = dao.getBudgetPlan(period.periodId) ?: return@transaction GameResult.Error("Нет плана")
        if (planEntity.status != PlanStatus.CONFIRMED) {
            return@transaction GameResult.Error("Сначала подтверди план")
        }
        val plan = BudgetPlanData(
            period.id(), planEntity.baseAvailable, planEntity.requiredPlanned,
            planEntity.optionalPlanned, planEntity.savingsPlanned, planEntity.bufferPlanned,
            planEntity.status,
        )
        val requiredActual = dao.actualSpent(period.periodId, BudgetCategory.REQUIRED)
        val optionalActual = dao.actualSpent(period.periodId, BudgetCategory.OPTIONAL)
        val deposits = dao.savingsDelta(period.periodId, OperationType.SAVINGS_DEPOSIT)
        val withdrawn = -dao.savingsDelta(period.periodId, OperationType.SAVINGS_WITHDRAWAL)
        val requiredPurchases = dao.purchaseCount(period.periodId, BudgetCategory.REQUIRED)
        val income = dao.sumAvailableDelta(
            period.periodId,
            listOf(OperationType.PERIOD_INCOME, OperationType.TASK_REWARD),
        )
        val growth = EconomyRules.growthAward(
            requiredPurchases, requiredActual, optionalActual, deposits, plan,
        )
        val wallet = currentWallet(profile.profileId)
        val essentialsScore = if (requiredPurchases > 0) 10 else 0
        val planScore = if (requiredActual <= plan.required && optionalActual <= plan.optional) 5 else 0
        val savingsScore = if (deposits >= plan.savings) 5 else 0
        val feedback = when {
            growth == 20 -> "Отличный период: нужное куплено, план соблюдён, мечта стала ближе."
            growth >= 10 -> "Хорошая работа! В следующем периоде можно точнее свериться с планом."
            else -> "Это была тренировка. Начни следующий план с заботы и небольшого взноса."
        }
        val now = System.currentTimeMillis()
        val newGrowth = pet.growthPoints + growth
        val newPet = pet.copy(
            growthPoints = newGrowth,
            stage = GrowthStage.fromPoints(newGrowth),
            updatedAt = now,
        )
        dao.updatePet(newPet)
        dao.insertPetStateEvent(
            PetStateEventEntity(
                eventId = newId(),
                petId = pet.petId,
                periodId = period.periodId,
                reasonType = "PERIOD_COMPLETE",
                reasonId = period.periodId,
                satietyDelta = 0,
                moodDelta = 0,
                comfortDelta = 0,
                growthDelta = growth,
                satietyAfter = newPet.satiety,
                moodAfter = newPet.mood,
                comfortAfter = newPet.comfort,
                growthAfter = newPet.growthPoints,
                message = feedback,
                createdAt = now,
            ),
        )
        dao.insertPeriodSummary(
            PeriodSummaryEntity(
                periodId = period.periodId,
                periodNumber = period.periodNumber,
                incomeTotal = income,
                requiredActual = requiredActual,
                optionalActual = optionalActual,
                savingsDeposited = deposits,
                savingsWithdrawn = withdrawn,
                closingAvailable = wallet.available,
                closingSavings = wallet.savings,
                essentialsScore = essentialsScore,
                planScore = planScore,
                savingsScore = savingsScore,
                growthAwarded = growth,
                feedback = feedback,
                createdAt = now,
            ),
        )
        dao.updatePeriodStatus(period.periodId, PeriodStatus.COMPLETED, now)
        GameResult.Success("Период завершён. Питомец получил $growth очков роста")
    }

    suspend fun startNextPeriod(): GameResult = database.transaction {
        val profile = dao.getProfile() ?: return@transaction GameResult.Error("Профиль не найден")
        if (dao.getActivePeriod(profile.profileId) != null) {
            return@transaction GameResult.Error("Текущий период ещё не завершён")
        }
        val pet = dao.getPet(profile.profileId) ?: return@transaction GameResult.Error("Питомец не найден")
        val latest = dao.getLatestPeriod(profile.profileId)
        val nextNumber = (latest?.periodNumber ?: 0) + 1
        dao.updatePet(
            pet.copy(
                mood = (pet.mood - 5).coerceAtLeast(20),
                comfort = (pet.comfort - 5).coerceAtLeast(20),
                updatedAt = System.currentTimeMillis(),
            ),
        )
        startPeriodLocked(profile, nextNumber)
        GameResult.Success("Начался период $nextNumber. Доход: +100 монет")
    }

    suspend fun deleteProfile(): GameResult = database.transaction {
        val profile = dao.getProfile() ?: return@transaction GameResult.Success("Данных уже нет")
        dao.deleteProfile(profile.profileId)
        GameResult.Success("Локальный профиль удалён")
    }

    suspend fun resetDemoProfile(): GameResult = createDemoProfile()

    private fun startPeriodLocked(profile: ProfileEntity, number: Int) {
        val wallet = currentWallet(profile.profileId)
        val now = System.currentTimeMillis()
        val periodId = newId()
        dao.insertPeriod(
            GamePeriodEntity(
                periodId = periodId,
                profileId = profile.profileId,
                periodNumber = number,
                templateId = "period_${((number - 1) % 5) + 1}",
                contentVersion = CONTENT_VERSION,
                status = PeriodStatus.ACTIVE,
                openingAvailable = wallet.available,
                openingSavings = wallet.savings,
                startedAt = now,
                completedAt = null,
            ),
        )
        val income = 100
        dao.insertLedgerEntry(
            LedgerEntryEntity(
                entryId = newId(),
                operationKey = "period-income:$periodId",
                profileId = profile.profileId,
                periodId = periodId,
                operationType = OperationType.PERIOD_INCOME,
                budgetCategory = null,
                availableDelta = income,
                savingsDelta = 0,
                availableAfter = wallet.available + income,
                savingsAfter = wallet.savings,
                sourceId = periodId,
                explanation = "Доход за период $number: +$income",
                createdAt = now,
            ),
        )
        dao.insertBudgetPlan(
            BudgetPlanEntity(
                periodId = periodId,
                baseAvailable = wallet.available + income,
                requiredPlanned = 0,
                optionalPlanned = 0,
                savingsPlanned = 0,
                bufferPlanned = wallet.available + income,
                status = PlanStatus.DRAFT,
                confirmedAt = null,
                updatedAt = now,
            ),
        )
    }

    private fun currentWallet(profileId: String): Wallet {
        val latest = dao.getLatestLedgerEntry(profileId)
        return Wallet(
            available = latest?.availableAfter ?: 0,
            savings = latest?.savingsAfter ?: 0,
            revision = latest?.rowId ?: 0,
        )
    }

    private fun PeriodSummaryEntity.toDomain() = PeriodSummaryData(
        periodId = periodId,
        periodNumber = periodNumber,
        income = incomeTotal,
        requiredActual = requiredActual,
        optionalActual = optionalActual,
        savingsDeposited = savingsDeposited,
        closingAvailable = closingAvailable,
        closingSavings = closingSavings,
        growthAwarded = growthAwarded,
        feedback = feedback,
    )

    private fun GamePeriodEntity.id(): String = periodId
    private fun newId(): String = UUID.randomUUID().toString()

    private companion object {
        const val CONTENT_VERSION = "2026.1"
    }
}
