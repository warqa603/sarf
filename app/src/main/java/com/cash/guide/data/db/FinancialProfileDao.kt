package com.cash.guide.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: FinancialProfileEntity)

    @Update
    suspend fun updateProfile(profile: FinancialProfileEntity)

    @Query("SELECT * FROM financial_profiles WHERE goalId = :goalId LIMIT 1")
    fun getProfileForGoalFlow(goalId: String): Flow<FinancialProfileEntity?>

    @Query("SELECT * FROM financial_profiles WHERE goalId = :goalId LIMIT 1")
    suspend fun getProfileForGoal(goalId: String): FinancialProfileEntity?

    @Query("DELETE FROM financial_profiles WHERE goalId = :goalId")
    suspend fun deleteProfileForGoal(goalId: String)

    @Query("SELECT * FROM financial_profiles")
    suspend fun getAllProfiles(): List<FinancialProfileEntity>

    @Query("DELETE FROM financial_profiles")
    suspend fun deleteAllProfiles()

    @androidx.room.Transaction
    suspend fun restoreProfiles(
        profiles: List<FinancialProfileEntity>,
        replaceExisting: Boolean
    ) {
        if (replaceExisting) {
            deleteAllProfiles()
        }
        for (profile in profiles) {
            insertProfile(profile)
        }
    }
}
