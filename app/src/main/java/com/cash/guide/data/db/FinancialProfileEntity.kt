package com.cash.guide.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Stores the full Financial Interview (questionnaire) answers for a given savings goal.
 * One entity per goal. JSON strings are used for multi-select answers.
 *
 * All money fields are in centimes (Long). 0 = not answered / unknown.
 * Boolean-ish fields use Int: 0 = no/unknown, 1 = yes.
 */
@Entity(
    tableName = "financial_profiles",
    indices = [
        Index(value = ["goalId"], unique = true),
        Index(value = ["createdAtEpochMs"])
    ]
)
data class FinancialProfileEntity(
    @PrimaryKey
    val id: String,
    val goalId: String,

    // ── Section A: Personal / Family ──────────────────────────────
    /** GOAL_SOLO | GOAL_SHARED */
    val goalOwnership: String = "GOAL_SOLO",
    /** Number of dependents: 0 = none, 1 = one, 2 = two, 3 = three+ */
    val dependentsCount: Int = 0,
    /** Fixed family obligations per month in centimes */
    val familyCommitmentCentimes: Long = 0L,

    // ── Section B: Income ─────────────────────────────────────────
    /** Exact net monthly income in centimes. 0 = user chose bracket instead. */
    val netMonthlyIncomeCentimes: Long = 0L,
    /** INCOME_STABLE | INCOME_VARIABLE | INCOME_HIGH_VARIANCE | INCOME_SEASONAL */
    val incomeType: String = "INCOME_STABLE",
    /** Lowest monthly income in centimes (for variable income) */
    val incomeLowestCentimes: Long = 0L,
    /** Highest monthly income in centimes (for variable income) */
    val incomeHighestCentimes: Long = 0L,
    /** Additional income types JSON array e.g. ["FREELANCE","RENTAL"] */
    val additionalIncomeSources: String = "[]",
    /** REGULAR | OCCASIONAL | SEASONAL */
    val additionalIncomeRegularity: String = "",

    // ── Section C: Protected Essentials ──────────────────────────
    /** Housing rent or mortgage in centimes */
    val housingCentimes: Long = 0L,
    /** Utilities (water + electricity) in centimes */
    val utilitiesCentimes: Long = 0L,
    /** Internet + phone in centimes */
    val internetPhoneCentimes: Long = 0L,
    /** Home groceries in centimes */
    val groceriesCentimes: Long = 0L,
    /** Transport to work in centimes */
    val workTransportCentimes: Long = 0L,
    /** TRANSIT | FUEL | TAXI | MIXED */
    val workTransportType: String = "TRANSIT",
    /** Health / pharmacy in centimes */
    val healthCentimes: Long = 0L,
    /** School / children / daycare in centimes */
    val educationCentimes: Long = 0L,
    /** Insurance in centimes */
    val insuranceCentimes: Long = 0L,
    /** Other essential custom in centimes */
    val otherEssentialsCentimes: Long = 0L,

    // ── Section D: Debts ─────────────────────────────────────────
    /** DEBT_NONE | DEBT_MORTGAGE | DEBT_CAR | DEBT_CONSUMER | DEBT_CARD | DEBT_PERSONAL | DEBT_MULTIPLE */
    val debtType: String = "DEBT_NONE",
    /** Total monthly debt payments in centimes */
    val debtPaymentsCentimes: Long = 0L,
    /** 0=never 1=rarely 2=sometimes 3=often */
    val paymentDelayFrequency: Int = 0,
    /** 0=never 1=rarely 2=some months 3=often */
    val endOfMonthBorrowFrequency: Int = 0,

    // ── Section E: Emergency Fund ─────────────────────────────────
    /** Emergency fund in centimes */
    val emergencyFundCentimes: Long = 0L,
    /** READY_CASH | FROM_SAVINGS | CREDIT | BORROW | UNKNOWN */
    val emergencyResponse: String = "UNKNOWN",
    /** SAME_ACCOUNT | SEPARATE | CASH | NONE */
    val emergencyFundLocation: String = "NONE",

    // ── Section F: Cash Flow Feeling ──────────────────────────────
    /** END_SURPLUS | END_LITTLE | END_ZERO | END_DEFICIT | END_BORROW */
    val monthEndSituation: String = "END_LITTLE",
    /** KNOWS_WELL | APPROXIMATELY | NO */
    val spendingAwareness: String = "APPROXIMATELY",
    /** ALL | MAJOR_ONLY | SOMETIMES | NEVER */
    val spendingTrackingHabit: String = "SOMETIMES",

    // ── Section G: Flexible Leaks (multi-select + detail) ─────────
    /** JSON array of selected leak categories e.g. ["CAFE","SHOPPING","SUBSCRIPTIONS"] */
    val selectedLeaks: String = "[]",
    /** JSON object: category → {costPerUse, weeklyFreq, willingness} */
    val leakDetails: String = "{}",

    // ── Section H: Seasonal / Annual Expenses ────────────────────
    /** JSON array of {label, annualCentimes, monthDue} objects */
    val seasonalExpenses: String = "[]",

    // ── Section I: Savings Behavior ───────────────────────────────
    /** SAVE_FIRST | SAVE_MID | SAVE_END | SAVE_IRREGULAR | SAVE_NEVER */
    val savingTiming: String = "SAVE_END",
    /** 1 = yes, 0 = no */
    val hasStandingTransfer: Int = 0,
    /** SAVE_SOME | SPEND_MOST | COVER_OBLIGATIONS | DEPENDS */
    val bonusHandling: String = "DEPENDS",
    /** Comfortable monthly saving amount in centimes */
    val comfortSavingCentimes: Long = 0L,
    /** Minimum saving amount even in hard months in centimes */
    val minimumSavingCentimes: Long = 0L,

    // ── Section J: Buying Behavior ────────────────────────────────
    /** QUICK | WAIT_A_BIT | COMPARE */
    val purchaseDecisionStyle: String = "WAIT_A_BIT",
    /** OFTEN | SOMETIMES | RARELY */
    val discountTriggerBuying: Int = 1,
    /** OFTEN | SOMETIMES | NEVER */
    val usesShoppingList: String = "SOMETIMES",
    /** CASH | CARD | WALLET | NO_DIFF */
    val paymentMethodThatMakesSpendMore: String = "CARD",

    // ── Section K: User-Protected Preferences ─────────────────────
    /** JSON array of things the user does NOT want the plan to cut */
    val userProtectedPreferences: String = "[]",

    // ── Section L: Goal Flexibility ──────────────────────────────
    /** ESSENTIAL | IMPORTANT | NICE_TO_HAVE */
    val goalImportance: String = "IMPORTANT",
    /** FIXED | FLEXIBLE_3M | FLEXIBLE_6M | VERY_FLEXIBLE */
    val deadlineFlexibility: String = "FLEXIBLE_3M",
    /** 1 = willing, 0 = no */
    val willingToIncreaseIncome: Int = 0,
    /** NONE | A_LITTLE | MODERATE | A_LOT_SHORT_TERM */
    val willingToCutFlexible: String = "A_LITTLE",

    // ── Meta ──────────────────────────────────────────────────────
    /** Computed tags JSON array */
    val computedTags: String = "[]",
    /** Schema version for future migration */
    val answersVersion: Int = 1,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)
