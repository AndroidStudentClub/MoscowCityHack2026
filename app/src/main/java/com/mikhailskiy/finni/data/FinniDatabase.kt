package com.mikhailskiy.finni.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FinniDatabase private constructor(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION,
) {
    val dao: FinniDao by lazy { FinniDao(this) }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE profiles (
                profile_id TEXT PRIMARY KEY NOT NULL,
                nickname TEXT NOT NULL,
                avatar_id TEXT NOT NULL,
                is_demo INTEGER NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE pets (
                pet_id TEXT PRIMARY KEY NOT NULL,
                profile_id TEXT NOT NULL UNIQUE,
                name TEXT NOT NULL,
                species_id TEXT NOT NULL,
                color_id TEXT NOT NULL,
                accessory_id TEXT,
                has_shoes INTEGER NOT NULL DEFAULT 0,
                has_glasses INTEGER NOT NULL DEFAULT 0,
                has_jetpack INTEGER NOT NULL DEFAULT 0,
                stage TEXT NOT NULL,
                growth_points INTEGER NOT NULL,
                satiety INTEGER NOT NULL,
                mood INTEGER NOT NULL,
                comfort INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                FOREIGN KEY(profile_id) REFERENCES profiles(profile_id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE game_periods (
                period_id TEXT PRIMARY KEY NOT NULL,
                profile_id TEXT NOT NULL,
                period_number INTEGER NOT NULL,
                template_id TEXT NOT NULL,
                content_version TEXT NOT NULL,
                status TEXT NOT NULL,
                opening_available INTEGER NOT NULL,
                opening_savings INTEGER NOT NULL,
                started_at INTEGER NOT NULL,
                completed_at INTEGER,
                FOREIGN KEY(profile_id) REFERENCES profiles(profile_id) ON DELETE CASCADE,
                UNIQUE(profile_id, period_number)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX idx_period_profile_status ON game_periods(profile_id, status)")
        db.execSQL(
            """
            CREATE TABLE budget_plans (
                period_id TEXT PRIMARY KEY NOT NULL,
                base_available INTEGER NOT NULL,
                required_planned INTEGER NOT NULL,
                optional_planned INTEGER NOT NULL,
                savings_planned INTEGER NOT NULL,
                buffer_planned INTEGER NOT NULL,
                status TEXT NOT NULL,
                confirmed_at INTEGER,
                updated_at INTEGER NOT NULL,
                FOREIGN KEY(period_id) REFERENCES game_periods(period_id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE ledger_entries (
                row_id INTEGER PRIMARY KEY AUTOINCREMENT,
                entry_id TEXT NOT NULL UNIQUE,
                operation_key TEXT NOT NULL UNIQUE,
                profile_id TEXT NOT NULL,
                period_id TEXT,
                operation_type TEXT NOT NULL,
                budget_category TEXT,
                available_delta INTEGER NOT NULL,
                savings_delta INTEGER NOT NULL,
                available_after INTEGER NOT NULL CHECK(available_after >= 0),
                savings_after INTEGER NOT NULL CHECK(savings_after >= 0),
                source_id TEXT,
                explanation TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                FOREIGN KEY(profile_id) REFERENCES profiles(profile_id) ON DELETE CASCADE,
                FOREIGN KEY(period_id) REFERENCES game_periods(period_id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX idx_ledger_profile_row ON ledger_entries(profile_id, row_id)")
        db.execSQL("CREATE INDEX idx_ledger_period_type ON ledger_entries(period_id, operation_type)")
        db.execSQL(
            """
            CREATE TABLE purchases (
                purchase_id TEXT PRIMARY KEY NOT NULL,
                profile_id TEXT NOT NULL,
                period_id TEXT NOT NULL,
                ledger_row_id INTEGER NOT NULL UNIQUE,
                catalog_item_id TEXT NOT NULL,
                title_snapshot TEXT NOT NULL,
                category_snapshot TEXT NOT NULL,
                price_snapshot INTEGER NOT NULL,
                satiety_delta INTEGER NOT NULL,
                mood_delta INTEGER NOT NULL,
                comfort_delta INTEGER NOT NULL,
                purchased_at INTEGER NOT NULL,
                FOREIGN KEY(profile_id) REFERENCES profiles(profile_id) ON DELETE CASCADE,
                FOREIGN KEY(period_id) REFERENCES game_periods(period_id) ON DELETE CASCADE,
                FOREIGN KEY(ledger_row_id) REFERENCES ledger_entries(row_id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX idx_purchase_period ON purchases(period_id)")
        db.execSQL(
            """
            CREATE TABLE goal_progress (
                goal_progress_id TEXT PRIMARY KEY NOT NULL,
                profile_id TEXT NOT NULL,
                goal_id TEXT NOT NULL,
                title_snapshot TEXT NOT NULL,
                target_snapshot INTEGER NOT NULL,
                status TEXT NOT NULL,
                selected_at INTEGER NOT NULL,
                completed_at INTEGER,
                savings_at_finish INTEGER,
                FOREIGN KEY(profile_id) REFERENCES profiles(profile_id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX idx_goal_profile_status ON goal_progress(profile_id, status)")
        db.execSQL(
            """
            CREATE TABLE task_attempts (
                attempt_id TEXT PRIMARY KEY NOT NULL,
                profile_id TEXT NOT NULL,
                period_id TEXT,
                task_id TEXT NOT NULL,
                content_version TEXT NOT NULL,
                result_code TEXT NOT NULL,
                actions_snapshot TEXT NOT NULL,
                reward INTEGER NOT NULL,
                reward_ledger_row_id INTEGER UNIQUE,
                started_at INTEGER NOT NULL,
                completed_at INTEGER NOT NULL,
                FOREIGN KEY(profile_id) REFERENCES profiles(profile_id) ON DELETE CASCADE,
                FOREIGN KEY(period_id) REFERENCES game_periods(period_id) ON DELETE CASCADE,
                FOREIGN KEY(reward_ledger_row_id) REFERENCES ledger_entries(row_id) ON DELETE SET NULL,
                UNIQUE(profile_id, task_id)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX idx_task_profile ON task_attempts(profile_id)")
        db.execSQL(
            """
            CREATE TABLE pet_state_events (
                event_id TEXT PRIMARY KEY NOT NULL,
                pet_id TEXT NOT NULL,
                period_id TEXT,
                reason_type TEXT NOT NULL,
                reason_id TEXT,
                satiety_delta INTEGER NOT NULL,
                mood_delta INTEGER NOT NULL,
                comfort_delta INTEGER NOT NULL,
                growth_delta INTEGER NOT NULL,
                satiety_after INTEGER NOT NULL,
                mood_after INTEGER NOT NULL,
                comfort_after INTEGER NOT NULL,
                growth_after INTEGER NOT NULL,
                message TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                FOREIGN KEY(pet_id) REFERENCES pets(pet_id) ON DELETE CASCADE,
                FOREIGN KEY(period_id) REFERENCES game_periods(period_id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX idx_pet_event_pet ON pet_state_events(pet_id)")
        db.execSQL(
            """
            CREATE TABLE period_summaries (
                period_id TEXT PRIMARY KEY NOT NULL,
                period_number INTEGER NOT NULL,
                income_total INTEGER NOT NULL,
                required_actual INTEGER NOT NULL,
                optional_actual INTEGER NOT NULL,
                savings_deposited INTEGER NOT NULL,
                savings_withdrawn INTEGER NOT NULL,
                closing_available INTEGER NOT NULL,
                closing_savings INTEGER NOT NULL,
                essentials_score INTEGER NOT NULL,
                plan_score INTEGER NOT NULL,
                savings_score INTEGER NOT NULL,
                growth_awarded INTEGER NOT NULL,
                feedback TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                FOREIGN KEY(period_id) REFERENCES game_periods(period_id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        createStepTrackingTable(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) createStepTrackingTable(db)
        if (oldVersion == 2) migrateStepTrackingForVariableThreshold(db)
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE pets ADD COLUMN has_shoes INTEGER NOT NULL DEFAULT 0")
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE pets ADD COLUMN has_glasses INTEGER NOT NULL DEFAULT 0")
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE pets ADD COLUMN has_jetpack INTEGER NOT NULL DEFAULT 0")
        }
        if (newVersion > DATABASE_VERSION) {
            throw IllegalStateException("Migration from $oldVersion to $newVersion is not implemented")
        }
    }

    private fun createStepTrackingTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS step_tracking (
                profile_id TEXT PRIMARY KEY NOT NULL,
                last_sensor_total INTEGER,
                tracked_steps INTEGER NOT NULL CHECK(tracked_steps >= 0),
                pending_steps INTEGER NOT NULL CHECK(pending_steps >= 0),
                updated_at INTEGER NOT NULL,
                FOREIGN KEY(profile_id) REFERENCES profiles(profile_id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
    }

    private fun migrateStepTrackingForVariableThreshold(db: SQLiteDatabase) {
        db.execSQL("ALTER TABLE step_tracking RENAME TO step_tracking_fixed_threshold")
        createStepTrackingTable(db)
        db.execSQL(
            """
            INSERT INTO step_tracking (
                profile_id, last_sensor_total, tracked_steps, pending_steps, updated_at
            )
            SELECT profile_id, last_sensor_total, tracked_steps, pending_steps, updated_at
            FROM step_tracking_fixed_threshold
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE step_tracking_fixed_threshold")
    }

    suspend fun <T> read(block: () -> T): T = withContext(Dispatchers.IO) { block() }

    suspend fun <T> transaction(block: () -> T): T = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val result = block()
            db.setTransactionSuccessful()
            result
        } finally {
            db.endTransaction()
        }
    }

    companion object {
        private const val DATABASE_NAME = "finni.db"
        private const val DATABASE_VERSION = 5

        @Volatile private var instance: FinniDatabase? = null

        fun getInstance(context: Context): FinniDatabase = instance ?: synchronized(this) {
            instance ?: FinniDatabase(context).also { instance = it }
        }
    }
}
