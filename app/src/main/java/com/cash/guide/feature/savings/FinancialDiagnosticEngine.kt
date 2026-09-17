package com.cash.guide.feature.savings

import com.cash.guide.data.db.FinancialProfileEntity
import com.cash.guide.data.db.SavingsGoalEntity
import com.cash.guide.domain.JournalLedgerManager
import kotlin.math.abs

/**
 * Pure offline deterministic Financial Diagnostic Engine.
 *
 * Flow: QuestionnaireAnswers → Facts/Tags → Metrics → Profile → Recommendations → DiagnosticResult
 *
 * NO network required. NO AI required. All computation is local math.
 */
object FinancialDiagnosticEngine {

    // ══════════════════════════════════════════════════════════════════════════
    // 1. Build tags from questionnaire answers
    // ══════════════════════════════════════════════════════════════════════════

    fun buildTags(answers: QuestionnaireAnswers, goal: SavingsGoalEntity): Set<String> {
        val tags = mutableSetOf<String>()

        // Ownership
        if (answers.goalOwnership == "GOAL_SHARED") tags += DiagnosticTag.GOAL_SHARED
        else tags += DiagnosticTag.GOAL_SOLO

        // Dependents
        when {
            answers.dependentsCount == 0 -> tags += DiagnosticTag.DEPENDENTS_NONE
            answers.dependentsCount <= 2 -> tags += DiagnosticTag.DEPENDENTS_LOW
            else -> tags += DiagnosticTag.DEPENDENTS_HIGH
        }

        // Income type
        when (answers.incomeType) {
            "INCOME_STABLE" -> tags += DiagnosticTag.INCOME_STABLE
            "INCOME_VARIABLE" -> tags += DiagnosticTag.INCOME_VARIABLE
            "INCOME_HIGH_VARIANCE" -> {
                tags += DiagnosticTag.INCOME_VARIABLE
                tags += DiagnosticTag.INCOME_HIGH_VARIANCE
            }
            "INCOME_SEASONAL" -> {
                tags += DiagnosticTag.INCOME_VARIABLE
                tags += DiagnosticTag.INCOME_SEASONAL
            }
        }

        // Debts
        if (answers.debtType == "DEBT_NONE") {
            tags += DiagnosticTag.DEBT_NONE
        } else {
            tags += DiagnosticTag.DEBT_PRESENT
            val income = effectiveIncomeCentimes(answers)
            if (income > 0 && answers.debtPaymentsCentimes.toDouble() / income > 0.40) {
                tags += DiagnosticTag.DEBT_STRESS
            }
        }
        if (answers.paymentDelayFrequency >= 2) tags += DiagnosticTag.PAYMENT_DELAY
        if (answers.endOfMonthBorrowFrequency >= 2) tags += DiagnosticTag.MONTH_END_BORROWING

        // Emergency
        val essentials = protectedEssentialsCentimes(answers)
        val emergencyMonths = if (essentials > 0) answers.emergencyFundCentimes.toDouble() / essentials else 0.0
        when {
            answers.emergencyFundCentimes <= 0 || answers.emergencyFundLocation == "NONE" -> {
                tags += DiagnosticTag.NO_EMERGENCY_FUND
                if (DiagnosticTag.DEBT_NONE !in tags) tags += DiagnosticTag.GOAL_AT_RISK_FROM_EMERGENCY
            }
            emergencyMonths < 1.0 -> tags += DiagnosticTag.LOW_EMERGENCY_BUFFER
            else -> tags += DiagnosticTag.EMERGENCY_READY
        }

        // Cash flow awareness
        when (answers.spendingAwareness) {
            "NO" -> tags += DiagnosticTag.SPENDING_VISIBILITY_LOW
            "KNOWS_WELL" -> tags += DiagnosticTag.TRACKING_GOOD
        }
        if (answers.spendingTrackingHabit == "ALL" || answers.spendingTrackingHabit == "MAJOR_ONLY") {
            tags += DiagnosticTag.TRACKING_GOOD
        }

        // Leaks
        if ("CAFE" in answers.selectedLeaks || "FOOD_OUT" in answers.selectedLeaks) tags += DiagnosticTag.FOOD_OUT_HIGH
        if ("SHOPPING" in answers.selectedLeaks) tags += DiagnosticTag.SHOPPING_HIGH
        if ("SUBSCRIPTIONS" in answers.selectedLeaks) tags += DiagnosticTag.SUBSCRIPTIONS_UNUSED
        if ("CAFE" in answers.selectedLeaks) tags += DiagnosticTag.COFFEE_HIGH

        // Buying behavior
        if (answers.discountTriggerBuying >= 2) tags += DiagnosticTag.DISCOUNT_TRIGGER
        if (answers.purchaseDecisionStyle == "QUICK") tags += DiagnosticTag.IMPULSE_BUYING

        // Savings timing
        when (answers.savingTiming) {
            "SAVE_FIRST" -> tags += DiagnosticTag.SAVE_FIRST
            "SAVE_END", "SAVE_IRREGULAR", "SAVE_NEVER" -> tags += DiagnosticTag.SAVE_WHATS_LEFT
        }

        // Protected preferences
        if (answers.userProtectedPreferences.isNotEmpty()) tags += DiagnosticTag.USER_PROTECTED_PREFERENCE

        // Deadline flexibility
        if (answers.deadlineFlexibility == "FIXED") tags += DiagnosticTag.GOAL_FIXED_DEADLINE
        else tags += DiagnosticTag.GOAL_FLEXIBLE_DEADLINE

        // Seasonal
        val totalSeasonalAnnual = answers.seasonalExpenses.sumOf { it.annualCentimes }
        if (totalSeasonalAnnual > 0) {
            val monthlyReserve = totalSeasonalAnnual / 12
            val freeCashApprox = computeFreeCashFlow(answers).freeCashFlowCentimes
            if (monthlyReserve > freeCashApprox * 0.2) tags += DiagnosticTag.IRREGULAR_EXPENSE_UNFUNDED
        }

        // Cash flow overall
        val fcf = computeFreeCashFlow(answers)
        when {
            fcf.freeCashFlowCentimes < 0 || answers.endOfMonthBorrowFrequency >= 2 || answers.paymentDelayFrequency >= 2 ->
                tags += DiagnosticTag.CASHFLOW_NEGATIVE
            fcf.freeCashFlowCentimes < (goal.targetAmountCentimes / goal.targetMonths.coerceAtLeast(1)) ->
                tags += DiagnosticTag.CASHFLOW_TIGHT
            else ->
                tags += DiagnosticTag.CASHFLOW_HEALTHY
        }

        return tags
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. Compute core financial metrics
    // ══════════════════════════════════════════════════════════════════════════

    data class CashFlowBreakdown(
        val incomeCentimes: Long,
        val essentialsCentimes: Long,
        val debtCentimes: Long,
        val familyCentimes: Long,
        val flexibleCentimes: Long,
        val seasonalMonthlyReserveCentimes: Long,
        val freeCashFlowCentimes: Long
    )

    fun computeFreeCashFlow(answers: QuestionnaireAnswers): CashFlowBreakdown {
        val income = effectiveIncomeCentimes(answers)
        val essentials = protectedEssentialsCentimes(answers)
        val debts = answers.debtPaymentsCentimes
        val family = answers.familyCommitmentCentimes
        val seasonal = answers.seasonalExpenses.sumOf { se ->
            if (se.monthDue > 0) se.annualCentimes / 12 else se.annualCentimes / 12
        }
        val flexible = estimateFlexibleSpending(answers, income, essentials, debts, family)
        val fcf = income - essentials - debts - family - flexible - seasonal
        return CashFlowBreakdown(
            incomeCentimes = income,
            essentialsCentimes = essentials,
            debtCentimes = debts,
            familyCentimes = family,
            flexibleCentimes = flexible,
            seasonalMonthlyReserveCentimes = seasonal,
            freeCashFlowCentimes = fcf
        )
    }

    fun computeMetrics(answers: QuestionnaireAnswers, goal: SavingsGoalEntity, tags: Set<String>): DiagnosticMetrics {
        val fcf = computeFreeCashFlow(answers)
        val income = fcf.incomeCentimes

        val goalGap = (goal.targetAmountCentimes - goal.currentAmountCentimes).coerceAtLeast(0L)
        val months = goal.targetMonths.coerceAtLeast(1)
        val requiredMonthly = goalGap / months

        val comfortSaving = answers.comfortSavingCentimes.takeIf { it > 0 }
            ?: (fcf.freeCashFlowCentimes * 0.8).toLong().coerceAtLeast(0L)

        val realisticCapacity = if (fcf.freeCashFlowCentimes > 0) {
            minOf(comfortSaving, fcf.freeCashFlowCentimes)
        } else 0L

        val feasibilityGap = realisticCapacity - requiredMonthly

        val essentials = fcf.essentialsCentimes
        val emergencyMonths = if (essentials > 0) answers.emergencyFundCentimes.toDouble() / essentials else 0.0
        val debtServiceRate = if (income > 0) answers.debtPaymentsCentimes.toDouble() / income else 0.0

        val feasibility = classifyFeasibility(
            realisticCapacityCentimes = realisticCapacity,
            requiredMonthlyCentimes = requiredMonthly,
            freeCashFlowCentimes = fcf.freeCashFlowCentimes,
            tags = tags
        )

        // Recoverable from leaks (top leaks reduction at 50%)
        val leakRecoverable = topLeakInsights(answers, tags)
            .sumOf { (it.monthlyDrainDh * 0.5 * 100).toLong() }

        val fmt = { c: Long -> JournalLedgerManager.formatFrenchNumber((c / 100).toString()) + " DH" }

        return DiagnosticMetrics(
            monthlyIncomeCentimes = income,
            protectedEssentialsCentimes = essentials,
            debtPaymentsCentimes = fcf.debtCentimes,
            familyCommitmentsCentimes = fcf.familyCentimes,
            flexibleSpendingCentimes = fcf.flexibleCentimes,
            irregularMonthlyReserveCentimes = fcf.seasonalMonthlyReserveCentimes,
            freeCashFlowCentimes = fcf.freeCashFlowCentimes,
            realisticCapacityCentimes = realisticCapacity,
            requiredMonthlyCentimes = requiredMonthly,
            feasibilityGapCentimes = feasibilityGap,
            emergencyFundCentimes = answers.emergencyFundCentimes,
            emergencyMonths = emergencyMonths,
            debtServiceRate = debtServiceRate,
            leakRecoverableMonthlyCentimes = leakRecoverable,
            feasibility = feasibility,
            incomeStr = fmt(income),
            essentialsStr = fmt(essentials),
            debtsStr = fmt(fcf.debtCentimes + fcf.familyCentimes),
            flexibleStr = fmt(fcf.flexibleCentimes),
            marginStr = fmt(fcf.freeCashFlowCentimes),
            requiredStr = fmt(requiredMonthly)
        )
    }

    fun classifyFeasibility(
        realisticCapacityCentimes: Long,
        requiredMonthlyCentimes: Long,
        freeCashFlowCentimes: Long,
        tags: Set<String>
    ): GoalFeasibility {
        if (DiagnosticTag.CASHFLOW_NEGATIVE in tags ||
            DiagnosticTag.DEBT_STRESS in tags ||
            DiagnosticTag.MONTH_END_BORROWING in tags ||
            freeCashFlowCentimes < 0
        ) return GoalFeasibility.UNSAFE_NOW

        if (requiredMonthlyCentimes <= 0) return GoalFeasibility.COMFORTABLE

        val ratio = realisticCapacityCentimes.toDouble() / requiredMonthlyCentimes.toDouble()
        return when {
            ratio >= 1.15 -> GoalFeasibility.COMFORTABLE
            ratio >= 0.90 -> GoalFeasibility.FEASIBLE
            ratio >= 0.70 -> GoalFeasibility.TIGHT
            else -> GoalFeasibility.AGGRESSIVE
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. Leak insights
    // ══════════════════════════════════════════════════════════════════════════

    fun topLeakInsights(answers: QuestionnaireAnswers, tags: Set<String>): List<LeakInsight> {
        val insights = mutableListOf<LeakInsight>()

        // From detailed answers first
        for (key in answers.selectedLeaks) {
            val detail = answers.leakDetails[key]
            val leakInfo = SavingsKnowledgeBase.getLeak(key)
            val costPerUse = detail?.costPerUseDh ?: leakInfo.defaultDailyCostDh
            val freq = detail?.weeklyFrequency ?: leakInfo.defaultDaysPerWeek.toDouble()
            val willingness = detail?.willingness ?: "HALF"
            val monthly = costPerUse * freq * 52.0 / 12.0
            val annual = costPerUse * freq * 52.0
            insights += LeakInsight(
                key = key,
                titleAr = leakInfo.titleAr,
                titleFr = leakInfo.titleFr,
                costPerUseDh = costPerUse,
                timesPerWeek = freq,
                monthlyDrainDh = monthly,
                annualDrainDh = annual,
                saving25pctAnnualDh = annual * 0.25,
                saving50pctAnnualDh = annual * 0.50,
                willingness = willingness
            )
        }

        // Fallback: if no deep answers but wizard set a leisureCategory
        if (insights.isEmpty()) {
            val key = "CAFE"
            val leakInfo = SavingsKnowledgeBase.getLeak(key)
            val monthly = leakInfo.defaultDailyCostDh * leakInfo.defaultDaysPerWeek * 52.0 / 12.0
            val annual = leakInfo.defaultDailyCostDh * leakInfo.defaultDaysPerWeek * 52.0
            insights += LeakInsight(
                key = key,
                titleAr = leakInfo.titleAr,
                titleFr = leakInfo.titleFr,
                costPerUseDh = leakInfo.defaultDailyCostDh,
                timesPerWeek = leakInfo.defaultDaysPerWeek.toDouble(),
                monthlyDrainDh = monthly,
                annualDrainDh = annual,
                saving25pctAnnualDh = annual * 0.25,
                saving50pctAnnualDh = annual * 0.50
            )
        }

        // Sort by monthly drain descending, take top 3
        return insights.sortedByDescending { it.monthlyDrainDh }.take(3)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. Plan options A / B / C
    // ══════════════════════════════════════════════════════════════════════════

    fun generatePlanOptions(
        metrics: DiagnosticMetrics,
        goal: SavingsGoalEntity,
        leaks: List<LeakInsight>,
        isRtl: Boolean
    ): List<SavingsPlanOption> {
        val goalGap = (goal.targetAmountCentimes - goal.currentAmountCentimes).coerceAtLeast(0L)
        val required = metrics.requiredMonthlyCentimes
        val capacity = metrics.realisticCapacityCentimes
        val plans = mutableListOf<SavingsPlanOption>()
        val targetMonths = goal.targetMonths.coerceAtLeast(1)

        val leakRecoverable = leaks.sumOf { (it.monthlyDrainDh * 0.5 * 100).toLong() }

        // Plan A — comfortable (at current realistic capacity, may extend deadline)
        val planAMonthly = maxOf(capacity, goalGap / (targetMonths + 6).coerceAtLeast(1))
        val planAMonths = if (planAMonthly > 0) (goalGap / planAMonthly).toInt().coerceIn(targetMonths, targetMonths + 24) else targetMonths
        plans += SavingsPlanOption(
            label = "A",
            monthlyCentimes = planAMonthly,
            months = planAMonths,
            descAr = "خطة مريحة مع القدرة الحالية — قد تمتد المدة شهوراً إضافية",
            descFr = "Plan confort avec capacité actuelle — délai peut s'étendre",
            requiresLeakReductionCentimes = 0L
        )

        // Plan B — balanced (target on time by recovering some leaks)
        if (required > 0 && abs(required - capacity) < required * 0.5) {
            plans += SavingsPlanOption(
                label = "B",
                monthlyCentimes = required,
                months = targetMonths,
                descAr = "الخطة المتوازنة — تحقيق الهدف في الوقت بتقليص فرصة تسرب واحدة",
                descFr = "Plan équilibré — atteindre l'objectif en réduisant une fuite",
                requiresLeakReductionCentimes = (required - capacity).coerceAtLeast(0L)
            )
        }

        // Plan C — aggressive (faster, only if capacity allows with serious cuts)
        if (leakRecoverable > 0 && capacity + leakRecoverable > required * 1.10) {
            val planCMonthly = (capacity + leakRecoverable).coerceAtMost(required * 130 / 100)
            val planCMonths = if (planCMonthly > 0) (goalGap / planCMonthly).toInt().coerceIn(1, targetMonths) else targetMonths
            if (planCMonths < targetMonths) {
                plans += SavingsPlanOption(
                    label = "C",
                    monthlyCentimes = planCMonthly,
                    months = planCMonths,
                    descAr = "خطة سريعة — بتقليص كامل للتسربات يمكن تحقيق الهدف أسرع بـ ${targetMonths - planCMonths} شهر",
                    descFr = "Plan accéléré — couper toutes les fuites pour atteindre ${targetMonths - planCMonths} mois plus tôt",
                    requiresLeakReductionCentimes = leakRecoverable
                )
            }
        }

        return plans
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5. Top 3 personalized action steps
    // ══════════════════════════════════════════════════════════════════════════

    fun generateTopActions(
        tags: Set<String>,
        leaks: List<LeakInsight>,
        metrics: DiagnosticMetrics,
        goal: SavingsGoalEntity,
        protectedPrefs: List<String>
    ): List<ActionStep> {
        val actions = mutableListOf<ActionStep>()

        // Safety-critical first
        if (DiagnosticTag.CASHFLOW_NEGATIVE in tags || DiagnosticTag.DEBT_STRESS in tags) {
            actions += ActionStep(
                id = "DEBT_RELIEF",
                emojiAr = "🛑", emojiEn = "🛑",
                textAr = "أولوية الشهر: لا تزيد قسطاً جديداً قبل ما تعرف الهامش الحقيقي الشهري",
                textFr = "Priorité ce mois : aucune nouvelle mensualité avant de connaître votre marge réelle",
                category = "DEBT"
            )
        }

        if (DiagnosticTag.NO_EMERGENCY_FUND in tags) {
            actions += ActionStep(
                id = "EMERGENCY_START",
                emojiAr = "🛡️", emojiEn = "🛡️",
                textAr = "ابدأ بـ 200 DH هاد الأسبوع في حساب منفصل بلا بطاقة",
                textFr = "Commencez par 200 DH cette semaine sur un compte séparé sans carte",
                category = "EMERGENCY"
            )
        }

        // Top leak action (skip if protected)
        val topLeak = leaks.firstOrNull { it.key !in protectedPrefs }
        if (topLeak != null) {
            val halfCutMonthly = (topLeak.monthlyDrainDh * 0.5).toLong()
            actions += ActionStep(
                id = "LEAK_CUT_${topLeak.key}",
                emojiAr = "✂️", emojiEn = "✂️",
                textAr = "ضع سقف ${topLeak.monthStr} DH لـ \"${topLeak.titleAr}\" هاد الشهر — توفر +${(topLeak.monthlyDrainDh * 0.5).toLong()} DH",
                textFr = "Plafonnez \"${topLeak.titleFr}\" à ${topLeak.monthStr} DH ce mois — économisez +$halfCutMonthly DH",
                category = "LEAK"
            )
        }

        // Save first action
        if (DiagnosticTag.SAVE_WHATS_LEFT in tags && actions.size < 3) {
            val required = JournalLedgerManager.formatFrenchNumber((metrics.requiredMonthlyCentimes / 100).toString())
            actions += ActionStep(
                id = "SAVE_FIRST",
                emojiAr = "🚀", emojiEn = "🚀",
                textAr = "حوّل $required DH للهدف نهار الصالير مباشرة — قبل صرف أي حاجة",
                textFr = "Virez $required DH vers l'objectif le jour même de la paie",
                category = "GOAL"
            )
        }

        // Tracking action
        if (DiagnosticTag.SPENDING_VISIBILITY_LOW in tags && actions.size < 3) {
            actions += ActionStep(
                id = "TRACKING",
                emojiAr = "📝", emojiEn = "📝",
                textAr = "اكتب كل صرف هاد الأسبوع — غير الكبار — لمدة 7 أيام",
                textFr = "Notez toutes les dépenses de la semaine — même les petites — pendant 7 jours",
                category = "HABIT"
            )
        }

        return actions.take(3)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5b. Comprehensive Actionable Advice Engine (Real-world personalized levers)
    // ══════════════════════════════════════════════════════════════════════════

    fun generatePersonalizedAdviceCards(
        answers: QuestionnaireAnswers,
        tags: Set<String>,
        leaks: List<LeakInsight>,
        metrics: DiagnosticMetrics,
        goal: SavingsGoalEntity
    ): List<DiagnosticAdviceCard> {
        val cards = mutableListOf<DiagnosticAdviceCard>()

        // ── Priority 1: Safety, Debt & Survival Levers ──
        if (answers.endOfMonthBorrowFrequency >= 2) {
            cards += DiagnosticAdviceCard(
                id = "STOP_BORROWING",
                priorityLevel = 1,
                iconEmoji = "🛑",
                titleAr = "أولوية الأمان: إيقاف استلاف نهاية الشهر",
                titleFr = "Priorité sécurité : stopper les emprunts de fin de mois",
                detailedAdviceAr = "إلى كنتي كتسلف فـ آخر الشهر باش تكمل، ما تبداش بقسط توفير كبير يضغط عليك. ركز أولاً على عزل مصروف الأسبوع كاش وتفادي أي دين جديد، حيت كل سلف كيحكم على الشهر الجاي بالعجز.",
                detailedAdviceFr = "Si vous empruntez pour boucler les fins de mois, évitez une mensualité d'épargne trop forte. Isolez les dépenses de la semaine en cash et gelez tout nouvel emprunt.",
                concreteImpactAr = "استقرار التوازن المالي وتفادي دوامة الديون",
                concreteImpactFr = "Stabiliser le budget et stopper l'effet boule de neige",
                categoryTag = "DEBT"
            )
        } else if (answers.debtPaymentsCentimes > 0 && answers.debtType != "DEBT_NONE") {
            cards += DiagnosticAdviceCard(
                id = "DEBT_SNOWBALL",
                priorityLevel = 1,
                iconEmoji = "📋",
                titleAr = "استراتيجية التعامل مع أقساط الديون",
                titleFr = "Stratégie de désendettement progressif",
                detailedAdviceAr = "الأقساط الشهرية كتستهلك جزء مهم من مدخولك. اعتمد استراتيجية تسديد أصغر كريدي أولاً باش تحرر قسط شهري كامل وتحولو مباشرة لحصالة هدفك.",
                detailedAdviceFr = "Remboursez en priorité la plus petite dette pour libérer rapidement une mensualité complète et la basculer vers votre objectif.",
                concreteImpactAr = "تحرير سيولة إضافية ثابتة شهرياً",
                concreteImpactFr = "Libération d'une marge mensuelle pérenne",
                categoryTag = "DEBT"
            )
        }

        if (answers.emergencyFundCentimes <= 0 || answers.emergencyFundLocation == "NONE" || DiagnosticTag.NO_EMERGENCY_FUND in tags) {
            cards += DiagnosticAdviceCard(
                id = "EMERGENCY_SHIELD",
                priorityLevel = 1,
                iconEmoji = "🛡️",
                titleAr = "حزام الأمان قبل الهدف (صندوق الطوارئ)",
                titleFr = "Coussin de sécurité avant l'objectif",
                detailedAdviceAr = "أي هدف توفير بلا صندوق طوارئ يقدر ينهار فـ أول مفاجأة (بان فـ السيارة، طبيب، إصلاح فـ الدار). خصص 200 إلى 400 DH شهرياً فـ حساب بنكي منفصل بلا كارط كيشي حتى تجمع على الأقل مصاريف شهر واحد من الأساسيات.",
                detailedAdviceFr = "Tout objectif sans fonds d'urgence risque d'être cassé au premier imprévu (santé, panne, travaux). Isolez 200 à 400 DH/mois sur un compte séparé sans carte.",
                concreteImpactAr = "حماية الهدف من السحب والكسر الفجائي",
                concreteImpactFr = "Protection de votre épargne contre les retraits imprévus",
                categoryTag = "EMERGENCY"
            )
        }

        // ── Priority 2: Targeted Leaks Reduction ──
        val protectedPrefs = answers.userProtectedPreferences
        val actionableLeaks = leaks.filter { it.key !in protectedPrefs }
        for (leak in actionableLeaks.take(2)) {
            val saveHalfMonth = (leak.monthlyDrainDh * 0.5).toInt()
            val saveHalfYear = (leak.saving50pctAnnualDh).toInt()
            when (leak.key) {
                "CAFE", "FOOD_OUT" -> {
                    cards += DiagnosticAdviceCard(
                        id = "LEAK_CAFE_FOOD",
                        priorityLevel = 2,
                        iconEmoji = "☕",
                        titleAr = "استرجع مصاريف القهوة والزنقة بلا ما تقطع مع الصحاب",
                        titleFr = "Optimiser cafés et repas extérieurs sans privation",
                        detailedAdviceAr = "ما تقطعش القهوة نهائياً؛ شرب قهوة الصباح فـ المكتب أو الدار، وخصص جلسة المقهى مع الأصدقاء كمتعة أسبوعية. نقص 50% من التردد كيرجع ليك +$saveHalfMonth DH شهرياً بلا ما تحس بأي حرمان.",
                        detailedAdviceFr = "Ne vous privez pas totalement : prenez le café du matin à la maison ou au bureau, et gardez la sortie café comme moment plaisir entre amis. Réduire de 50% dégage +$saveHalfMonth DH/mois.",
                        concreteImpactAr = "+$saveHalfMonth DH/شهر (+$saveHalfYear DH/عام)",
                        concreteImpactFr = "+$saveHalfMonth DH/mois (+$saveHalfYear DH/an)",
                        categoryTag = "LEAK"
                    )
                }
                "SHOPPING" -> {
                    cards += DiagnosticAdviceCard(
                        id = "LEAK_SHOPPING",
                        priorityLevel = 2,
                        iconEmoji = "🛍️",
                        titleAr = "سلاح قاعدة 24 ساعة ضد الشراء الاندفاعي",
                        titleFr = "La règle des 24h contre les achats impulsifs",
                        detailedAdviceAr = "قبل ما تشري أي حاجة فايتة 150 DH وما كانتش مبرمجة فـ الميزانية، فرض على راسك تنتظر 24 ساعة. فـ أغلب الحالات، الرغبة اللحظية كتهدا وكتكتشف أنك ما محتاجهاش فعلاً.",
                        detailedAdviceFr = "Avant tout achat non prévu de plus de 150 DH, imposez-vous un délai de 24h. Dans la majorité des cas, l'envie retombe et l'argent reste dans votre poche.",
                        concreteImpactAr = "+$saveHalfMonth DH/شهر توفير فوري",
                        concreteImpactFr = "+$saveHalfMonth DH/mois économisés",
                        categoryTag = "LEAK"
                    )
                }
                "SUBSCRIPTIONS" -> {
                    cards += DiagnosticAdviceCard(
                        id = "LEAK_SUBSCRIPTIONS",
                        priorityLevel = 2,
                        iconEmoji = "📱",
                        titleAr = "مراجعة الفورفيات والاشتراكات المنسية",
                        titleFr = "Audit des forfaits et abonnements oubliés",
                        detailedAdviceAr = "راجع فورفي الهاتف المنزلي والنقال، اشتراكات التطبيقات والقنوات. تبديل الفورفي لـ عرض مناسب أو إلغاء اشتراك غير مستعمل كيعطيك توفير فوري دائم بلا أي مجهود يومي.",
                        detailedAdviceFr = "Revoyez vos forfaits téléphone/internet et résiliez les abonnements inutilisés. Un ajustement rapide génère une économie permanente chaque mois.",
                        concreteImpactAr = "+100 إلى +250 DH/شهر ثابتة",
                        concreteImpactFr = "+100 à +250 DH/mois permanents",
                        categoryTag = "LEAK"
                    )
                }
                else -> {
                    cards += DiagnosticAdviceCard(
                        id = "LEAK_GENERIC_${leak.key}",
                        priorityLevel = 2,
                        iconEmoji = "✂️",
                        titleAr = "تقليص تسرب \"${leak.titleAr}\" تدريجياً",
                        titleFr = "Maîtrise progressive du poste \"${leak.titleFr}\"",
                        detailedAdviceAr = "هاد الباب كيستنزف قرابة ${leak.monthStr} DH شهرياً (${leak.annualStr} DH فـ العام!). وضع سقف أسبوعي أو تقليص عدد المرات للنصف كافي يمول قسط هدفك.",
                        detailedAdviceFr = "Ce poste draine environ ${leak.monthStr} DH/mois (${leak.annualStr} DH/an !). Plafonner à la semaine ou diviser par deux suffit à financer votre objectif.",
                        concreteImpactAr = "+$saveHalfMonth DH/شهر (+$saveHalfYear DH/عام)",
                        concreteImpactFr = "+$saveHalfMonth DH/mois (+$saveHalfYear DH/an)",
                        categoryTag = "LEAK"
                    )
                }
            }
        }

        // ── Priority 2 / 3: Income & Seasonal Levers ──
        if (answers.incomeType != "INCOME_STABLE") {
            cards += DiagnosticAdviceCard(
                id = "VARIABLE_INCOME_PACING",
                priorityLevel = 2,
                iconEmoji = "💼",
                titleAr = "قاعدة الصالير الوهمي لأصحاب الدخل المتغير",
                titleFr = "Règle du salaire de base pour revenus variables",
                detailedAdviceAr = "حيت مدخولك كيتغير من شهر لشهر، حدد لنفسك راتباً شهرياً ثابتاً على أساس أضعف شهر. فـ الشهور القوية، خبي الفائض فـ حساب عازل (Buffer) باش يغطي الشهور الضعيفة ويبقى قسط التوفير منتظم ومستقر.",
                detailedAdviceFr = "Fixez-vous un salaire mensuel de référence basé sur vos mois les plus faibles. Lors des mois forts, versez l'excédent sur un compte tampon pour lisser les périodes creuses.",
                concreteImpactAr = "ضمان استمرارية التوفير بلا انقطاع",
                concreteImpactFr = "Continuité de l'effort d'épargne toute l'année",
                categoryTag = "PACING"
            )
        }

        if (DiagnosticTag.IRREGULAR_EXPENSE_UNFUNDED in tags || answers.seasonalExpenses.isNotEmpty()) {
            cards += DiagnosticAdviceCard(
                id = "SEASONAL_PROVISION",
                priorityLevel = 2,
                iconEmoji = "🌙",
                titleAr = "عزل مخصصات المناسبات السنوية مسبقاً",
                titleFr = "Provisions mensuelles pour chocs saisonniers",
                detailedAdviceAr = "المناسبات المغربية (العيد الكبير، رمضان، الدخول المدرسي، التأمين) ماشي مفاجآت؛ كتجي كل عام فـ نفس التاريخ. عزل 10% شهرياً فـ صندوق خاص بالمناسبات باش نهار توصل ما تضطرش تكسر هدفك ولا تستلف.",
                detailedAdviceFr = "Les événements saisonniers (Aïd, Ramadan, rentrée scolaire, assurances) arrivent chaque année à date fixe. Isolez 10% chaque mois dans une provision dédiée pour ne jamais toucher à votre objectif.",
                concreteImpactAr = "تفادي الصدمات والكسر القسري للهدف",
                concreteImpactFr = "Éviter les ruptures forcées de plan d'épargne",
                categoryTag = "SEASONAL"
            )
        }

        // ── Priority 3: Habits & Execution Excellence ──
        if (answers.savingTiming != "SAVE_FIRST") {
            val reqStr = JournalLedgerManager.formatFrenchNumber((metrics.requiredMonthlyCentimes / 100).toString())
            cards += DiagnosticAdviceCard(
                id = "PAY_YOURSELF_FIRST",
                priorityLevel = 3,
                iconEmoji = "🚀",
                titleAr = "قاعدة: خلص راسك الأول نهار الصالير",
                titleFr = "Règle d'or : Payez-vous d'abord le jour de paie",
                detailedAdviceAr = "ما تسناش نهاية الشهر باش توفر شنو شاط، حيت فـ الغالب ما كيشيط والو. نهار كيدخل الصالير، حوّل $reqStr DH لحساب الهدف مباشرة كأنها فاتورة واجبة الأداء، وعيش بالباقي. هاد العادة كتزيد نسبة نجاح الهدف بـ 3 أضعاف.",
                detailedAdviceFr = "N'attendez pas la fin du mois pour épargner ce qui reste. Dès réception de la paie, virez les $reqStr DH vers l'objectif comme une facture obligatoire. Cette règle multiplie par 3 vos chances de réussite.",
                concreteImpactAr = "ضمان الوصول للهدف بنسبة تفوق 90%",
                concreteImpactFr = "Taux de réussite de l'objectif > 90%",
                categoryTag = "TIMING"
            )
        }

        if (DiagnosticTag.SPENDING_VISIBILITY_LOW in tags || answers.spendingTrackingHabit == "NONE") {
            cards += DiagnosticAdviceCard(
                id = "CASH_ENVELOPES",
                priorityLevel = 3,
                iconEmoji = "✉️",
                titleAr = "الأظرفة النقدية للكماليات والمصروف الأسبوعي",
                titleFr = "La méthode des enveloppes cash pour la semaine",
                detailedAdviceAr = "سحب ميزانية المصروف والكماليات نقداً كل يوم إثنين فـ أظرفة، وخلي البطاقة البنكية فـ الدار. الدفع نقداً كيحسسك بالفلوس كتنقص وكيفرملك الدماغ تلقائياً ضد المصاريف العشوائية.",
                detailedAdviceFr = "Retirez votre budget de poche en espèces chaque lundi et laissez la carte bancaire à la maison. Payer en liquide rend la dépense concrète et freine naturellement les achats impulsifs.",
                concreteImpactAr = "تقليص المصاريف العشوائية بنسبة 20-30%",
                concreteImpactFr = "Baisse des dépenses impulsives de 20 à 30%",
                categoryTag = "BEHAVIOR"
            )
        }

        if (metrics.feasibility == GoalFeasibility.TIGHT || metrics.feasibility == GoalFeasibility.AGGRESSIVE) {
            cards += DiagnosticAdviceCard(
                id = "GOAL_PACING",
                priorityLevel = 3,
                iconEmoji = "⏳",
                titleAr = "تمديد المدة قليلاً لضمان الاستمرارية",
                titleFr = "Ajustement du calendrier pour un marathon serein",
                detailedAdviceAr = "التوفير سباق ماراثون ماشي سباق سرعة. الهدف الحالي كيتطلب قسط فايت طاقتك المريحة. تمديد المدة بـ بضعة أشهر كيخفف العبء الشهري ويخليك تلتزم بلا ما تختنق ولا تستسلم فـ نص الطريق.",
                detailedAdviceFr = "L'épargne est un marathon. Le plan actuel exige une mensualité serrée par rapport à vos marges. Prolonger de quelques mois allège la charge et garantit d'arriver au bout sans abandonner.",
                concreteImpactAr = "خطة مريحة ومستدامة بدون ضغط نفسي",
                concreteImpactFr = "Plan pérenne sans asphyxie financière",
                categoryTag = "PACING"
            )
        }

        // Return sorted by priority (1 safety first, then 2 leaks, then 3 habits)
        return cards.sortedBy { it.priorityLevel }.take(5)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 6. Score and rank article recommendations
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Returns article IDs ordered by relevance score.
     * Article IDs from SavingsArticlesLibrarySection: art_budget, art_cafe, art_emergency, art_24h, art_income, art_annual
     */
    fun scoreContentIds(tags: Set<String>, goalPreset: String): List<String> {
        data class ArticleScore(val id: String, var score: Int)

        val scored = mutableListOf(
            ArticleScore("art_budget", 10),
            ArticleScore("art_cafe", 10),
            ArticleScore("art_emergency", 10),
            ArticleScore("art_24h", 10),
            ArticleScore("art_income", 10),
            ArticleScore("art_annual", 10)
        )

        fun boost(id: String, points: Int) = scored.find { it.id == id }?.let { it.score += points }

        if (DiagnosticTag.FOOD_OUT_HIGH in tags || DiagnosticTag.COFFEE_HIGH in tags) boost("art_cafe", 60)
        if (DiagnosticTag.NO_EMERGENCY_FUND in tags || DiagnosticTag.LOW_EMERGENCY_BUFFER in tags) boost("art_emergency", 60)
        if (DiagnosticTag.IMPULSE_BUYING in tags || DiagnosticTag.DISCOUNT_TRIGGER in tags) boost("art_24h", 60)
        if (DiagnosticTag.CASHFLOW_NEGATIVE in tags || DiagnosticTag.CASHFLOW_TIGHT in tags) boost("art_budget", 60)
        if (DiagnosticTag.INCOME_VARIABLE in tags || DiagnosticTag.INCOME_HIGH_VARIANCE in tags) boost("art_income", 50)
        if (DiagnosticTag.IRREGULAR_EXPENSE_UNFUNDED in tags) boost("art_annual", 50)
        if (DiagnosticTag.SAVE_WHATS_LEFT in tags) boost("art_budget", 40)
        if (goalPreset == "CAR" || goalPreset == "HOUSE") boost("art_annual", 30)
        if (goalPreset == "EMERGENCY") boost("art_emergency", 100)

        return scored.sortedByDescending { it.score }.map { it.id }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 7. Warnings
    // ══════════════════════════════════════════════════════════════════════════

    fun buildWarnings(tags: Set<String>, metrics: DiagnosticMetrics): List<DiagnosticWarning> {
        val warnings = mutableListOf<DiagnosticWarning>()

        if (DiagnosticTag.CASHFLOW_NEGATIVE in tags) {
            warnings += DiagnosticWarning(
                tag = DiagnosticTag.CASHFLOW_NEGATIVE,
                messageAr = "التدفق النقدي الحالي سلبي — الأولوية هي إعادة التوازن قبل تصعيد التوفير",
                messageFr = "Votre flux de trésorerie est négatif — la priorité est de retrouver l'équilibre avant d'augmenter l'épargne",
                isCritical = true
            )
        }
        if (DiagnosticTag.DEBT_STRESS in tags) {
            warnings += DiagnosticWarning(
                tag = DiagnosticTag.DEBT_STRESS,
                messageAr = "عبء الديون فوق 40% من الدخل — هذا سيجعل الاستمرار في الخطة صعباً",
                messageFr = "Charge de dettes >40% du revenu — difficile de tenir le plan sans allégement",
                isCritical = true
            )
        }
        if (DiagnosticTag.NO_EMERGENCY_FUND in tags) {
            warnings += DiagnosticWarning(
                tag = DiagnosticTag.NO_EMERGENCY_FUND,
                messageAr = "بلا صندوق طوارئ: أي طارئ يقدر يهدد استقرار الهدف",
                messageFr = "Sans fonds d'urgence : tout imprévu fragilise l'objectif"
            )
        }
        if (DiagnosticTag.IRREGULAR_EXPENSE_UNFUNDED in tags) {
            warnings += DiagnosticWarning(
                tag = DiagnosticTag.IRREGULAR_EXPENSE_UNFUNDED,
                messageAr = "المصاريف الموسمية (العيد، الدخول المدرسي، التأمين) ما كاينش ليها احتياط شهري",
                messageFr = "Dépenses saisonnières (Aïd, rentrée, assurance) sans provision mensuelle"
            )
        }

        return warnings
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 8. Profile labels
    // ══════════════════════════════════════════════════════════════════════════

    fun buildProfileLabel(tags: Set<String>, metrics: DiagnosticMetrics): Pair<String, String> {
        val ar = buildString {
            if (DiagnosticTag.INCOME_STABLE in tags) append("دخل مستقر")
            else if (DiagnosticTag.INCOME_HIGH_VARIANCE in tags) append("دخل متغير بشكل كبير")
            else if (DiagnosticTag.INCOME_SEASONAL in tags) append("دخل موسمي")
            else append("دخل متغير")

            val fcf = metrics.freeCashFlowCentimes
            when {
                fcf < 0 -> append(" + تدفق سلبي")
                fcf < metrics.requiredMonthlyCentimes * 0.8 -> append(" + هامش ضيق")
                fcf < metrics.requiredMonthlyCentimes * 1.1 -> append(" + هامش متوسط")
                else -> append(" + هامش مريح")
            }
        }
        val fr = buildString {
            if (DiagnosticTag.INCOME_STABLE in tags) append("Revenu stable")
            else if (DiagnosticTag.INCOME_HIGH_VARIANCE in tags) append("Revenu très variable")
            else if (DiagnosticTag.INCOME_SEASONAL in tags) append("Revenu saisonnier")
            else append("Revenu variable")

            val fcf = metrics.freeCashFlowCentimes
            when {
                fcf < 0 -> append(" + flux négatif")
                fcf < metrics.requiredMonthlyCentimes * 0.8 -> append(" + marge étroite")
                fcf < metrics.requiredMonthlyCentimes * 1.1 -> append(" + marge moyenne")
                else -> append(" + marge confortable")
            }
        }
        return Pair(ar, fr)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 9. Narrative summary
    // ══════════════════════════════════════════════════════════════════════════

    fun buildSummary(
        metrics: DiagnosticMetrics,
        tags: Set<String>,
        leaks: List<LeakInsight>,
        feasibility: GoalFeasibility,
        isRtl: Boolean
    ): Pair<String, String> {
        val requiredStr = JournalLedgerManager.formatFrenchNumber((metrics.requiredMonthlyCentimes / 100).toString())
        val capacityStr = JournalLedgerManager.formatFrenchNumber((metrics.realisticCapacityCentimes / 100).toString())
        val topLeakName = leaks.firstOrNull()?.titleAr ?: "المصاريف المرنة"
        val topLeakNameFr = leaks.firstOrNull()?.titleFr ?: "les dépenses flexibles"

        val ar = when (feasibility) {
            GoalFeasibility.COMFORTABLE -> "الهدف داخل فالقدرة ديالك بهامش مريح (قسط $requiredStr درهم/شهر)."
            GoalFeasibility.FEASIBLE -> "الهدف واقعي — يحتاج $requiredStr درهم/شهر وهامشك المريح $capacityStr درهم."
            GoalFeasibility.TIGHT -> "الهدف ممكن لكن ضاغط — يحتاج $requiredStr درهم. أكبر فرصة: $topLeakName."
            GoalFeasibility.AGGRESSIVE -> "الخطة قاصحة ($requiredStr درهم/شهر). الأفضل مد المدة أو خفض $topLeakName."
            GoalFeasibility.UNSAFE_NOW -> "الأولوية استرجاع هامش أمان شهري قبل التفكير في قسط إضافي ثابت."
        }

        val fr = when (feasibility) {
            GoalFeasibility.COMFORTABLE -> "L'objectif est dans vos capacités ($requiredStr DH/mois très accessible)."
            GoalFeasibility.FEASIBLE -> "Objectif réaliste — besoin de $requiredStr DH/mois pour une capacité de $capacityStr DH."
            GoalFeasibility.TIGHT -> "Objectif tendu — $requiredStr DH/mois. Meilleure opportunité : $topLeakNameFr."
            GoalFeasibility.AGGRESSIVE -> "Plan exigeant ($requiredStr DH/mois). Mieux vaut allonger ou réduire $topLeakNameFr."
            GoalFeasibility.UNSAFE_NOW -> "Priorité : retrouver une sécurité mensuelle avant toute épargne fixe."
        }

        return Pair(ar, fr)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 10. Protected areas
    // ══════════════════════════════════════════════════════════════════════════

    fun buildProtectedAreas(answers: QuestionnaireAnswers): Pair<List<String>, List<String>> {
        val ar = mutableListOf<String>()
        val fr = mutableListOf<String>()

        if (answers.housingCentimes > 0) { ar += "🏠 الكراء أو قرض السكن: أساس الاستقرار"; fr += "🏠 Loyer ou crédit logement : pilier de stabilité" }
        if (answers.healthCentimes > 0) { ar += "🏥 الصحة والتطبيب: أولوية قصوى لا تقشف فيها"; fr += "🏥 Santé et pharmacie : priorité absolue" }
        if (answers.groceriesCentimes > 0) { ar += "🥗 التقضية المنزلية: أساس الصحة والتوفير العائلي"; fr += "🥗 Courses maison : base de santé et d'économie" }
        if (answers.educationCentimes > 0) { ar += "📚 الدراسة والتعليم: استثمار لا نمسه"; fr += "📚 Scolarité : investissement sacré" }
        if (answers.utilitiesCentimes > 0) { ar += "⚡ الفواتير الأساسية: نرشد بعقل ولا نقطع"; fr += "⚡ Factures essentielles : à optimiser, jamais couper" }
        if (answers.familyCommitmentCentimes > 0) { ar += "👨‍👩‍👧 التزامات العائلة: حاجة محمية ومصنفة أساسية"; fr += "👨‍👩‍👧 Obligations familiales : poste protégé" }

        if (ar.isEmpty()) {
            ar += "🛡️ المصاريف الأساسية: الكراء، الصحة، والتقضية خط أحمر لا يمس"
            fr += "🛡️ Dépenses essentielles : loyer, santé, alimentation — lignes rouges"
        }

        return Pair(ar, fr)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 11. Austerity steps (compatible with legacy PlanDiagnosis)
    // ══════════════════════════════════════════════════════════════════════════

    fun buildAusteritySteps(
        leaks: List<LeakInsight>,
        goal: SavingsGoalEntity,
        metrics: DiagnosticMetrics
    ): Pair<List<String>, List<String>> {
        val reqStr = JournalLedgerManager.formatFrenchNumber((metrics.requiredMonthlyCentimes / 100).toString())
        val topLeak = leaks.firstOrNull()
        val leakInfo = if (topLeak != null) SavingsKnowledgeBase.getLeak(topLeak.key) else null

        val ar = listOfNotNull(
            "⏳ قاعدة 24 ساعة: التمهل يوماً كاملاً قبل أي شراء غير مبرمج فايت 150 DH",
            leakInfo?.austerityStepAr,
            "💵 الأظرفة الكاش: سحب ميزانية الأسبوع نقداً وتفادي الكارط للكماليات",
            "🚀 الاقتطاع الفوري: عزل $reqStr DH أول ما يدخل الصالير",
            topLeak?.let { "💰 الفرصة الحقيقية: ${it.titleAr} وحدها كتكلفك ${it.monthStr} DH/شهر — نصفها = +${it.save50Str} DH/عام!" }
        )
        val fr = listOfNotNull(
            "⏳ Règle des 24h : Attendre un jour avant tout achat >150 DH",
            leakInfo?.austerityStepFr,
            "💵 Enveloppe cash : Retrait de poche en liquide, laisser la carte",
            "🚀 Payez-vous d'abord : Isoler $reqStr DH dès la paie",
            topLeak?.let { "💰 Opportunité réelle : ${it.titleFr} vous coûte ${it.monthStr} DH/mois — réduire de 50% = +${it.save50Str} DH/an !" }
        )

        return Pair(ar, fr)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 12. MAIN ENTRY POINT — build full diagnostic result
    // ══════════════════════════════════════════════════════════════════════════

    fun buildResult(
        answers: QuestionnaireAnswers,
        goal: SavingsGoalEntity
    ): FullDiagnosticResult {
        val goalPreset = when {
            goal.title.contains("سيارة", true) || goal.title.contains("voiture", true) -> "CAR"
            goal.title.contains("دار", true) || goal.title.contains("سكن", true) || goal.title.contains("maison", true) -> "HOUSE"
            goal.title.contains("طوارئ", true) || goal.title.contains("urgence", true) -> "EMERGENCY"
            goal.title.contains("مشروع", true) || goal.title.contains("projet", true) -> "PROJECT"
            else -> goal.goalPreset.ifBlank { "EVENT" }
        }

        val tags = buildTags(answers, goal)
        val leaks = topLeakInsights(answers, tags)
        val metrics = computeMetrics(answers, goal, tags)
        val (profileAr, profileFr) = buildProfileLabel(tags, metrics)
        val (summaryAr, summaryFr) = buildSummary(metrics, tags, leaks, metrics.feasibility, true)
        val warnings = buildWarnings(tags, metrics)
        val plans = generatePlanOptions(metrics, goal, leaks, true)
        val (protectedAr, protectedFr) = buildProtectedAreas(answers)
        val protectedPrefs = answers.userProtectedPreferences
        val actions = generateTopActions(tags, leaks, metrics, goal, protectedPrefs)
        val adviceCards = generatePersonalizedAdviceCards(answers, tags, leaks, metrics, goal)
        val contentIds = scoreContentIds(tags, goalPreset)
        val (austerityAr, austerityFr) = buildAusteritySteps(leaks, goal, metrics)

        return FullDiagnosticResult(
            profileLabelAr = profileAr,
            profileLabelFr = profileFr,
            summaryAr = summaryAr,
            summaryFr = summaryFr,
            metrics = metrics,
            tags = tags,
            warnings = warnings,
            topLeaks = leaks,
            protectedAreasAr = protectedAr,
            protectedAreasFr = protectedFr,
            planOptions = plans,
            topActions = actions,
            adviceCards = adviceCards,
            recommendedContentIds = contentIds,
            austerityStepsAr = austerityAr,
            austerityStepsFr = austerityFr
        )
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 13. Convert QuestionnaireAnswers → FinancialProfileEntity
    // ══════════════════════════════════════════════════════════════════════════

    fun answersToEntity(
        answers: QuestionnaireAnswers,
        goalId: String,
        tags: Set<String>
    ): FinancialProfileEntity {
        val now = System.currentTimeMillis()
        return FinancialProfileEntity(
            id = java.util.UUID.randomUUID().toString(),
            goalId = goalId,
            goalOwnership = answers.goalOwnership,
            dependentsCount = answers.dependentsCount,
            familyCommitmentCentimes = answers.familyCommitmentCentimes,
            netMonthlyIncomeCentimes = answers.netMonthlyIncomeCentimes,
            incomeType = answers.incomeType,
            incomeLowestCentimes = answers.incomeLowestCentimes,
            incomeHighestCentimes = answers.incomeHighestCentimes,
            additionalIncomeSources = answers.additionalIncomeSources.joinToString(",", "[\"", "\"]") { it },
            additionalIncomeRegularity = answers.additionalIncomeRegularity,
            housingCentimes = answers.housingCentimes,
            utilitiesCentimes = answers.utilitiesCentimes,
            internetPhoneCentimes = answers.internetPhoneCentimes,
            groceriesCentimes = answers.groceriesCentimes,
            workTransportCentimes = answers.workTransportCentimes,
            workTransportType = answers.workTransportType,
            healthCentimes = answers.healthCentimes,
            educationCentimes = answers.educationCentimes,
            insuranceCentimes = answers.insuranceCentimes,
            otherEssentialsCentimes = answers.otherEssentialsCentimes,
            debtType = answers.debtType,
            debtPaymentsCentimes = answers.debtPaymentsCentimes,
            paymentDelayFrequency = answers.paymentDelayFrequency,
            endOfMonthBorrowFrequency = answers.endOfMonthBorrowFrequency,
            emergencyFundCentimes = answers.emergencyFundCentimes,
            emergencyResponse = answers.emergencyResponse,
            emergencyFundLocation = answers.emergencyFundLocation,
            monthEndSituation = answers.monthEndSituation,
            spendingAwareness = answers.spendingAwareness,
            spendingTrackingHabit = answers.spendingTrackingHabit,
            selectedLeaks = answers.selectedLeaks.joinToString(",", "[\"", "\"]") { it },
            leakDetails = buildString {
                append("{")
                answers.leakDetails.entries.forEachIndexed { i, (k, v) ->
                    if (i > 0) append(",")
                    append("\"$k\":{\"cost\":${v.costPerUseDh},\"freq\":${v.weeklyFrequency},\"will\":\"${v.willingness}\"}")
                }
                append("}")
            },
            seasonalExpenses = buildString {
                append("[")
                answers.seasonalExpenses.forEachIndexed { i, se ->
                    if (i > 0) append(",")
                    append("{\"label\":\"${se.label}\",\"amt\":${se.annualCentimes},\"month\":${se.monthDue}}")
                }
                append("]")
            },
            savingTiming = answers.savingTiming,
            hasStandingTransfer = if (answers.hasStandingTransfer) 1 else 0,
            bonusHandling = answers.bonusHandling,
            comfortSavingCentimes = answers.comfortSavingCentimes,
            minimumSavingCentimes = answers.minimumSavingCentimes,
            purchaseDecisionStyle = answers.purchaseDecisionStyle,
            discountTriggerBuying = answers.discountTriggerBuying,
            usesShoppingList = answers.usesShoppingList,
            paymentMethodThatMakesSpendMore = answers.paymentMethodThatMakesSpendMore,
            userProtectedPreferences = answers.userProtectedPreferences.joinToString(",", "[\"", "\"]") { it },
            goalImportance = answers.goalImportance,
            deadlineFlexibility = answers.deadlineFlexibility,
            willingToIncreaseIncome = if (answers.willingToIncreaseIncome) 1 else 0,
            willingToCutFlexible = answers.willingToCutFlexible,
            computedTags = tags.joinToString(",", "[\"", "\"]") { it },
            answersVersion = 1,
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )
    }

    /** Convert persisted entity back to QuestionnaireAnswers domain object. */
    fun entityToAnswers(entity: FinancialProfileEntity): QuestionnaireAnswers {
        fun parseJsonStrArray(s: String): List<String> {
            if (s == "[]" || s.isBlank()) return emptyList()
            return s.trim('[', ']').split(",")
                .map { it.trim().trim('"') }
                .filter { it.isNotBlank() }
        }

        return QuestionnaireAnswers(
            goalOwnership = entity.goalOwnership,
            dependentsCount = entity.dependentsCount,
            hasFamilyCommitments = entity.familyCommitmentCentimes > 0,
            familyCommitmentCentimes = entity.familyCommitmentCentimes,
            netMonthlyIncomeCentimes = entity.netMonthlyIncomeCentimes,
            incomeType = entity.incomeType,
            incomeLowestCentimes = entity.incomeLowestCentimes,
            incomeHighestCentimes = entity.incomeHighestCentimes,
            additionalIncomeSources = parseJsonStrArray(entity.additionalIncomeSources),
            additionalIncomeRegularity = entity.additionalIncomeRegularity,
            housingCentimes = entity.housingCentimes,
            utilitiesCentimes = entity.utilitiesCentimes,
            internetPhoneCentimes = entity.internetPhoneCentimes,
            groceriesCentimes = entity.groceriesCentimes,
            workTransportCentimes = entity.workTransportCentimes,
            workTransportType = entity.workTransportType,
            healthCentimes = entity.healthCentimes,
            educationCentimes = entity.educationCentimes,
            insuranceCentimes = entity.insuranceCentimes,
            otherEssentialsCentimes = entity.otherEssentialsCentimes,
            debtType = entity.debtType,
            debtPaymentsCentimes = entity.debtPaymentsCentimes,
            paymentDelayFrequency = entity.paymentDelayFrequency,
            endOfMonthBorrowFrequency = entity.endOfMonthBorrowFrequency,
            emergencyFundCentimes = entity.emergencyFundCentimes,
            emergencyResponse = entity.emergencyResponse,
            emergencyFundLocation = entity.emergencyFundLocation,
            monthEndSituation = entity.monthEndSituation,
            spendingAwareness = entity.spendingAwareness,
            spendingTrackingHabit = entity.spendingTrackingHabit,
            selectedLeaks = parseJsonStrArray(entity.selectedLeaks),
            savingTiming = entity.savingTiming,
            hasStandingTransfer = entity.hasStandingTransfer == 1,
            bonusHandling = entity.bonusHandling,
            comfortSavingCentimes = entity.comfortSavingCentimes,
            minimumSavingCentimes = entity.minimumSavingCentimes,
            purchaseDecisionStyle = entity.purchaseDecisionStyle,
            discountTriggerBuying = entity.discountTriggerBuying,
            usesShoppingList = entity.usesShoppingList,
            paymentMethodThatMakesSpendMore = entity.paymentMethodThatMakesSpendMore,
            userProtectedPreferences = parseJsonStrArray(entity.userProtectedPreferences),
            goalImportance = entity.goalImportance,
            deadlineFlexibility = entity.deadlineFlexibility,
            willingToIncreaseIncome = entity.willingToIncreaseIncome == 1,
            willingToCutFlexible = entity.willingToCutFlexible
        )
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Private helpers
    // ══════════════════════════════════════════════════════════════════════════

    private fun effectiveIncomeCentimes(answers: QuestionnaireAnswers): Long {
        val explicit = answers.netMonthlyIncomeCentimes
        if (explicit > 0) return explicit

        // For variable income: use conservative estimate (low + safety margin)
        if (answers.incomeLowestCentimes > 0 && answers.incomeHighestCentimes > 0) {
            val safetyBuffer = (answers.incomeHighestCentimes - answers.incomeLowestCentimes) * 0.20
            return (answers.incomeLowestCentimes + safetyBuffer).toLong()
        }
        if (answers.incomeLowestCentimes > 0) return answers.incomeLowestCentimes

        // Absolute fallback — user hasn't answered income yet (shouldn't happen in full interview)
        return 600000L // 6000 DH as default
    }

    private fun protectedEssentialsCentimes(answers: QuestionnaireAnswers): Long {
        return answers.housingCentimes +
                answers.utilitiesCentimes +
                answers.internetPhoneCentimes +
                answers.groceriesCentimes +
                answers.workTransportCentimes +
                answers.healthCentimes +
                answers.educationCentimes +
                answers.insuranceCentimes +
                answers.otherEssentialsCentimes
    }

    private fun estimateFlexibleSpending(
        answers: QuestionnaireAnswers,
        income: Long,
        essentials: Long,
        debts: Long,
        family: Long
    ): Long {
        // If user has detailed leaks, sum them
        val leakTotal = answers.leakDetails.values.sumOf { detail ->
            (detail.costPerUseDh * detail.weeklyFrequency * 52.0 / 12.0 * 100).toLong()
        }
        if (leakTotal > 0) return leakTotal

        // Fallback: estimate 25% of remaining income as flexible
        val remaining = income - essentials - debts - family
        return (remaining * 0.25).toLong().coerceAtLeast(0L)
    }

    // convenience extension for GoalFeasibility stored in SavingsGoalEntity
    private val SavingsGoalEntity.goalPreset: String
        get() = when {
            title.contains("سيارة", true) || title.contains("voiture", true) -> "CAR"
            title.contains("دار", true) || title.contains("سكن", true) -> "HOUSE"
            title.contains("طوارئ", true) || title.contains("urgence", true) -> "EMERGENCY"
            title.contains("مشروع", true) || title.contains("projet", true) -> "PROJECT"
            else -> "EVENT"
        }
}
