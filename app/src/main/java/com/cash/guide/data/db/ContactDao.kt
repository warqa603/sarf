package com.cash.guide.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {

    @Query("SELECT * FROM contacts ORDER BY isPinned DESC, name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE id = :id LIMIT 1")
    fun observeContact(id: String): Flow<ContactEntity?>

    @Query("SELECT * FROM contacts WHERE id = :id LIMIT 1")
    suspend fun getContact(id: String): ContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity)

    @Update
    suspend fun updateContact(contact: ContactEntity)

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteContact(id: String)

    @Query("UPDATE contacts SET isPinned = :isPinned, updatedAtEpochMs = :updatedAtEpochMs WHERE id = :id")
    suspend fun setPinned(id: String, isPinned: Boolean, updatedAtEpochMs: Long)

    @Query("SELECT * FROM contacts WHERE groupId = :groupId ORDER BY isPinned DESC, name COLLATE NOCASE ASC")
    fun observeByGroup(groupId: String): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE groupId = :groupId ORDER BY isPinned DESC, name COLLATE NOCASE ASC")
    suspend fun getContactsByGroup(groupId: String): List<ContactEntity>

    @Query("UPDATE contacts SET groupId = :groupId, updatedAtEpochMs = :now WHERE id = :contactId")
    suspend fun assignContactToGroup(contactId: String, groupId: String?, now: Long)

    @Query("SELECT * FROM contacts")
    suspend fun getAllContacts(): List<ContactEntity>

    @Query("DELETE FROM contacts")
    suspend fun deleteAllContacts()
}
