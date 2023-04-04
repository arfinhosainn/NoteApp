package com.example.write.navigation

import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.util.Constants
import com.example.util.Screen
import com.example.util.model.Mood
import com.example.write.WriteScreen
import com.example.write.WriteViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.rememberPagerState

@ExperimentalPagerApi
fun NavGraphBuilder.writeRoute(
    onBackPressed: () -> Unit,
    onDeleteConfirmed: () -> Unit
) {
    composable(
        route = Screen.Write.route,
        arguments = listOf(navArgument(Constants.WRITE_SCREEN_ARGUMENT_KEY) {
            type = NavType.StringType
            defaultValue = null
            nullable = true
        })
    ) {
        val pagerState = rememberPagerState()
        val viewModel: WriteViewModel = hiltViewModel()
        val context = LocalContext.current
        val galleryState = viewModel.galleryState
        val uiState = viewModel.uiState

        val pageNumber by remember {
            derivedStateOf {
                pagerState.currentPage
            }
        }

        LaunchedEffect(key1 = uiState) {
            Log.d("selectedNote", "writeRoute: ${uiState.selectedNoteId}")
        }

        WriteScreen(
            onBackPressed = onBackPressed,
            onDeleteNoteConfirmed = {
                viewModel.deleteNote(
                    onSuccess = {
                        Toast.makeText(context, "Note Successfully Deleted", Toast.LENGTH_LONG)
                            .show()
                        onBackPressed()
                    },
                    onError = {
                        Toast.makeText(context, it, Toast.LENGTH_LONG)
                            .show()

                    }
                )
            },
            pagerState = pagerState,
            onTitleChanged = {
                viewModel.setTitle(
                    title = it
                )
            },
            onDescriptionChanged = { viewModel.setDescription(description = it) },
            uiState = uiState,
            moodName = { Mood.values()[pageNumber].name },
            onSavedClick = {
                viewModel.upsertNote(
                    note = it.apply { mood = Mood.values()[pageNumber].name },
                    onSuccess = { onBackPressed() },
                    onError = {error ->
                        Toast.makeText(context, error, Toast.LENGTH_LONG)
                            .show()
                    })

            },
            onUpdateDateTime = {
                viewModel.updateDateTime(it)
            },
            galleryState = galleryState,
            onImageSelect = { image ->
                val type = context.contentResolver.getType(image)?.split("/")?.last() ?: "jpg"

                Log.d("writeviewmodel", "writeRoute: $image")
                viewModel.addImage(
                    image = image,
                    imageType = type
                )

            },
            onImageDeleteClicked = {
                galleryState.removeImage(it)
            }
        )

    }
}