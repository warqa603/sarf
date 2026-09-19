package com.cash.guide.feature.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cash.guide.data.CalculationRepository
import com.cash.guide.data.ContactRepository
import com.cash.guide.data.db.CalculationGroupEntity
import com.cash.guide.data.db.ContactEntity
import com.cash.guide.domain.GroupCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ContactsUiState(
    val contacts: List<ContactEntity> = emptyList(),
    val groups: List<CalculationGroupEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedGroupId: String? = null, // null = All, "UNGROUPED" = without group, or specific groupId
    val isAddSheetOpen: Boolean = false,
    val editingContact: ContactEntity? = null,
    val contactToDelete: ContactEntity? = null,
    val isLoading: Boolean = true
) {
    val filteredContacts: List<ContactEntity>
        get() {
            return contacts.filter { contact ->
                val matchesGroup = when (selectedGroupId) {
                    null -> true
                    "UNGROUPED" -> contact.groupId == null
                    else -> contact.groupId == selectedGroupId
                }
                val matchesQuery = if (searchQuery.isBlank()) true else {
                    contact.name.contains(searchQuery, ignoreCase = true) ||
                    contact.phoneNumber.contains(searchQuery, ignoreCase = true) ||
                    (contact.secondaryPhone?.contains(searchQuery, ignoreCase = true) == true) ||
                    (contact.note?.contains(searchQuery, ignoreCase = true) == true)
                }
                matchesGroup && matchesQuery
            }
        }
}

class ContactsViewModel(
    private val contactRepository: ContactRepository,
    private val calculationRepository: CalculationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                contactRepository.observeAll(),
                calculationRepository.observeAllGroups()
            ) { contacts, allGroups ->
                val contactGroups = allGroups.filter {
                    it.category.equals(GroupCategory.CONTACTS.storageKey, ignoreCase = true)
                }
                _uiState.update {
                    it.copy(
                        contacts = contacts,
                        groups = contactGroups,
                        isLoading = false
                    )
                }
            }.collect {}
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectGroup(groupId: String?) {
        _uiState.update { it.copy(selectedGroupId = groupId) }
    }

    fun openAddContactSheet(prefillContact: ContactEntity? = null) {
        _uiState.update { it.copy(isAddSheetOpen = true, editingContact = prefillContact) }
    }

    fun closeAddContactSheet() {
        _uiState.update { it.copy(isAddSheetOpen = false, editingContact = null) }
    }

    fun saveContact(
        id: String?,
        name: String,
        phoneNumber: String,
        secondaryPhone: String?,
        note: String?,
        groupId: String?,
        colorTag: String
    ) {
        viewModelScope.launch {
            if (id != null) {
                val existing = contactRepository.getContact(id)
                if (existing != null) {
                    contactRepository.updateContact(
                        existing.copy(
                            name = name.trim(),
                            phoneNumber = phoneNumber.trim(),
                            secondaryPhone = secondaryPhone?.trim()?.ifBlank { null },
                            note = note?.trim()?.ifBlank { null },
                            groupId = groupId,
                            colorTag = colorTag
                        )
                    )
                }
            } else {
                contactRepository.createContact(
                    name = name,
                    phoneNumber = phoneNumber,
                    secondaryPhone = secondaryPhone,
                    note = note,
                    groupId = groupId,
                    colorTag = colorTag
                )
            }
            closeAddContactSheet()
        }
    }

    fun togglePin(contactId: String) {
        viewModelScope.launch {
            contactRepository.togglePin(contactId)
        }
    }

    fun confirmDelete(contact: ContactEntity) {
        _uiState.update { it.copy(contactToDelete = contact) }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(contactToDelete = null) }
    }

    fun deleteContact(contactId: String) {
        viewModelScope.launch {
            contactRepository.deleteContact(contactId)
            dismissDeleteDialog()
        }
    }

    fun createGroup(name: String, colorHex: String) {
        viewModelScope.launch {
            calculationRepository.createGroup(name.trim(), colorHex, GroupCategory.CONTACTS.storageKey)
        }
    }
}
