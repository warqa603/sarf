package com.cash.guide.feature.savings

import com.cash.guide.data.db.FinancialProfileEntity

// ══════════════════════════════════════════════════════════════════════════════
// Domain tags (Facts) — produced from questionnaire answers
// ══════════════════════════════════════════════════════════════════════════════

object DiagnosticTag {
    const val INCOME_STABLE = "INCOME_STABLE"
    const val INCOME_VARIABLE = "INCOME_VARIABLE"
    const val INCOME_HIGH_VARIANCE = "INCOME_HIGH_VARIANCE"
    const val INCOME_SEASONAL = "INCOME_SEASONAL"
    const val CASHFLOW_HEALTHY = "CASHFLOW_HEALTHY"
    const val CASHFLOW_TIGHT = "CASHFLOW_TIGHT"
    const val CASHFLOW_NEGATIVE = "CASHFLOW_NEGATIVE"
    const val DEBT_NONE = "DEBT_NONE"
    const val DEBT_PRESENT = "DEBT_PRESENT"
    const val DEBT_STRESS = "DEBT_STRESS"
    const val PAYMENT_DELAY = "PAYMENT_DELAY"
    const val MONTH_END_BORROWING = "MONTH_END_BORROWING"
    const val NO_EMERGENCY_FUND = "NO_EMERGENCY_FUND"
    const val LOW_EMERGENCY_BUFFER = "LOW_EMERGENCY_BUFFER"
    const val EMERGENCY_READY = "EMERGENCY_READY"
    const val GOAL_AT_RISK_FROM_EMERGENCY = "GOAL_AT_RISK_FROM_EMERGENCY"
    const val SPENDING_VISIBILITY_LOW = "SPENDING_VISIBILITY_LOW"
    const val TRACKING_GOOD = "TRACKING_GOOD"
    const val COFFEE_HIGH = "COFFEE_HIGH"
    const val FOOD_OUT_HIGH = "FOOD_OUT_HIGH"
    const val SHOPPING_HIGH = "SHOPPING_HIGH"
    const val IMPULSE_BUYING = "IMPULSE_BUYING"
    const val DISCOUNT_TRIGGER = "DISCOUNT_TRIGGER"
    const val EMOTIONAL_SPENDING = "EMOTIONAL_SPENDING"
    const val SUBSCRIPTIONS_UNUSED = "SUBSCRIPTIONS_UNUSED"
    const val IRREGULAR_EXPENSE_UNFUNDED = "IRREGULAR_EXPENSE_UNFUNDED"
    const val SAVE_FIRST = "SAVE_FIRST"
    const val SAVE_WHATS_LEFT = "SAVE_WHATS_LEFT"
    const val GOAL_COMFORTABLE = "GOAL_COMFORTABLE"
    const val GOAL_FEASIBLE = "GOAL_FEASIBLE"
    const val GOAL_TIGHT = "GOAL_TIGHT"
    const val GOAL_AGGRESSIVE = "GOAL_AGGRESSIVE"
    const val GOAL_UNSAFE = "GOAL_UNSAFE"
    const val GOAL_FIXED_DEADLINE = "GOAL_FIXED_DEADLINE"
    const val GOAL_FLEXIBLE_DEADLINE = "GOAL_FLEXIBLE_DEADLINE"
    const val DEPENDENTS_NONE = "DEPENDENTS_NONE"
    const val DEPENDENTS_LOW = "DEPENDENTS_LOW"
    const val DEPENDENTS_HIGH = "DEPENDENTS_HIGH"
    const val USER_PROTECTED_PREFERENCE = "USER_PROTECTED_PREFERENCE"
    const val GOAL_SOLO = "GOAL_SOLO"
    const val GOAL_SHARED = "GOAL_SHARED"
    const val GAMBLING_SPEND = "GAMBLING_SPEND"
    const val LOSS_CHASING_RISK = "LOSS_CHASING_RISK"
    const val VARIABLE_INCOME_BASE = "VARIABLE_INCOME_BASE"
}

