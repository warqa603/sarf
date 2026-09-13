package com.cash.guide.feature.note

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cash.guide.data.NoteRepository
import com.cash.guide.data.db.NoteEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class NoteMonthGroup(
    val monthYearKey: String,
    val displayTitle: String,
    val notes: List<NoteEntity>
)

data class NotesOverviewUiState(
    val searchQuery: String = "",
    val selectedMonthKey: String? = null,
    val availableMonths: List<Pair<String, String>> = emptyList(), // Pair(key, displayTitle)
    val monthGroups: List<NoteMonthGroup> = emptyList(),
    val totalCount: Int = 0,
    val isLoading: Boolean = true
)

class NotesOverviewViewModel(
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedMonthKey = MutableStateFlow<String?>(null)
    val selectedMonthKey: StateFlow<String?> = _selectedMonthKey

    init {
        viewModelScope.launch {
            if (noteRepository.getNote("sample_projet_app") == null) {
                noteRepository.insertNote(
                    NoteEntity(
                        id = "sample_projet_app",
                        title = "Projet application",
                        content = "- Garder le style carnet\n- Pas de grosses cartes\n- Search simple et visible\n- Partage en image plus tard\n- Sections avec highlights très doux\n\nIdée : ajouter un petit calcul rapide dans les notes (comme dans Hssabi) pour les budgets, voyages, etc.\n\nPenser aussi au mode sombre !",
                        colorTag = "YELLOW",
                        isPinned = true,
                        createdAtEpochMs = System.currentTimeMillis() - 86400000L * 3,
                        updatedAtEpochMs = System.currentTimeMillis() - 86400000L * 3
                    )
                )
            }
        }
    }

    val uiState: StateFlow<NotesOverviewUiState> = combine(
        noteRepository.observeAll(),
        _searchQuery,
        _selectedMonthKey
    ) { allNotes, query, selectedMonth ->
        val monthKeyFormatter = SimpleDateFormat("yyyy-MM", Locale.US)
        val monthDisplayFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

        // Compute available distinct months from all existing notes
        val allMonthKeys = allNotes.map { note ->
            monthKeyFormatter.format(Date(note.createdAtEpochMs))
        }.distinct()

        val availableMonths = allMonthKeys.map { key ->
            val sampleNote = allNotes.first { monthKeyFormatter.format(Date(it.createdAtEpochMs)) == key }
            val raw = monthDisplayFormatter.format(Date(sampleNote.createdAtEpochMs))
            val display = raw.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
            Pair(key, display)
        }

        // Apply month filter
        val monthFiltered = if (selectedMonth.isNullOrBlank()) {
            allNotes
        } else {
            allNotes.filter { note ->
                monthKeyFormatter.format(Date(note.createdAtEpochMs)) == selectedMonth
            }
        }

        // Apply search query filter
        val searchFiltered = if (query.isBlank()) {
            monthFiltered
        } else {
            val q = query.trim().lowercase()
            monthFiltered.filter { note ->
                note.title.lowercase().contains(q) || note.content.lowercase().contains(q)
            }
        }

        // Group by Month and Year
        val groupedMap = searchFiltered.groupBy { note ->
            monthKeyFormatter.format(Date(note.createdAtEpochMs))
        }

        val groups = groupedMap.map { (key, notesInMonth) ->
            val sortedNotes = notesInMonth.sortedWith(
                compareByDescending<NoteEntity> { it.isPinned }
                    .thenByDescending { it.updatedAtEpochMs.coerceAtLeast(it.createdAtEpochMs) }
            )
            val firstNoteDate = Date(sortedNotes.first().createdAtEpochMs)
            val rawTitle = monthDisplayFormatter.format(firstNoteDate)
            val displayTitle = rawTitle.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
            NoteMonthGroup(
                monthYearKey = key,
                displayTitle = displayTitle,
                notes = sortedNotes
            )
        }.sortedByDescending { it.monthYearKey }

        NotesOverviewUiState(
            searchQuery = query,
            selectedMonthKey = selectedMonth,
            availableMonths = availableMonths,
            monthGroups = groups,
            totalCount = searchFiltered.size,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NotesOverviewUiState()
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectMonth(monthKey: String?) {
        _selectedMonthKey.value = monthKey
    }

    fun deleteNote(id: String) {
        viewModelScope.launch {
            noteRepository.deleteNote(id)
        }
    }

    fun togglePin(id: String, isPinned: Boolean) {
        viewModelScope.launch {
            noteRepository.togglePin(id, isPinned)
        }
    }

    fun setColorTag(id: String, colorTag: String) {
        viewModelScope.launch {
            noteRepository.setColorTag(id, colorTag)
        }
    }

    fun assignNoteToGroup(id: String, groupId: String?) {
        viewModelScope.launch {
            noteRepository.assignNoteToGroup(id, groupId)
        }
    }

    suspend fun createNewNote(): String {
        return noteRepository.createNote()
    }
}
