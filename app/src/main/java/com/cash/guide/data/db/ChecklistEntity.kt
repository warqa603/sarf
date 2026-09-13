package com.cash.guide.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "checklists",
    indices = [
        Index("updatedAtEpochMs"),
        Index("groupId")
    ]
)
data class ChecklistEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val groupId: String? = null
)