// ══════════════════════════════════════════════════════════════════════════════
// Goal feasibility tier
// ══════════════════════════════════════════════════════════════════════════════

enum class GoalFeasibility {
    COMFORTABLE,  // capacity >= required * 1.15
    FEASIBLE,     // capacity ~= required (within 15%)
    TIGHT,        // required slightly > capacity
    AGGRESSIVE,   // large gap
    UNSAFE_NOW;   // negative cash flow / debt stress / month-end borrowing
}

// ══════════════════════════════════════════════════════════════════════════════
// Computed domain metrics
// ══════════════════════════════════════════════════════════════════════════════

data class DiagnosticMetrics(
    val monthlyIncomeCentimes: Long,
    val protectedEssentialsCentimes: Long,
    val debtPaymentsCentimes: Long,
    val familyCommitmentsCentimes: Long,
    val flexibleSpendingCentimes: Long,
    val irregularMonthlyReserveCentimes: Long,
    val freeCashFlowCentimes: Long,
    val realisticCapacityCentimes: Long,   // min(comfort, freeCashFlow)
    val requiredMonthlyCentimes: Long,
    val feasibilityGapCentimes: Long,      // realisticCapacity - required
    val emergencyFundCentimes: Long,
    val emergencyMonths: Double,
    val debtServiceRate: Double,           // debtPayments / income
    val leakRecoverableMonthlyCentimes: Long,
    val feasibility: GoalFeasibility,
    /** Key figures as formatted DH strings */
    val incomeStr: String = "",
    val essentialsStr: String = "",
    val debtsStr: String = "",
    val flexibleStr: String = "",
    val marginStr: String = "",
    val requiredStr: String = ""
)

// ══════════════════════════════════════════════════════════════════════════════
// Leak insight (per selected leak category with full numbers)
// ══════════════════════════════════════════════════════════════════════════════

data class LeakInsight(
    val key: String,
    val titleAr: String,
    val titleFr: String,
    val costPerUseDh: Double,
    val timesPerWeek: Double,
    val monthlyDrainDh: Double,
    val annualDrainDh: Double,
    val saving25pctAnnualDh: Double,
    val saving50pctAnnualDh: Double,
    /** NONE | SOME | HALF | ALL */
    val willingness: String = "HALF"
) {
    val monthlyStr: String get() = com.cash.guide.domain.JournalLedgerManager.formatFrenchNumber(monthlyDrainDh.toLong().toString())
    val monthStr: String get() = monthlyStr
    val annualStr: String get() = com.cash.guide.domain.JournalLedgerManager.formatFrenchNumber(annualDrainDh.toLong().toString())
    val save50Str: String get() = com.cash.guide.domain.JournalLedgerManager.formatFrenchNumber(saving50pctAnnualDh.toLong().toString())
    val save25Str: String get() = com.cash.guide.domain.JournalLedgerManager.formatFrenchNumber(saving25pctAnnualDh.toLong().toString())
}

// ══════════════════════════════════════════════════════════════════════════════
// Savings plan option (A=comfortable, B=balanced, C=aggressive)
// ══════════════════════════════════════════════════════════════════════════════

data class SavingsPlanOption(
    val label: String,          // "A", "B", "C"
    val monthlyCentimes: Long,
    val months: Int,
    val descAr: String,
    val descFr: String,
    val requiresLeakReductionCentimes: Long = 0L
) {
    val monthlyStr: String get() = com.cash.guide.domain.JournalLedgerManager.formatFrenchNumber((monthlyCentimes / 100).toString())
}

// ══════════════════════════════════════════════════════════════════════════════
// Immediate action step
// ══════════════════════════════════════════════════════════════════════════════

data class ActionStep(
    val id: String,
    val emojiAr: String = "",
    val emojiEn: String = "",
    val textAr: String,
    val textFr: String,
    val category: String   // LEAK | EMERGENCY | GOAL | HABIT | DEBT
)

