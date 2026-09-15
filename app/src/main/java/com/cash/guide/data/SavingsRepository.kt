package com.cash.guide.data

import com.cash.guide.data.db.FinancialProfileDao
import com.cash.guide.data.db.FinancialProfileEntity
import com.cash.guide.data.db.SavingsDao
import com.cash.guide.data.db.SavingsDepositEntity
import com.cash.guide.data.db.SavingsGoalEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class SavingsRepository(
    private val savingsDao: SavingsDao,
    private val financialProfileDao: FinancialProfileDao
) {

    val allGoals: Flow<List<SavingsGoalEntity>> = savingsDao.getAllGoals()

    val totalSavedCentimes: Flow<Long> = savingsDao.getTotalSavedCentimes().map { it ?: 0L }

    suspend fun createGoal(
        title: String,
        targetAmountCentimes: Long,
        initialAmountCentimes: Long = 0L,
        monthlyContributionCentimes: Long = 0L,
        targetDateEpochMs: Long? = null,
        currency: String = "DIRHAM",
        colorTag: String = "BLUE",
        icon: String = "STAR",
        targetMonths: Int = 24,
        monthlySalaryCentimes: Long = 0L,
        essentialBracket: String = "MEDIUM",
        leisureCategory: String = "CAFE",
        savingsStyle: String = "BALANCED",
        leakDailyCostCentimes: Long = 2500L,
        leakDaysPerWeek: Int = 6
    ): String {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val isCompleted = initialAmountCentimes >= targetAmountCentimes && targetAmountCentimes > 0
        val goal = SavingsGoalEntity(
            id = id,
            title = title.trim(),
            targetAmountCentimes = targetAmountCentimes,
            currentAmountCentimes = initialAmountCentimes,
            monthlyContributionCentimes = monthlyContributionCentimes,
            targetDateEpochMs = targetDateEpochMs,
            currency = currency,
            colorTag = colorTag,
            icon = icon,
            isCompleted = isCompleted,
            targetMonths = targetMonths,
            monthlySalaryCentimes = monthlySalaryCentimes,
            essentialBracket = essentialBracket,
            leisureCategory = leisureCategory,
            savingsStyle = savingsStyle,
            initialAmountCentimes = initialAmountCentimes,
            leakDailyCostCentimes = leakDailyCostCentimes,
            leakDaysPerWeek = leakDaysPerWeek,
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )
        savingsDao.insertGoal(goal)
        if (initialAmountCentimes > 0) {
            val deposit = SavingsDepositEntity(
                id = UUID.randomUUID().toString(),
                goalId = id,
                amountCentimes = initialAmountCentimes,
                note = "Départ",
                dateEpochMs = now
            )
            savingsDao.insertDeposit(deposit)
        }
        return id
    }

    suspend fun updateGoal(
        id: String,
        title: String,
        targetAmountCentimes: Long,
        monthlyContributionCentimes: Long = 0L,
        targetDateEpochMs: Long? = null,
        colorTag: String = "BLUE",
        icon: String = "STAR"
    ) {
        val existing = savingsDao.getGoalById(id) ?: return
        val now = System.currentTimeMillis()
        val isCompleted = existing.currentAmountCentimes >= targetAmountCentimes && targetAmountCentimes > 0
        val updated = existing.copy(
            title = title.trim(),
            targetAmountCentimes = targetAmountCentimes,
            monthlyContributionCentimes = monthlyContributionCentimes,
            targetDateEpochMs = targetDateEpochMs,
            colorTag = colorTag,
            icon = icon,
            isCompleted = isCompleted,
            updatedAtEpochMs = now
        )
        savingsDao.updateGoal(updated)
    }

    suspend fun addDeposit(
        goalId: String,
        amountCentimes: Long,
        note: String = ""
    ) {
        if (amountCentimes <= 0) return
        val goal = savingsDao.getGoalById(goalId) ?: return
        val now = System.currentTimeMillis()
        val newAmount = goal.currentAmountCentimes + amountCentimes
        val isCompleted = newAmount >= goal.targetAmountCentimes && goal.targetAmountCentimes > 0

        savingsDao.insertDeposit(
            SavingsDepositEntity(
                id = UUID.randomUUID().toString(),
                goalId = goalId,
                amountCentimes = amountCentimes,
                note = note.trim(),
                dateEpochMs = now
            )
        )
        savingsDao.updateGoalProgress(goalId, newAmount, isCompleted, now)
    }

    suspend fun deleteGoalWithProfile(id: String) {
        financialProfileDao.deleteProfileForGoal(id)
        savingsDao.deleteGoal(id)
    }

    suspend fun deleteDeposit(depositId: String) {
        val deposit = savingsDao.getDepositById(depositId) ?: return
        val goal = savingsDao.getGoalById(deposit.goalId)
        savingsDao.deleteDeposit(depositId)
        if (goal != null) {
            val now = System.currentTimeMillis()
            val newAmount = (goal.currentAmountCentimes - deposit.amountCentimes).coerceAtLeast(0L)
            val isCompleted = newAmount >= goal.targetAmountCentimes && goal.targetAmountCentimes > 0
            savingsDao.updateGoalProgress(goal.id, newAmount, isCompleted, now)
        }
    }

    suspend fun updateGoalDuration(goalId: String, newMonths: Int) {
        val goal = savingsDao.getGoalById(goalId) ?: return
        val months = newMonths.coerceIn(1, 120)
        val newMonthly = if (months > 0) goal.targetAmountCentimes / months else goal.targetAmountCentimes
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = goal.createdAtEpochMs
            add(java.util.Calendar.MONTH, months)
        }
        val updated = goal.copy(
            targetMonths = months,
            monthlyContributionCentimes = newMonthly,
            targetDateEpochMs = cal.timeInMillis,
            updatedAtEpochMs = System.currentTimeMillis()
        )
        savingsDao.updateGoal(updated)
    }

    fun getDepositsForGoal(goalId: String): Flow<List<SavingsDepositEntity>> {
        return savingsDao.getDepositsForGoal(goalId)
    }

    suspend fun saveFinancialProfile(entity: FinancialProfileEntity) {
        financialProfileDao.insertProfile(entity)
    }

    suspend fun updateFinancialProfile(entity: FinancialProfileEntity) {
        financialProfileDao.updateProfile(entity)
    }

    fun getProfileForGoalFlow(goalId: String): Flow<FinancialProfileEntity?> {
        return financialProfileDao.getProfileForGoalFlow(goalId)
    }

    suspend fun getProfileForGoal(goalId: String): FinancialProfileEntity? {
        return financialProfileDao.getProfileForGoal(goalId)
    }
}