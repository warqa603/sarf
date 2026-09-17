package com.cash.guide.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsDao {

    @Query("SELECT * FROM savings_goals ORDER BY isCompleted ASC, createdAtEpochMs DESC")
    fun getAllGoals(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals WHERE id = :id LIMIT 1")
    suspend fun getGoalById(id: String): SavingsGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: SavingsGoalEntity)

    @Update
    suspend fun updateGoal(goal: SavingsGoalEntity)

    @Query("DELETE FROM savings_goals WHERE id = :id")
    suspend fun deleteGoal(id: String)

    @Query("UPDATE savings_goals SET currentAmountCentimes = :currentAmount, isCompleted = :isCompleted, updatedAtEpochMs = :updatedAt WHERE id = :id")
    suspend fun updateGoalProgress(id: String, currentAmount: Long, isCompleted: Boolean, updatedAt: Long)

    @Query("SELECT * FROM savings_deposits WHERE goalId = :goalId ORDER BY dateEpochMs DESC")
    fun getDepositsForGoal(goalId: String): Flow<List<SavingsDepositEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeposit(deposit: SavingsDepositEntity)

    @Query("DELETE FROM savings_deposits WHERE id = :id")
    suspend fun deleteDeposit(id: String)

    @Query("SELECT * FROM savings_deposits WHERE id = :id LIMIT 1")
    suspend fun getDepositById(id: String): SavingsDepositEntity?

    @Query("SELECT SUM(currentAmountCentimes) FROM savings_goals")
    fun getTotalSavedCentimes(): Flow<Long?>

    @Query("SELECT * FROM savings_goals")
    suspend fun getAllGoalsList(): List<SavingsGoalEntity>

    @Query("SELECT * FROM savings_deposits")
    suspend fun getAllDepositsList(): List<SavingsDepositEntity>

    @Query("DELETE FROM savings_goals")
    suspend fun deleteAllGoals()

    @Query("DELETE FROM savings_deposits")
    suspend fun deleteAllDeposits()

    @androidx.room.Transaction
    suspend fun restoreSavings(
        goals: List<SavingsGoalEntity>,
        deposits: List<SavingsDepositEntity>,
        replaceExisting: Boolean
    ) {
        if (replaceExisting) {
            deleteAllDeposits()
            deleteAllGoals()
        }
        for (goal in goals) {
            insertGoal(goal)
        }
        for (deposit in deposits) {
            insertDeposit(deposit)
        }
    }
}