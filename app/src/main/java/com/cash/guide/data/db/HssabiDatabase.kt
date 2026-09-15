package com.cash.guide.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CalculationEntity::class,
        CalculationItemEntity::class,
        CalculationGroupEntity::class,
        ChecklistEntity::class,
        ChecklistItemEntity::class,
        NoteEntity::class,
        ReminderEntity::class,
        SavingsGoalEntity::class,
        SavingsDepositEntity::class
    ],
    version = 12,
    exportSchema = true
)
abstract class HssabiDatabase : RoomDatabase() {

    abstract fun calculationDao(): CalculationDao
    abstract fun calculationGroupDao(): CalculationGroupDao
    abstract fun checklistDao(): ChecklistDao
    abstract fun noteDao(): NoteDao
    abstract fun reminderDao(): ReminderDao
    abstract fun savingsDao(): SavingsDao


    companion object {
        @Volatile
        private var INSTANCE: HssabiDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `calculation_groups` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `colorHex` TEXT NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("ALTER TABLE `calculations` ADD COLUMN `groupId` TEXT DEFAULT NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_calculations_groupId` ON `calculations` (`groupId`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `calculations` ADD COLUMN `paymentStatus` TEXT NOT NULL DEFAULT 'PAID'")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_calculations_paymentStatus` ON `calculations` (`paymentStatus`)")
                db.execSQL("UPDATE `calculations` SET `paymentStatus` = 'UNPAID' WHERE `title` LIKE '%Chantier%' OR `title` LIKE '%Salaires%' OR `title` LIKE '%Tissus%'")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `calculations` ADD COLUMN `calcType` TEXT NOT NULL DEFAULT 'PERSONNEL'")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_calculations_calcType` ON `calculations` (`calcType`)")
                db.execSQL("UPDATE `calculations` SET `calcType` = 'CREDIT' WHERE `paymentStatus` = 'UNPAID' OR `title` LIKE '%Chantier%' OR `title` LIKE '%Salaires%' OR `title` LIKE '%Tissus%'")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `calculations` ADD COLUMN `dueDateEpochMs` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `calculations` ADD COLUMN `reminderEnabled` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `calculations` ADD COLUMN `reminderTimeEpochMs` INTEGER DEFAULT NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_calculations_dueDateEpochMs` ON `calculations` (`dueDateEpochMs`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `checklists` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_checklists_updatedAtEpochMs` ON `checklists` (`updatedAtEpochMs`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `checklist_items` (
                        `id` TEXT NOT NULL,
                        `checklistId` TEXT NOT NULL,
                        `text` TEXT NOT NULL,
                        `isChecked` INTEGER NOT NULL DEFAULT 0,
                        `position` INTEGER NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`checklistId`) REFERENCES `checklists`(`id`) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_checklist_items_checklistId` ON `checklist_items` (`checklistId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_checklist_items_checklistId_position` ON `checklist_items` (`checklistId`, `position`)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `notes` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `colorTag` TEXT NOT NULL DEFAULT 'DEFAULT',
                        `isPinned` INTEGER NOT NULL DEFAULT 0,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_updatedAtEpochMs` ON `notes` (`updatedAtEpochMs`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_isPinned` ON `notes` (`isPinned`)")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `reminders` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL DEFAULT '',
                        `targetEpochMs` INTEGER NOT NULL,
                        `recurrenceType` TEXT NOT NULL DEFAULT 'ONCE',
                        `repeatDays` TEXT NOT NULL DEFAULT '',
                        `timeHour` INTEGER NOT NULL DEFAULT 9,
                        `timeMinute` INTEGER NOT NULL DEFAULT 0,
                        `isEnabled` INTEGER NOT NULL DEFAULT 1,
                        `isCompleted` INTEGER NOT NULL DEFAULT 0,
                        `colorTag` TEXT NOT NULL DEFAULT 'BLUE',
                        `calculationId` TEXT DEFAULT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_targetEpochMs` ON `reminders` (`targetEpochMs`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_isEnabled` ON `reminders` (`isEnabled`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_isCompleted` ON `reminders` (`isCompleted`)")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `calculation_groups` ADD COLUMN `category` TEXT NOT NULL DEFAULT 'CALCULATIONS'")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_calculation_groups_category` ON `calculation_groups` (`category`)")
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `groupId` TEXT DEFAULT NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_groupId` ON `notes` (`groupId`)")
                db.execSQL("ALTER TABLE `checklists` ADD COLUMN `groupId` TEXT DEFAULT NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_checklists_groupId` ON `checklists` (`groupId`)")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `savings_goals` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `targetAmountCentimes` INTEGER NOT NULL,
                        `currentAmountCentimes` INTEGER NOT NULL DEFAULT 0,
                        `monthlyContributionCentimes` INTEGER NOT NULL DEFAULT 0,
                        `targetDateEpochMs` INTEGER DEFAULT NULL,
                        `currency` TEXT NOT NULL DEFAULT 'DIRHAM',
                        `colorTag` TEXT NOT NULL DEFAULT 'BLUE',
                        `icon` TEXT NOT NULL DEFAULT 'STAR',
                        `isCompleted` INTEGER NOT NULL DEFAULT 0,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_savings_goals_isCompleted` ON `savings_goals` (`isCompleted`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_savings_goals_createdAtEpochMs` ON `savings_goals` (`createdAtEpochMs`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `savings_deposits` (
                        `id` TEXT NOT NULL,
                        `goalId` TEXT NOT NULL,
                        `amountCentimes` INTEGER NOT NULL,
                        `note` TEXT NOT NULL DEFAULT '',
                        `dateEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`goalId`) REFERENCES `savings_goals`(`id`) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_savings_deposits_goalId` ON `savings_deposits` (`goalId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_savings_deposits_dateEpochMs` ON `savings_deposits` (`dateEpochMs`)")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `savings_goals` ADD COLUMN `targetMonths` INTEGER NOT NULL DEFAULT 24")
                db.execSQL("ALTER TABLE `savings_goals` ADD COLUMN `monthlySalaryCentimes` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `savings_goals` ADD COLUMN `essentialBracket` TEXT NOT NULL DEFAULT 'MEDIUM'")
                db.execSQL("ALTER TABLE `savings_goals` ADD COLUMN `leisureCategory` TEXT NOT NULL DEFAULT 'CAFE'")
                db.execSQL("ALTER TABLE `savings_goals` ADD COLUMN `savingsStyle` TEXT NOT NULL DEFAULT 'BALANCED'")
                db.execSQL("ALTER TABLE `savings_goals` ADD COLUMN `initialAmountCentimes` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `savings_goals` ADD COLUMN `leakDailyCostCentimes` INTEGER NOT NULL DEFAULT 2500")
                db.execSQL("ALTER TABLE `savings_goals` ADD COLUMN `leakDaysPerWeek` INTEGER NOT NULL DEFAULT 6")
            }
        }

        fun getInstance(context: Context): HssabiDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HssabiDatabase::class.java,
                    "hssabi.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

