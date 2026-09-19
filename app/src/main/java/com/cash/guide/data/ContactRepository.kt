package com.cash.guide.data

import com.cash.guide.data.db.ContactDao
import com.cash.guide.data.db.ContactEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ContactRepository(
    private val contactDao: ContactDao
) {
    fun observeAll(): Flow<List<ContactEntity>> = contactDao.observeAll()

    fun observeByGroup(groupId: String): Flow<List<ContactEntity>> = contactDao.observeByGroup(groupId)

    fun observeContact(id: String): Flow<ContactEntity?> = contactDao.observeContact(id)

    suspend fun getContact(id: String): ContactEntity? = contactDao.getContact(id)

    suspend fun createContact(
        name: String,
        phoneNumber: String,
        secondaryPhone: String? = null,
        note: String? = null,
        groupId: String? = null,
        colorTag: String = "BLUE"
    ): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val entity = ContactEntity(
            id = id,
            name = name.trim(),
            phoneNumber = phoneNumber.trim(),
            secondaryPhone = secondaryPhone?.trim()?.ifBlank { null },
            note = note?.trim()?.ifBlank { null },
            groupId = groupId,
            colorTag = colorTag,
            isPinned = false,
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )
        contactDao.insertContact(entity)
        return id
    }

    suspend fun updateContact(contact: ContactEntity) {
        val updated = contact.copy(updatedAtEpochMs = System.currentTimeMillis())
        contactDao.updateContact(updated)
    }

    suspend fun deleteContact(id: String) {
        contactDao.deleteContact(id)
    }

    suspend fun togglePin(id: String) {
        val current = contactDao.getContact(id) ?: return
        contactDao.setPinned(id, !current.isPinned, System.currentTimeMillis())
    }

    suspend fun assignGroup(contactId: String, groupId: String?) {
        contactDao.assignContactToGroup(contactId, groupId, System.currentTimeMillis())
    }

    suspend fun getAllContacts(): List<ContactEntity> = contactDao.getAllContacts()

    suspend fun deleteAllContacts() {
        contactDao.deleteAllContacts()
    }
}
