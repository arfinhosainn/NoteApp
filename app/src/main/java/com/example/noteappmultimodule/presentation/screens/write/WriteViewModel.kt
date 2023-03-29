package com.example.noteappmultimodule.presentation.screens.write

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.noteappmultimodule.data.MongoDB
import com.example.noteappmultimodule.model.Mood
import com.example.noteappmultimodule.model.Note
import com.example.noteappmultimodule.model.RequestState
import com.example.noteappmultimodule.utils.Constants.WRITE_SCREEN_ARGUMENT_KEY
import io.realm.kotlin.types.ObjectId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WriteViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    var uiState by mutableStateOf(UiState())
        private set

    init {
        getNoteArgument()
        fetchSelectedNote()
    }

    private fun getNoteArgument() {
        uiState = uiState.copy(
            selectedNoteId = savedStateHandle.get<String>(
                key = WRITE_SCREEN_ARGUMENT_KEY
            )
        )
    }

    private fun fetchSelectedNote() {
        if (uiState.selectedNoteId != null) {
            viewModelScope.launch {
                MongoDB.getSelectedNote(
                    noteId = ObjectId.Companion.from(uiState.selectedNoteId!!)
                ).collect { note ->
                    if (note is RequestState.Success) {
                        setSelectedNote(note = note.data)
                        setTitle(note.data.title)
                        setDescription(note.data.description)
                        setMood(mood = Mood.valueOf(note.data.mood))
                    }
                }
            }
        }
    }


    fun insertNote(
        note: Note,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = MongoDB.addNewNote(note)
            if (result is RequestState.Success) {
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } else if (result is RequestState.Error) {
                withContext(Dispatchers.Main) {
                    onError(result.error.message.toString())
                }
            }
        }
    }


    fun setTitle(title: String) {
        uiState = uiState.copy(title = title)
    }

    fun setDescription(description: String) {
        uiState = uiState.copy(description = description)
    }

    private fun setMood(mood: Mood) {
        uiState = uiState.copy(mood = mood)
    }

    private fun setSelectedNote(note: Note) {
        uiState = uiState.copy(selectedNote = note)
    }


}


data class UiState(
    val selectedNoteId: String? = null,
    val title: String = "",
    val selectedNote: Note? = null,
    val description: String = "",
    val mood: Mood = Mood.Neutral
)