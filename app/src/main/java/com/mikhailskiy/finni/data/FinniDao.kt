package com.mikhailskiy.finni.data

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.mikhailskiy.finni.domain.BudgetCategory
import com.mikhailskiy.finni.domain.GoalStatus
import com.mikhailskiy.finni.domain.GrowthStage
import com.mikhailskiy.finni.domain.OperationType
import com.mikhailskiy.finni.domain.PeriodStatus
import com.mikhailskiy.finni.domain.PlanStatus

class FinniDao(private val helper: FinniDatabase) {
    private val db: SQLiteDatabase get() = helper.writableDatabase

    fun getProfile(): ProfileEntity? = db.query(
        "profiles", null, null, null, null, null, "created_at ASC", "1",
    ).use { cursor -> if (cursor.moveToFirst()) cursor.toProfile() else null }

    fun insertProfile(profile: ProfileEntity) {
        db.insertOrThrow("profiles", null, values(
            "profile_id" to profile.profileId,
            "nickname" to profile.nickname,
            "avatar_id" to profile.avatarId,
            "is_demo" to profile.isDemo,
            "created_at" to profile.createdAt,
            "updated_at" to profile.updatedAt,
        ))
    }

    fun deleteProfile(profileId: String) {
        db.delete("profiles", "profile_id = ?", arrayOf(profileId))
    }

    fun getPet(profileId: String): PetEntity? = db.query(
        "pets", null, "profile_id = ?", arrayOf(profileId), null, null, null, "1",
    ).use { cursor -> if (cursor.moveToFirst()) cursor.toPet() else null }

