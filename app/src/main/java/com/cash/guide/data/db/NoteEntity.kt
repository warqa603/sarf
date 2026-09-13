package com.cash.guide.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["updatedAtEpochMs"]),
        Index(value = ["isPinned"]),
        Index(value = ["groupId"])
    ]
)
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val colorTag: String = "DEFAULT", // "DEFAULT", "YELLOW", "PINK", "BLUE", "GREEN", "PURPLE"
    val isPinned: Boolean = false,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val groupId: String? = null
)
