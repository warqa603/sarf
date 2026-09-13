package com.cash.guide.data

import com.cash.guide.data.db.NoteDao
import com.cash.guide.data.db.NoteEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class NoteRepository(
    private val noteDao: NoteDao
) {
    fun observeAll(): Flow<List<NoteEntity>> = noteDao.observeAll()

    fun observeByGroup(groupId: String): Flow<List<NoteEntity>> = noteDao.observeByGroup(groupId)

    suspend fun getNotesByGroup(groupId: String): List<NoteEntity> = noteDao.getNotesByGroup(groupId)

    fun observeNote(id: String): Flow<NoteEntity?> = noteDao.observeNote(id)

    suspend fun getNote(id: String): NoteEntity? = noteDao.getNote(id)

    suspend fun createNote(
        title: String = "",
        content: String = "",
        colorTag: String = "DEFAULT",
        groupId: String? = null
    ): String {
        val noteId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val note = NoteEntity(
            id = noteId,
            title = title.trim(),
            content = content.trim(),
            colorTag = colorTag,
            isPinned = false,
            createdAtEpochMs = now,
            updatedAtEpochMs = now,
            groupId = groupId
        )
        noteDao.insertNote(note)
        return noteId
    }

    suspend fun assignNoteToGroup(noteId: String, groupId: String?) {
        noteDao.assignNoteToGroup(noteId, groupId, System.currentTimeMillis())
    }

    suspend fun insertNote(note: NoteEntity) {
        noteDao.insertNote(note)
    }

    suspend fun updateNote(
        id: String,
        title: String,
        content: String,
        colorTag: String? = null,
        isPinned: Boolean? = null
    ) {
        val existing = noteDao.getNote(id) ?: return
        val now = System.currentTimeMillis()
        val updated = existing.copy(
            title = title.trim(),
            content = content.trim(),
            colorTag = colorTag ?: existing.colorTag,
            isPinned = isPinned ?: existing.isPinned,
            updatedAtEpochMs = now
        )
        noteDao.updateNote(updated)
    }

    suspend fun deleteNote(id: String) {
        noteDao.deleteNote(id)
    }

    suspend fun togglePin(id: String, isPinned: Boolean) {
        noteDao.setPinned(id, isPinned, System.currentTimeMillis())
    }

    suspend fun setColorTag(id: String, colorTag: String) {
        noteDao.setColorTag(id, colorTag, System.currentTimeMillis())
    }
}