// ══════════════════════════════════════════════════════════════════════════════
// Diagnostic Actionable Advice Card (Real-life, tailored financial prescription)
// ══════════════════════════════════════════════════════════════════════════════

data class DiagnosticAdviceCard(
    val id: String,
    val priorityLevel: Int, // 1 = Critical / Safety, 2 = Leak recovery, 3 = Habits & Growth
    val iconEmoji: String,
    val titleAr: String,
    val titleFr: String,
    val detailedAdviceAr: String,
    val detailedAdviceFr: String,
    val concreteImpactAr: String = "",
    val concreteImpactFr: String = "",
    val categoryTag: String // DEBT | EMERGENCY | LEAK | TIMING | SEASONAL | BEHAVIOR | PACING
)

// ══════════════════════════════════════════════════════════════════════════════
// Diagnostic warning
// ══════════════════════════════════════════════════════════════════════════════

data class DiagnosticWarning(
    val tag: String,
    val messageAr: String,
    val messageFr: String,
    val isCritical: Boolean = false
)

// ══════════════════════════════════════════════════════════════════════════════
// Full diagnostic result (output of the engine)
// ══════════════════════════════════════════════════════════════════════════════

data class FullDiagnosticResult(
    val profileLabelAr: String,
    val profileLabelFr: String,
    val summaryAr: String,
    val summaryFr: String,
    val metrics: DiagnosticMetrics,
    val tags: Set<String>,
    val warnings: List<DiagnosticWarning>,
    val topLeaks: List<LeakInsight>,          // top 1-3 by monthly drain
    val protectedAreasAr: List<String>,
    val protectedAreasFr: List<String>,
    val planOptions: List<SavingsPlanOption>,  // Plans A, B, (C)
    val topActions: List<ActionStep>,          // max 3
    val adviceCards: List<DiagnosticAdviceCard> = emptyList(), // Tailored real-world advice cards
    val recommendedContentIds: List<String>,   // ordered article IDs
    // legacy compatibility: keep PlanDiagnosis fields for Simulator
    val austerityStepsAr: List<String>,
    val austerityStepsFr: List<String>
)

// ══════════════════════════════════════════════════════════════════════════════
// Simulator input state (pure in-memory, no DB)
// ══════════════════════════════════════════════════════════════════════════════

data class SimulatorState(
    val mode: SimulatorMode = SimulatorMode.LEAK_BREAKDOWN,
    val leakReductionPct: Int = 50,           // 0, 25, 50, 75
    val deadlineExtensionMonths: Int = 0,     // 0–12
    val extraMonthlyContributionCentimes: Long = 0L, // 0, 10000, 20000, 50000
    val incomeShockPct: Int = 0,              // 0, -10, -20
    val annualExpenseAmount: Double = 0.0,
    val annualExpenseMonthsUntilDue: Int = 6,
    val emergencyTargetMonths: Int = 3
)

enum class SimulatorMode {
    LEAK_BREAKDOWN,
    REDUCE_LEAKS,
    EXTEND_DEADLINE,
    INCREASE_SAVINGS,
    TOUGH_MONTH,
    ANNUAL_EXPENSE,
    EMERGENCY_FUND
}

// ══════════════════════════════════════════════════════════════════════════════
// Questionnaire state machine
// ══════════════════════════════════════════════════════════════════════════════

/** All sections in order. Branching logic is computed in ViewModel. */
enum class QuestionnaireSection {
    GOAL_SETUP,
    A_PERSONAL,
    B_INCOME,
    C_ESSENTIALS,
    D_DEBTS,
    E_EMERGENCY,
    F_CASHFLOW,
    G_LEAKS,
    H_SEASONAL,
    I_SAVINGS_BEHAVIOR,
    J_BUYING_BEHAVIOR,
    K_USER_LIMITS,
    L_GOAL_FLEXIBILITY,
    REVIEW_PROFILE
}

