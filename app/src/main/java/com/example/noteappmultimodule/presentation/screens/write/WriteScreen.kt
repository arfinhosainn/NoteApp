package com.example.noteappmultimodule.presentation.screens.write

import android.annotation.SuppressLint
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.example.noteappmultimodule.model.Mood
import com.example.noteappmultimodule.model.Note
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerState
import java.time.ZonedDateTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPagerApi::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun WriteScreen(
    uiState: UiState,
    onBackPressed: () -> Unit,
    moodName: () -> String,
    onDeleteNoteConfirmed: () -> Unit,
    pagerState: PagerState,
    onTitleChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onSavedClick: (Note) -> Unit,
    onUpdateDateTime: (ZonedDateTime) -> Unit
) {
    LaunchedEffect(key1 = uiState.mood) {
        pagerState.scrollToPage(Mood.valueOf(uiState.mood.name).ordinal)
    }

    Scaffold(
        topBar = {
            WriteTopBar(
                onBackPressed = onBackPressed,
                selectedNote = uiState.selectedNote,
                onDeleteConfirmed = onDeleteNoteConfirmed,
                moodName = moodName,
                onUpdateDateTime = onUpdateDateTime
            )

        }, content = {
            WriteContent(
                pagerState = pagerState,
                title = uiState.title,
                onTitleChanged = onTitleChanged,
                onDescriptionChanged = onDescriptionChanged,
                description = uiState.description,
                paddingValues = it,
                onSavedClick = onSavedClick, uiState = uiState
            )

        }
    )

}