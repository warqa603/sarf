package com.cash.guide.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAtEpochMs DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    fun observeNote(id: String): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNote(id: String): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: String)

    @Query("UPDATE notes SET isPinned = :isPinned, updatedAtEpochMs = :updatedAtEpochMs WHERE id = :id")
    suspend fun setPinned(id: String, isPinned: Boolean, updatedAtEpochMs: Long)

    @Query("UPDATE notes SET colorTag = :colorTag, updatedAtEpochMs = :updatedAtEpochMs WHERE id = :id")
    suspend fun setColorTag(id: String, colorTag: String, updatedAtEpochMs: Long)

    @Query("SELECT * FROM notes WHERE groupId = :groupId ORDER BY isPinned DESC, updatedAtEpochMs DESC")
    fun observeByGroup(groupId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE groupId = :groupId ORDER BY isPinned DESC, updatedAtEpochMs DESC")
    suspend fun getNotesByGroup(groupId: String): List<NoteEntity>

    @Query("UPDATE notes SET groupId = :groupId, updatedAtEpochMs = :now WHERE id = :noteId")
    suspend fun assignNoteToGroup(noteId: String, groupId: String?, now: Long)

    @Query("SELECT * FROM notes")
    suspend fun getAllNotes(): List<NoteEntity>

    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()

    @androidx.room.Transaction
    suspend fun restoreNotes(
        notes: List<NoteEntity>,
        replaceExisting: Boolean
    ) {
        if (replaceExisting) {
            deleteAllNotes()
        }
        for (note in notes) {
            insertNote(note)
        }
    }
}
