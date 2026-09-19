package com.cash.guide.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "contacts",
    indices = [
        Index("groupId"),
        Index("updatedAtEpochMs")
    ]
)
data class ContactEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phoneNumber: String,
    val secondaryPhone: String? = null,
    val note: String? = null,
    val groupId: String? = null,
    val colorTag: String = "BLUE",
    val isPinned: Boolean = false,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)
