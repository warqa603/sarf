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
        SavingsDepositEntity::class,
        FinancialProfileEntity::class
    ],
    version = 13,
    exportSchema = true
)
abstract class HssabiDatabase : RoomDatabase() {

    abstract fun calculationDao(): CalculationDao
    abstract fun calculationGroupDao(): CalculationGroupDao
    abstract fun checklistDao(): ChecklistDao
    abstract fun noteDao(): NoteDao
    abstract fun reminderDao(): ReminderDao
    abstract fun savingsDao(): SavingsDao
    abstract fun financialProfileDao(): FinancialProfileDao



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

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `financial_profiles` (
                        `id` TEXT NOT NULL,
                        `goalId` TEXT NOT NULL,
                        `goalOwnership` TEXT NOT NULL DEFAULT 'GOAL_SOLO',
                        `dependentsCount` INTEGER NOT NULL DEFAULT 0,
                        `familyCommitmentCentimes` INTEGER NOT NULL DEFAULT 0,
                        `netMonthlyIncomeCentimes` INTEGER NOT NULL DEFAULT 0,
                        `incomeType` TEXT NOT NULL DEFAULT 'INCOME_STABLE',
                        `incomeLowestCentimes` INTEGER NOT NULL DEFAULT 0,
                        `incomeHighestCentimes` INTEGER NOT NULL DEFAULT 0,
                        `additionalIncomeSources` TEXT NOT NULL DEFAULT '[]',
                        `additionalIncomeRegularity` TEXT NOT NULL DEFAULT '',
                        `housingCentimes` INTEGER NOT NULL DEFAULT 0,
                        `utilitiesCentimes` INTEGER NOT NULL DEFAULT 0,
                        `internetPhoneCentimes` INTEGER NOT NULL DEFAULT 0,
                        `groceriesCentimes` INTEGER NOT NULL DEFAULT 0,
                        `workTransportCentimes` INTEGER NOT NULL DEFAULT 0,
                        `workTransportType` TEXT NOT NULL DEFAULT 'TRANSIT',
                        `healthCentimes` INTEGER NOT NULL DEFAULT 0,
                        `educationCentimes` INTEGER NOT NULL DEFAULT 0,
                        `insuranceCentimes` INTEGER NOT NULL DEFAULT 0,
                        `otherEssentialsCentimes` INTEGER NOT NULL DEFAULT 0,
                        `debtType` TEXT NOT NULL DEFAULT 'DEBT_NONE',
                        `debtPaymentsCentimes` INTEGER NOT NULL DEFAULT 0,
                        `paymentDelayFrequency` INTEGER NOT NULL DEFAULT 0,
                        `endOfMonthBorrowFrequency` INTEGER NOT NULL DEFAULT 0,
                        `emergencyFundCentimes` INTEGER NOT NULL DEFAULT 0,
                        `emergencyResponse` TEXT NOT NULL DEFAULT 'UNKNOWN',
                        `emergencyFundLocation` TEXT NOT NULL DEFAULT 'NONE',
                        `monthEndSituation` TEXT NOT NULL DEFAULT 'END_LITTLE',
                        `spendingAwareness` TEXT NOT NULL DEFAULT 'APPROXIMATELY',
                        `spendingTrackingHabit` TEXT NOT NULL DEFAULT 'SOMETIMES',
                        `selectedLeaks` TEXT NOT NULL DEFAULT '[]',
                        `leakDetails` TEXT NOT NULL DEFAULT '{}',
                        `seasonalExpenses` TEXT NOT NULL DEFAULT '[]',
                        `savingTiming` TEXT NOT NULL DEFAULT 'SAVE_END',
                        `hasStandingTransfer` INTEGER NOT NULL DEFAULT 0,
                        `bonusHandling` TEXT NOT NULL DEFAULT 'DEPENDS',
                        `comfortSavingCentimes` INTEGER NOT NULL DEFAULT 0,
                        `minimumSavingCentimes` INTEGER NOT NULL DEFAULT 0,
                        `purchaseDecisionStyle` TEXT NOT NULL DEFAULT 'WAIT_A_BIT',
                        `discountTriggerBuying` INTEGER NOT NULL DEFAULT 1,
                        `usesShoppingList` TEXT NOT NULL DEFAULT 'SOMETIMES',
                        `paymentMethodThatMakesSpendMore` TEXT NOT NULL DEFAULT 'CARD',
                        `userProtectedPreferences` TEXT NOT NULL DEFAULT '[]',
                        `goalImportance` TEXT NOT NULL DEFAULT 'IMPORTANT',
                        `deadlineFlexibility` TEXT NOT NULL DEFAULT 'FLEXIBLE_3M',
                        `willingToIncreaseIncome` INTEGER NOT NULL DEFAULT 0,
                        `willingToCutFlexible` TEXT NOT NULL DEFAULT 'A_LITTLE',
                        `computedTags` TEXT NOT NULL DEFAULT '[]',
                        `answersVersion` INTEGER NOT NULL DEFAULT 1,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_financial_profiles_goalId` ON `financial_profiles` (`goalId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_financial_profiles_createdAtEpochMs` ON `financial_profiles` (`createdAtEpochMs`)")
            }
        }

        fun getInstance(context: Context): HssabiDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HssabiDatabase::class.java,
                    "hssabi.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

