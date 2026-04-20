package com.example.quizfromfileapp.ui.screens.organization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quizfromfileapp.data.local.entity.FolderEntity
import com.example.quizfromfileapp.data.local.entity.TagEntity
import com.example.quizfromfileapp.data.repository.OrganizationRepository
import com.example.quizfromfileapp.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class FolderWithCount(
    val folder: FolderEntity,
    val studySetCount: Int = 0
)

data class TagWithCount(
    val tag: TagEntity,
    val studySetCount: Int = 0
)

data class OrganizationUiState(
    val folders: List<FolderWithCount> = emptyList(),
    val tags: List<TagWithCount> = emptyList(),
    val isLoading: Boolean = true,
    // Dialog states
    val showFolderDialog: Boolean = false,
    val editingFolder: FolderEntity? = null,
    val showTagDialog: Boolean = false,
    val editingTag: TagEntity? = null,
    val showDeleteFolderDialog: Boolean = false,
    val deletingFolderId: Long? = null,
    val showDeleteTagDialog: Boolean = false,
    val deletingTagId: Long? = null,
    // Form state
    val folderName: String = "",
    val folderDescription: String = "",
    val folderColor: String = "#5B6CFF",
    val tagName: String = "",
    val tagColor: String = "#8B5CF6",
    // Folder form errors
    val folderNameError: String? = null,
    val tagNameError: String? = null,
)

class OrganizationViewModel : ViewModel() {

    private val orgRepo: OrganizationRepository = AppContainer.organizationRepository

    private val _uiState = MutableStateFlow(OrganizationUiState())
    val uiState: StateFlow<OrganizationUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                orgRepo.getAllFolders(),
                orgRepo.getAllTags()
            ) { folders, tags ->
                Pair(folders, tags)
            }.collectLatest { (folders, tags) ->
                // Load counts in parallel
                val folderCounts = folders.map { folder ->
                    FolderWithCount(
                        folder = folder,
                        studySetCount = orgRepo.getStudySetCountInFolder(folder.id)
                    )
                }
                val tagCounts = tags.map { tag ->
                    TagWithCount(
                        tag = tag,
                        studySetCount = orgRepo.getStudySetCountWithTag(tag.id)
                    )
                }
                _uiState.value = _uiState.value.copy(
                    folders = folderCounts,
                    tags = tagCounts,
                    isLoading = false
                )
            }
        }
    }

    // ─── Folder Dialog ────────────────────────────────────────

    fun showCreateFolderDialog() {
        _uiState.value = _uiState.value.copy(
            showFolderDialog = true,
            editingFolder = null,
            folderName = "",
            folderDescription = "",
            folderColor = FolderEntity.DEFAULT_COLORS.random(),
            folderNameError = null
        )
    }

    fun showEditFolderDialog(folder: FolderEntity) {
        _uiState.value = _uiState.value.copy(
            showFolderDialog = true,
            editingFolder = folder,
            folderName = folder.name,
            folderDescription = folder.description,
            folderColor = folder.colorHex,
            folderNameError = null
        )
    }

    fun dismissFolderDialog() {
        _uiState.value = _uiState.value.copy(
            showFolderDialog = false,
            editingFolder = null,
            folderNameError = null
        )
    }

    fun updateFolderName(name: String) {
        _uiState.value = _uiState.value.copy(
            folderName = name,
            folderNameError = if (name.isBlank()) "Tên thư mục không được trống" else null
        )
    }

    fun updateFolderDescription(desc: String) {
        _uiState.value = _uiState.value.copy(folderDescription = desc)
    }

    fun updateFolderColor(color: String) {
        _uiState.value = _uiState.value.copy(folderColor = color)
    }

    fun saveFolder() {
        val state = _uiState.value
        if (state.folderName.isBlank()) {
            _uiState.value = state.copy(folderNameError = "Tên thư mục không được trống")
            return
        }

        viewModelScope.launch {
            val editing = state.editingFolder
            if (editing != null) {
                orgRepo.updateFolder(editing.id, state.folderName, state.folderDescription, state.folderColor)
            } else {
                orgRepo.createFolder(state.folderName, state.folderDescription, state.folderColor)
            }
            dismissFolderDialog()
        }
    }

    // ─── Folder Delete ────────────────────────────────────────

    fun showDeleteFolderDialog(folderId: Long) {
        _uiState.value = _uiState.value.copy(
            showDeleteFolderDialog = true,
            deletingFolderId = folderId
        )
    }

    fun dismissDeleteFolderDialog() {
        _uiState.value = _uiState.value.copy(
            showDeleteFolderDialog = false,
            deletingFolderId = null
        )
    }

    fun confirmDeleteFolder() {
        val folderId = _uiState.value.deletingFolderId ?: return
        viewModelScope.launch {
            orgRepo.deleteFolder(folderId)
            dismissDeleteFolderDialog()
        }
    }

    // ─── Tag Dialog ───────────────────────────────────────────

    fun showCreateTagDialog() {
        _uiState.value = _uiState.value.copy(
            showTagDialog = true,
            editingTag = null,
            tagName = "",
            tagColor = TagEntity.DEFAULT_COLORS.random(),
            tagNameError = null
        )
    }

    fun showEditTagDialog(tag: TagEntity) {
        _uiState.value = _uiState.value.copy(
            showTagDialog = true,
            editingTag = tag,
            tagName = tag.name,
            tagColor = tag.colorHex,
            tagNameError = null
        )
    }

    fun dismissTagDialog() {
        _uiState.value = _uiState.value.copy(
            showTagDialog = false,
            editingTag = null,
            tagNameError = null
        )
    }

    fun updateTagName(name: String) {
        _uiState.value = _uiState.value.copy(
            tagName = name,
            tagNameError = if (name.isBlank()) "Tên nhãn không được trống" else null
        )
    }

    fun updateTagColor(color: String) {
        _uiState.value = _uiState.value.copy(tagColor = color)
    }

    fun saveTag() {
        val state = _uiState.value
        if (state.tagName.isBlank()) {
            _uiState.value = state.copy(tagNameError = "Tên nhãn không được trống")
            return
        }

        viewModelScope.launch {
            val editing = state.editingTag
            if (editing != null) {
                orgRepo.updateTag(editing.id, state.tagName, state.tagColor)
            } else {
                orgRepo.createTag(state.tagName, state.tagColor)
            }
            dismissTagDialog()
        }
    }

    // ─── Tag Delete ───────────────────────────────────────────

    fun showDeleteTagDialog(tagId: Long) {
        _uiState.value = _uiState.value.copy(
            showDeleteTagDialog = true,
            deletingTagId = tagId
        )
    }

    fun dismissDeleteTagDialog() {
        _uiState.value = _uiState.value.copy(
            showDeleteTagDialog = false,
            deletingTagId = null
        )
    }

    fun confirmDeleteTag() {
        val tagId = _uiState.value.deletingTagId ?: return
        viewModelScope.launch {
            orgRepo.deleteTag(tagId)
            dismissDeleteTagDialog()
        }
    }
}
