package com.example.home.navigation

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.home.HomeScreen
import com.example.home.HomeViewModel
import com.example.mongo.repository.MongoDB
import com.example.ui.components.DisplayAlertDialog
import com.example.util.Constants.APP_ID
import com.example.util.Screen
import com.example.util.model.RequestState
import io.realm.kotlin.mongodb.App
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@RequiresApi(Build.VERSION_CODES.O)
fun NavGraphBuilder.homeRoute(
    navigateToWrite: () -> Unit,
    navigateToAuth: () -> Unit,
    onDataLoaded: () -> Unit,
    navigateToWriteWithArgs: (String) -> Unit
) {
    composable(route = Screen.Home.route) {
        val viewModel: HomeViewModel = hiltViewModel()
        val notes by viewModel.notes
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        var signOutDialogOpened by remember {
            mutableStateOf(false)
        }

        var deleteAllDialogOpened by remember {
            mutableStateOf(false)
        }

        val context = LocalContext.current


        LaunchedEffect(key1 = notes) {
            if (notes !is RequestState.Loading) {
                onDataLoaded()
            }
        }
        HomeScreen(
            notes = notes,
            onMenuClicked = {
                scope.launch {
                    drawerState.open()
                }
            },
            navigateToWrite = navigateToWrite,
            onSignOutClicked = { signOutDialogOpened = true },
            drawerState = drawerState,
            navigateToWriteWithArgs = navigateToWriteWithArgs,
            onDeleteAllClicked = {
                deleteAllDialogOpened = true
            },
            dateIsSelected = viewModel.dateIsSelected,
            onDateReset = {
                viewModel.getNotes()

            },
            onDateSelected = {
                viewModel.getNotes(zonedDateTime = it)
            }
        )

//        LaunchedEffect(key1 = Unit) {
//            MongoDB.getAllNotes()
//        }

        DisplayAlertDialog(
            title = "Sign Out",
            message = "Are you sure you want to Sign Out from your Google Account?",
            dialogOpened = signOutDialogOpened,
            onDialogClosed = { signOutDialogOpened = false },
            onYesClicked = {
                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    val user = App.create(APP_ID).currentUser
                    if (user != null) {
                        user.logOut()
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            navigateToAuth()
                        }
                    }
                }
            }
        )
        DisplayAlertDialog(
            title = "Delete all notes",
            message = "Are you sure you want to delete all your notes",
            dialogOpened = deleteAllDialogOpened,
            onDialogClosed = { deleteAllDialogOpened = false },
            onYesClicked = {
                viewModel.deleteAllNotes(
                    onSuccess = {
                        Toast.makeText(
                            context,
                            "All notes deleted successfully",
                           Toast.LENGTH_LONG
                        )
                            .show()
                        scope.launch {
                            drawerState.close()
                        }
                    },
                    onError = {
                        Toast.makeText(
                            context,
                            if (it.message ==
                                "No internet connection"
                            ) "We need an internet connection to operation this task" else it.message,
                          Toast.LENGTH_LONG
                        )
                            .show()
                        scope.launch {
                            drawerState.close()
                        }

                    }
                )

            }
        )
    }
}
