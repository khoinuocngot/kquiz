package com.example.quizfromfileapp.ui.screens.studyset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quizfromfileapp.data.local.entity.StudySetEntity
import com.example.quizfromfileapp.di.AppContainer
import com.example.quizfromfileapp.ui.theme.AppStringsVi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SortOption(val displayName: String) {
    RECENTLY_UPDATED(AppStringsVi.SortNewest),
    OLDEST(AppStringsVi.SortOldest),
    ALPHABETICAL(AppStringsVi.SortAZ),
    CARD_COUNT(AppStringsVi.SortMostCards)
}

data class StudySetListUiState(
    val allItems: List<StudySetEntity> = emptyList(),
    val filteredItems: List<StudySetEntity> = emptyList(),
    val masteryPercents: Map<Long, Int> = emptyMap(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.RECENTLY_UPDATED,
    val showQuickImport: Boolean = false
)

class StudySetListViewModel : ViewModel() {

    private val repository = AppContainer.studySetRepository

    private val _uiState = MutableStateFlow(StudySetListUiState())
    val uiState: StateFlow<StudySetListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllStudySets().collect { sets ->
                // Load mastery percents in parallel for each set
                val masteryDeferreds = sets.map { set ->
                    viewModelScope.async {
                        val avg = repository.getAverageMastery(set.id)
                        set.id to (avg * 20).toInt() // convert 0-1 → 0-100
                    }
                }
                val masteryMap = masteryDeferreds.map { it.await() }.toMap()

                _uiState.value = _uiState.value.copy(
                    allItems = sets,
                    filteredItems = applyFiltersAndSort(sets),
                    masteryPercents = masteryMap,
                    isLoading = false
                )
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredItems = applyFiltersAndSort(_uiState.value.allItems)
        )
    }

    fun updateSortOption(option: SortOption) {
        _uiState.value = _uiState.value.copy(
            sortOption = option,
            filteredItems = applyFiltersAndSort(_uiState.value.allItems)
        )
    }

    fun showQuickImport() {
        _uiState.value = _uiState.value.copy(showQuickImport = true)
    }

    fun hideQuickImport() {
        _uiState.value = _uiState.value.copy(showQuickImport = false)
    }

    fun deleteStudySet(id: Long) {
        viewModelScope.launch {
            repository.deleteStudySet(id)
        }
    }

    fun duplicateStudySet(id: Long) {
        viewModelScope.launch {
            val original = repository.getStudySetById(id)
            original?.let { set ->
                val duplicate = set.copy(
                    id = 0,
                    title = "${set.title} (bản sao)",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                repository.insertStudySet(duplicate)
            }
        }
    }

    fun renameStudySet(id: Long, newTitle: String) {
        viewModelScope.launch {
            repository.updateStudySetTitle(id, newTitle)
        }
    }

    fun togglePin(id: Long) {
        viewModelScope.launch {
            val set = repository.getStudySetById(id) ?: return@launch
            repository.setPinned(id, !set.isPinned)
        }
    }

    fun toggleFavorite(id: Long) {
        viewModelScope.launch {
            val set = repository.getStudySetById(id) ?: return@launch
            repository.setFavorite(id, !set.isFavorite)
        }
    }

    private fun applyFiltersAndSort(sets: List<StudySetEntity>): List<StudySetEntity> {
        val query = _uiState.value.searchQuery.trim().lowercase()
        val sortOption = _uiState.value.sortOption

        return sets
            .filter { set ->
                if (query.isBlank()) true
                else {
                    set.title.lowercase().contains(query) ||
                    set.description.lowercase().contains(query)
                }
            }
            .let { filtered ->
                when (sortOption) {
                    SortOption.RECENTLY_UPDATED -> filtered.sortedWith(
                        compareByDescending<StudySetEntity> { it.isPinned }
                            .thenByDescending { it.updatedAt }
                    )
                    SortOption.OLDEST -> filtered.sortedWith(
                        compareByDescending<StudySetEntity> { it.isPinned }
                            .thenBy { it.updatedAt }
                    )
                    SortOption.ALPHABETICAL -> filtered.sortedWith(
                        compareByDescending<StudySetEntity> { it.isPinned }
                            .thenBy { it.title.lowercase() }
                    )
                    SortOption.CARD_COUNT -> filtered.sortedWith(
                        compareByDescending<StudySetEntity> { it.isPinned }
                            .thenByDescending { it.cardCount }
                    )
                }
            }
    }
}
