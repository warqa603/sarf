package com.cash.guide.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "savings_deposits",
    foreignKeys = [
        ForeignKey(
            entity = SavingsGoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["goalId"]),
        Index(value = ["dateEpochMs"])
    ]
)
data class SavingsDepositEntity(
    @PrimaryKey
    val id: String,
    val goalId: String,
    val amountCentimes: Long,
    val note: String = "",
    val dateEpochMs: Long
)