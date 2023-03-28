package com.example.noteappmultimodule.presentation.screens.home

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.noteappmultimodule.data.MongoDB
import com.example.noteappmultimodule.data.Notes
import com.example.noteappmultimodule.model.RequestState
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    var notes: MutableState<Notes> = mutableStateOf(RequestState.Idle)

    init {
        observeAllNotes()
    }

    private fun observeAllNotes() {
        viewModelScope.launch {
            MongoDB.getAllNotes().collect { result ->
                notes.value = result
            }
        }
    }


}