/** The mutable answers the user is filling in during the questionnaire. */
data class QuestionnaireAnswers(
    val section: QuestionnaireSection = QuestionnaireSection.GOAL_SETUP,
    val stepWithinSection: Int = 0,

    // Step 0 — Goal Definition
    val goalPreset: String = "CUSTOM",
    val goalTitle: String = "",
    val goalTargetCentimes: Long = 1000000L, // 10 000 DH default
    val goalInitialCentimes: Long = 0L,
    val goalTargetMonths: Int = 6,
    val goalColorTag: String = "BLUE",

    // Section A
    val goalOwnership: String = "GOAL_SOLO",
    val dependentsCount: Int = 0,
    val hasFamilyCommitments: Boolean = false,
    val familyCommitmentCentimes: Long = 0L,

    // Section B
    val netMonthlyIncomeCentimes: Long = 0L,
    val incomeType: String = "INCOME_STABLE",
    val incomeLowestCentimes: Long = 0L,
    val incomeHighestCentimes: Long = 0L,
    val additionalIncomeSources: List<String> = emptyList(),
    val additionalIncomeRegularity: String = "",

    // Section C
    val housingCentimes: Long = 0L,
    val utilitiesCentimes: Long = 0L,
    val internetPhoneCentimes: Long = 0L,
    val groceriesCentimes: Long = 0L,
    val workTransportCentimes: Long = 0L,
    val workTransportType: String = "TRANSIT",
    val healthCentimes: Long = 0L,
    val educationCentimes: Long = 0L,
    val insuranceCentimes: Long = 0L,
    val otherEssentialsCentimes: Long = 0L,

    // Section D
    val debtType: String = "DEBT_NONE",
    val debtPaymentsCentimes: Long = 0L,
    val paymentDelayFrequency: Int = 0,
    val endOfMonthBorrowFrequency: Int = 0,

    // Section E
    val emergencyFundCentimes: Long = 0L,
    val emergencyResponse: String = "UNKNOWN",
    val emergencyFundLocation: String = "NONE",

    // Section F
    val monthEndSituation: String = "END_LITTLE",
    val spendingAwareness: String = "APPROXIMATELY",
    val spendingTrackingHabit: String = "SOMETIMES",

    // Section G
    val selectedLeaks: List<String> = emptyList(),
    // map: category → {costPerUse, weeklyFreq, willingness}
    val leakDetails: Map<String, LeakAnswerDetail> = emptyMap(),
    val currentLeakBeingAnswered: String = "",

    // Section H
    val seasonalExpenses: List<SeasonalExpenseAnswer> = emptyList(),

    // Section I
    val savingTiming: String = "SAVE_END",
    val hasStandingTransfer: Boolean = false,
    val bonusHandling: String = "DEPENDS",
    val comfortSavingCentimes: Long = 0L,
    val minimumSavingCentimes: Long = 0L,

    // Section J
    val purchaseDecisionStyle: String = "WAIT_A_BIT",
    val discountTriggerBuying: Int = 1,
    val usesShoppingList: String = "SOMETIMES",
    val paymentMethodThatMakesSpendMore: String = "CARD",

    // Section K
    val userProtectedPreferences: List<String> = emptyList(),

    // Section L
    val goalImportance: String = "IMPORTANT",
    val deadlineFlexibility: String = "FLEXIBLE_3M",
    val willingToIncreaseIncome: Boolean = false,
    val willingToCutFlexible: String = "A_LITTLE"
)

data class LeakAnswerDetail(
    val costPerUseDh: Double = 25.0,
    val weeklyFrequency: Double = 5.0,
    /** NONE | SOME | HALF | ALL */
    val willingness: String = "HALF"
)

data class SeasonalExpenseAnswer(
    val label: String,
    val annualCentimes: Long,
    val monthDue: Int  // 1-12
)

// ══════════════════════════════════════════════════════════════════════════════
// Monthly check-in answers
// ══════════════════════════════════════════════════════════════════════════════

data class MonthlyCheckInAnswers(
    val actualSavedCentimes: Long = 0L,
    val biggestObstacle: String = "",
    val incomeOrObligationsChanged: Boolean = false,
    val goalStillSame: Boolean = true
)
