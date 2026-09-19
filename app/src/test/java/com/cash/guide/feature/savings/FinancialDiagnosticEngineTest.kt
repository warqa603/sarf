package com.cash.guide.feature.savings

import com.cash.guide.data.db.SavingsGoalEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialDiagnosticEngineTest {

    private fun goal() = SavingsGoalEntity(
        id = "goal",
        title = "Voiture",
        targetAmountCentimes = 6_000_000,
        currentAmountCentimes = 0,
        targetMonths = 12,
        createdAtEpochMs = 1,
        updatedAtEpochMs = 1
    )

    @Test
    fun diagnosticExplainsGoalBalanceAndEffort() {
        val answers = QuestionnaireAnswers(
            netMonthlyIncomeCentimes = 1_000_000,
            housingCentimes = 250_000,
            groceriesCentimes = 150_000,
            comfortSavingCentimes = 200_000,
            emergencyFundCentimes = 500_000,
            emergencyFundLocation = "BANK"
        )

        val result = FinancialDiagnosticEngine.buildResult(answers, goal())

        assertEquals(listOf("GOAL_CONTEXT", "MONTHLY_BALANCE", "GOAL_EFFORT"), result.understoodFacts.map { it.id })
        assertTrue(result.dataQualityScore >= 80)
        assertTrue(result.budgetDecisions.any { it.kind == BudgetDecisionKind.PROTECT && it.id == "HOUSING" })
    }

    @Test
    fun declaredLeakCreatesOnlyAcceptedReduction() {
        val answers = QuestionnaireAnswers(
            netMonthlyIncomeCentimes = 800_000,
            housingCentimes = 200_000,
            groceriesCentimes = 120_000,
            selectedLeaks = listOf("CAFE"),
            leakDetails = mapOf("CAFE" to LeakAnswerDetail(costPerUseDh = 20.0, weeklyFrequency = 5.0, willingness = "SOME"))
        )

        val decision = FinancialDiagnosticEngine.buildResult(answers, goal())
            .budgetDecisions.first { it.id == "LEAK_CAFE" }

        assertEquals(BudgetDecisionKind.REDUCE, decision.kind)
        assertTrue(decision.monthlyImpactCentimes > 0)
        assertEquals(decision.currentMonthlyCentimes - decision.monthlyImpactCentimes, decision.suggestedMonthlyCentimes)
    }

    @Test
    fun seasonalExpenseBecomesMonthlyProvision() {
        val answers = QuestionnaireAnswers(
            netMonthlyIncomeCentimes = 800_000,
            seasonalExpenses = listOf(SeasonalExpenseAnswer("الدخول المدرسي", 120_000, 9))
        )

        val provision = FinancialDiagnosticEngine.buildResult(answers, goal())
            .budgetDecisions.first { it.kind == BudgetDecisionKind.PREPARE }

        assertEquals(10_000, provision.suggestedMonthlyCentimes)
    }

    @Test
    fun missingIncomeIsNeverReplacedWithInventedSalary() {
        val result = FinancialDiagnosticEngine.buildResult(QuestionnaireAnswers(), goal())

        assertEquals(0, result.metrics.monthlyIncomeCentimes)
        assertTrue(result.metrics.flexibleSpendingEstimated)
        assertTrue(result.dataQualityScore < 60)
    }

    @Test
    fun recommendedPlanNeverUsesMoreThanGoalNeedsWhenCapacityIsHigh() {
        val answers = QuestionnaireAnswers(
            netMonthlyIncomeCentimes = 10_000_000,
            comfortSavingCentimes = 6_000_000
        )

        val result = FinancialDiagnosticEngine.buildResult(answers, goal())
        val recommended = result.planOptions.first()

        assertEquals(result.metrics.requiredMonthlyCentimes, recommended.monthlyCentimes)
        assertEquals(goal().targetMonths, recommended.months)
    }

    @Test
    fun recommendedPlanExtendsDurationWhenCapacityIsBelowRequired() {
        val answers = QuestionnaireAnswers(
            netMonthlyIncomeCentimes = 800_000,
            comfortSavingCentimes = 250_000
        )

        val result = FinancialDiagnosticEngine.buildResult(answers, goal())
        val recommended = result.planOptions.first()

        assertEquals(250_000, recommended.monthlyCentimes)
        assertEquals(24, recommended.months)
    }
}
