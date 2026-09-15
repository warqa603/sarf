package com.cash.guide.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "savings_goals",
    indices = [
        Index(value = ["isCompleted"]),
        Index(value = ["createdAtEpochMs"])
    ]
)
data class SavingsGoalEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val targetAmountCentimes: Long,
    val currentAmountCentimes: Long = 0L,
    val monthlyContributionCentimes: Long = 0L,
    val targetDateEpochMs: Long? = null,
    val currency: String = "DIRHAM",
    val colorTag: String = "BLUE",
    val icon: String = "STAR",
    val isCompleted: Boolean = false,
    val targetMonths: Int = 24,
    val monthlySalaryCentimes: Long = 0L,
    val essentialBracket: String = "MEDIUM",
    val leisureCategory: String = "CAFE",
    val savingsStyle: String = "BALANCED",
    val initialAmountCentimes: Long = 0L,
    val leakDailyCostCentimes: Long = 2500L,
    val leakDaysPerWeek: Int = 6,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)