    fun insertPet(pet: PetEntity) {
        db.insertWithOnConflict("pets", null, pet.values(), SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun updatePet(pet: PetEntity) {
        db.update("pets", pet.values(), "pet_id = ?", arrayOf(pet.petId))
    }

    fun getStepTracking(profileId: String): StepTrackingEntity? = db.query(
        "step_tracking", null, "profile_id = ?", arrayOf(profileId),
        null, null, null, "1",
    ).use { cursor -> if (cursor.moveToFirst()) cursor.toStepTracking() else null }

    fun upsertStepTracking(stepTracking: StepTrackingEntity) {
        db.insertWithOnConflict(
            "step_tracking",
            null,
            values(
                "profile_id" to stepTracking.profileId,
                "last_sensor_total" to stepTracking.lastSensorTotal,
                "tracked_steps" to stepTracking.trackedSteps,
                "pending_steps" to stepTracking.pendingSteps,
                "updated_at" to stepTracking.updatedAt,
            ),
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    fun getActivePeriod(profileId: String): GamePeriodEntity? = db.query(
        "game_periods", null,
        "profile_id = ? AND status = ?",
        arrayOf(profileId, PeriodStatus.ACTIVE.name),
        null, null, "period_number DESC", "1",
    ).use { cursor -> if (cursor.moveToFirst()) cursor.toPeriod() else null }

    fun getLatestPeriod(profileId: String): GamePeriodEntity? = db.query(
        "game_periods", null, "profile_id = ?", arrayOf(profileId),
        null, null, "period_number DESC", "1",
    ).use { cursor -> if (cursor.moveToFirst()) cursor.toPeriod() else null }

    fun insertPeriod(period: GamePeriodEntity) {
        db.insertOrThrow("game_periods", null, values(
            "period_id" to period.periodId,
            "profile_id" to period.profileId,
            "period_number" to period.periodNumber,
            "template_id" to period.templateId,
            "content_version" to period.contentVersion,
            "status" to period.status.name,
            "opening_available" to period.openingAvailable,
            "opening_savings" to period.openingSavings,
            "started_at" to period.startedAt,
            "completed_at" to period.completedAt,
        ))
    }

    fun updatePeriodStatus(periodId: String, status: PeriodStatus, completedAt: Long?) {
        db.update(
            "game_periods",
            values("status" to status.name, "completed_at" to completedAt),
            "period_id = ?",
            arrayOf(periodId),
        )
    }

    fun getBudgetPlan(periodId: String): BudgetPlanEntity? = db.query(
        "budget_plans", null, "period_id = ?", arrayOf(periodId),
        null, null, null, "1",
    ).use { cursor -> if (cursor.moveToFirst()) cursor.toBudgetPlan() else null }

    fun insertBudgetPlan(plan: BudgetPlanEntity) {
        db.insertWithOnConflict("budget_plans", null, values(
            "period_id" to plan.periodId,
            "base_available" to plan.baseAvailable,
            "required_planned" to plan.requiredPlanned,
            "optional_planned" to plan.optionalPlanned,
            "savings_planned" to plan.savingsPlanned,
            "buffer_planned" to plan.bufferPlanned,
            "status" to plan.status.name,
            "confirmed_at" to plan.confirmedAt,
            "updated_at" to plan.updatedAt,
        ), SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun confirmPlan(
        periodId: String,
        required: Int,
        optional: Int,
        savings: Int,
        buffer: Int,
        status: PlanStatus,
        confirmedAt: Long,
        updatedAt: Long,
    ): Int = db.update(
        "budget_plans",
        values(
            "required_planned" to required,
            "optional_planned" to optional,
            "savings_planned" to savings,
            "buffer_planned" to buffer,
            "status" to status.name,
            "confirmed_at" to confirmedAt,
            "updated_at" to updatedAt,
        ),
        "period_id = ? AND status = ?",
        arrayOf(periodId, PlanStatus.DRAFT.name),
    )

    fun getLatestLedgerEntry(profileId: String): LedgerEntryEntity? = db.query(
        "ledger_entries", null, "profile_id = ?", arrayOf(profileId),
        null, null, "row_id DESC", "1",
    ).use { cursor -> if (cursor.moveToFirst()) cursor.toLedgerEntry() else null }

    fun insertLedgerEntry(entry: LedgerEntryEntity): Long = db.insertOrThrow(
        "ledger_entries", null, values(
            "entry_id" to entry.entryId,
            "operation_key" to entry.operationKey,
            "profile_id" to entry.profileId,
            "period_id" to entry.periodId,
            "operation_type" to entry.operationType.name,
            "budget_category" to entry.budgetCategory?.name,
            "available_delta" to entry.availableDelta,
            "savings_delta" to entry.savingsDelta,
            "available_after" to entry.availableAfter,
            "savings_after" to entry.savingsAfter,
            "source_id" to entry.sourceId,
            "explanation" to entry.explanation,
            "created_at" to entry.createdAt,
        ),
    )

    fun sumAvailableDelta(periodId: String, types: List<OperationType>): Int {
        if (types.isEmpty()) return 0
        val placeholders = types.joinToString(",") { "?" }
        val args = arrayOf(periodId, *types.map { it.name }.toTypedArray())
        return scalarInt(
            "SELECT COALESCE(SUM(available_delta), 0) FROM ledger_entries WHERE period_id = ? AND operation_type IN ($placeholders)",
            args,
        )
    }

    fun actualSpent(periodId: String, category: BudgetCategory): Int = scalarInt(
        "SELECT COALESCE(SUM(-available_delta), 0) FROM ledger_entries WHERE period_id = ? AND operation_type = ? AND budget_category = ?",
        arrayOf(periodId, OperationType.PURCHASE.name, category.name),
    )

    fun savingsDelta(periodId: String, operationType: OperationType): Int = scalarInt(
        "SELECT COALESCE(SUM(savings_delta), 0) FROM ledger_entries WHERE period_id = ? AND operation_type = ?",
        arrayOf(periodId, operationType.name),
    )

    fun operationExists(operationKey: String): Int = scalarInt(
        "SELECT COUNT(*) FROM ledger_entries WHERE operation_key = ?", arrayOf(operationKey),
    )

    fun insertPurchase(purchase: PurchaseEntity) {
        db.insertOrThrow("purchases", null, values(
            "purchase_id" to purchase.purchaseId,
            "profile_id" to purchase.profileId,
            "period_id" to purchase.periodId,
            "ledger_row_id" to purchase.ledgerRowId,
            "catalog_item_id" to purchase.catalogItemId,
            "title_snapshot" to purchase.titleSnapshot,
            "category_snapshot" to purchase.categorySnapshot.name,
            "price_snapshot" to purchase.priceSnapshot,
            "satiety_delta" to purchase.satietyDelta,
            "mood_delta" to purchase.moodDelta,
            "comfort_delta" to purchase.comfortDelta,
            "purchased_at" to purchase.purchasedAt,
        ))
    }

    fun getPurchases(periodId: String): List<PurchaseEntity> = db.query(
        "purchases", null, "period_id = ?", arrayOf(periodId),
        null, null, "purchased_at DESC",
    ).use { cursor -> cursor.collect { it.toPurchase() } }

    fun purchaseCount(periodId: String, category: BudgetCategory): Int = scalarInt(
        "SELECT COUNT(*) FROM purchases WHERE period_id = ? AND category_snapshot = ?",
        arrayOf(periodId, category.name),
    )

    fun pauseActiveGoals(profileId: String) {
        db.update(
            "goal_progress", values("status" to GoalStatus.PAUSED.name),
            "profile_id = ? AND status = ?", arrayOf(profileId, GoalStatus.ACTIVE.name),
        )
    }

    fun insertGoalProgress(goal: GoalProgressEntity) {
        db.insertWithOnConflict("goal_progress", null, values(
            "goal_progress_id" to goal.goalProgressId,
            "profile_id" to goal.profileId,
            "goal_id" to goal.goalId,
            "title_snapshot" to goal.titleSnapshot,
            "target_snapshot" to goal.targetSnapshot,
            "status" to goal.status.name,
            "selected_at" to goal.selectedAt,
            "completed_at" to goal.completedAt,
            "savings_at_finish" to goal.savingsAtFinish,
        ), SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getActiveGoal(profileId: String): GoalProgressEntity? = db.query(
        "goal_progress", null,
        "profile_id = ? AND status = ?", arrayOf(profileId, GoalStatus.ACTIVE.name),
        null, null, "selected_at DESC", "1",
    ).use { cursor -> if (cursor.moveToFirst()) cursor.toGoal() else null }

    fun updateGoalStatus(goalProgressId: String, status: GoalStatus, completedAt: Long?, saved: Int?) {
        db.update(
            "goal_progress",
            values("status" to status.name, "completed_at" to completedAt, "savings_at_finish" to saved),
            "goal_progress_id = ?", arrayOf(goalProgressId),
        )
    }

    fun insertTaskAttempt(attempt: TaskAttemptEntity) {
        db.insertOrThrow("task_attempts", null, values(
            "attempt_id" to attempt.attemptId,
            "profile_id" to attempt.profileId,
            "period_id" to attempt.periodId,
            "task_id" to attempt.taskId,
            "content_version" to attempt.contentVersion,
            "result_code" to attempt.resultCode,
            "actions_snapshot" to attempt.actionsSnapshot,
            "reward" to attempt.reward,
            "reward_ledger_row_id" to attempt.rewardLedgerRowId,
            "started_at" to attempt.startedAt,
            "completed_at" to attempt.completedAt,
        ))
    }

    fun getCompletedTaskIds(profileId: String): List<String> = db.query(
        "task_attempts", arrayOf("task_id"), "profile_id = ?", arrayOf(profileId),
        null, null, null,
    ).use { cursor -> cursor.collect { it.string("task_id") } }

    fun isTaskCompleted(profileId: String, taskId: String): Int = scalarInt(
        "SELECT COUNT(*) FROM task_attempts WHERE profile_id = ? AND task_id = ?",
        arrayOf(profileId, taskId),
    )

    fun insertPetStateEvent(event: PetStateEventEntity) {
        db.insertOrThrow("pet_state_events", null, values(
            "event_id" to event.eventId,
            "pet_id" to event.petId,
            "period_id" to event.periodId,
            "reason_type" to event.reasonType,
            "reason_id" to event.reasonId,
            "satiety_delta" to event.satietyDelta,
            "mood_delta" to event.moodDelta,
            "comfort_delta" to event.comfortDelta,
            "growth_delta" to event.growthDelta,
            "satiety_after" to event.satietyAfter,
            "mood_after" to event.moodAfter,
            "comfort_after" to event.comfortAfter,
            "growth_after" to event.growthAfter,
            "message" to event.message,
            "created_at" to event.createdAt,
        ))
    }

    fun insertPeriodSummary(summary: PeriodSummaryEntity) {
        db.insertOrThrow("period_summaries", null, values(
            "period_id" to summary.periodId,
            "period_number" to summary.periodNumber,
            "income_total" to summary.incomeTotal,
            "required_actual" to summary.requiredActual,
            "optional_actual" to summary.optionalActual,
            "savings_deposited" to summary.savingsDeposited,
            "savings_withdrawn" to summary.savingsWithdrawn,
            "closing_available" to summary.closingAvailable,
            "closing_savings" to summary.closingSavings,
            "essentials_score" to summary.essentialsScore,
            "plan_score" to summary.planScore,
            "savings_score" to summary.savingsScore,
            "growth_awarded" to summary.growthAwarded,
            "feedback" to summary.feedback,
            "created_at" to summary.createdAt,
        ))
    }

    fun getPeriodSummaries(): List<PeriodSummaryEntity> = db.query(
        "period_summaries", null, null, null, null, null, "period_number DESC",
    ).use { cursor -> cursor.collect { it.toSummary() } }

    fun getPeriodSummary(periodId: String): PeriodSummaryEntity? = db.query(
        "period_summaries", null, "period_id = ?", arrayOf(periodId),
        null, null, null, "1",
    ).use { cursor -> if (cursor.moveToFirst()) cursor.toSummary() else null }

    private fun PetEntity.values() = values(
        "pet_id" to petId,
        "profile_id" to profileId,
        "name" to name,
        "species_id" to speciesId,
        "color_id" to colorId,
        "accessory_id" to accessoryId,
        "has_shoes" to hasShoes,
        "has_glasses" to hasGlasses,
        "has_jetpack" to hasJetpack,
        "stage" to stage.name,
        "growth_points" to growthPoints,
        "satiety" to satiety,
        "mood" to mood,
        "comfort" to comfort,
        "updated_at" to updatedAt,
    )

    private fun scalarInt(sql: String, args: Array<String>): Int = db.rawQuery(sql, args).use { cursor ->
        if (cursor.moveToFirst()) cursor.getInt(0) else 0
    }

    private fun values(vararg entries: Pair<String, Any?>): ContentValues = ContentValues().apply {
        entries.forEach { (key, value) ->
            when (value) {
                null -> putNull(key)
                is String -> put(key, value)
                is Int -> put(key, value)
                is Long -> put(key, value)
                is Boolean -> put(key, if (value) 1 else 0)
                else -> error("Unsupported SQLite value ${value::class}")
            }
        }
    }

    private fun Cursor.toProfile() = ProfileEntity(
        profileId = string("profile_id"),
        nickname = string("nickname"),
        avatarId = string("avatar_id"),
        isDemo = int("is_demo") == 1,
        createdAt = long("created_at"),
        updatedAt = long("updated_at"),
    )

    private fun Cursor.toPet() = PetEntity(
        petId = string("pet_id"),
        profileId = string("profile_id"),
        name = string("name"),
        speciesId = string("species_id"),
        colorId = string("color_id"),
        accessoryId = nullableString("accessory_id"),
        hasShoes = int("has_shoes") == 1,
        hasGlasses = int("has_glasses") == 1,
        hasJetpack = int("has_jetpack") == 1,
        stage = GrowthStage.valueOf(string("stage")),
        growthPoints = int("growth_points"),
        satiety = int("satiety"),
        mood = int("mood"),
        comfort = int("comfort"),
        updatedAt = long("updated_at"),
    )

    private fun Cursor.toPeriod() = GamePeriodEntity(
        periodId = string("period_id"),
        profileId = string("profile_id"),
        periodNumber = int("period_number"),
        templateId = string("template_id"),
        contentVersion = string("content_version"),
        status = PeriodStatus.valueOf(string("status")),
        openingAvailable = int("opening_available"),
        openingSavings = int("opening_savings"),
        startedAt = long("started_at"),
        completedAt = nullableLong("completed_at"),
    )

    private fun Cursor.toStepTracking() = StepTrackingEntity(
        profileId = string("profile_id"),
        lastSensorTotal = nullableLong("last_sensor_total"),
        trackedSteps = long("tracked_steps"),
        pendingSteps = int("pending_steps"),
        updatedAt = long("updated_at"),
    )

    private fun Cursor.toBudgetPlan() = BudgetPlanEntity(
        periodId = string("period_id"),
        baseAvailable = int("base_available"),
        requiredPlanned = int("required_planned"),
        optionalPlanned = int("optional_planned"),
        savingsPlanned = int("savings_planned"),
        bufferPlanned = int("buffer_planned"),
        status = PlanStatus.valueOf(string("status")),
        confirmedAt = nullableLong("confirmed_at"),
        updatedAt = long("updated_at"),
    )

    private fun Cursor.toLedgerEntry() = LedgerEntryEntity(
        rowId = long("row_id"),
        entryId = string("entry_id"),
        operationKey = string("operation_key"),
        profileId = string("profile_id"),
        periodId = nullableString("period_id"),
        operationType = OperationType.valueOf(string("operation_type")),
        budgetCategory = nullableString("budget_category")?.let(BudgetCategory::valueOf),
        availableDelta = int("available_delta"),
        savingsDelta = int("savings_delta"),
        availableAfter = int("available_after"),
        savingsAfter = int("savings_after"),
        sourceId = nullableString("source_id"),
        explanation = string("explanation"),
        createdAt = long("created_at"),
    )

    private fun Cursor.toPurchase() = PurchaseEntity(
        purchaseId = string("purchase_id"),
        profileId = string("profile_id"),
        periodId = string("period_id"),
        ledgerRowId = long("ledger_row_id"),
        catalogItemId = string("catalog_item_id"),
        titleSnapshot = string("title_snapshot"),
        categorySnapshot = BudgetCategory.valueOf(string("category_snapshot")),
        priceSnapshot = int("price_snapshot"),
        satietyDelta = int("satiety_delta"),
        moodDelta = int("mood_delta"),
        comfortDelta = int("comfort_delta"),
        purchasedAt = long("purchased_at"),
    )

    private fun Cursor.toGoal() = GoalProgressEntity(
        goalProgressId = string("goal_progress_id"),
        profileId = string("profile_id"),
        goalId = string("goal_id"),
        titleSnapshot = string("title_snapshot"),
        targetSnapshot = int("target_snapshot"),
        status = GoalStatus.valueOf(string("status")),
        selectedAt = long("selected_at"),
        completedAt = nullableLong("completed_at"),
        savingsAtFinish = nullableInt("savings_at_finish"),
    )

    private fun Cursor.toSummary() = PeriodSummaryEntity(
        periodId = string("period_id"),
        periodNumber = int("period_number"),
        incomeTotal = int("income_total"),
        requiredActual = int("required_actual"),
        optionalActual = int("optional_actual"),
        savingsDeposited = int("savings_deposited"),
        savingsWithdrawn = int("savings_withdrawn"),
        closingAvailable = int("closing_available"),
        closingSavings = int("closing_savings"),
        essentialsScore = int("essentials_score"),
        planScore = int("plan_score"),
        savingsScore = int("savings_score"),
        growthAwarded = int("growth_awarded"),
        feedback = string("feedback"),
        createdAt = long("created_at"),
    )

    private fun <T> Cursor.collect(mapper: (Cursor) -> T): List<T> = buildList {
        while (moveToNext()) add(mapper(this@collect))
    }

    private fun Cursor.index(name: String) = getColumnIndexOrThrow(name)
    private fun Cursor.string(name: String) = getString(index(name))
    private fun Cursor.nullableString(name: String) = index(name).let { if (isNull(it)) null else getString(it) }
    private fun Cursor.int(name: String) = getInt(index(name))
    private fun Cursor.nullableInt(name: String) = index(name).let { if (isNull(it)) null else getInt(it) }
    private fun Cursor.long(name: String) = getLong(index(name))
    private fun Cursor.nullableLong(name: String) = index(name).let { if (isNull(it)) null else getLong(it) }
}
