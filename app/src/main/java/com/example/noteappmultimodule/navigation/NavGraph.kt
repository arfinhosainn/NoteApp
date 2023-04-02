package com.example.noteappmultimodule.navigation

import android.util.Log
import android.widget.Toast
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.noteappmultimodule.data.MongoDB
import com.example.noteappmultimodule.model.GalleryImage
import com.example.noteappmultimodule.model.Mood
import com.example.noteappmultimodule.model.RequestState
import com.example.noteappmultimodule.model.rememberGalleryState
import com.example.noteappmultimodule.presentation.components.DisplayAlertDialog
import com.example.noteappmultimodule.presentation.screens.auth.AuthenticationScreen
import com.example.noteappmultimodule.presentation.screens.auth.AuthenticationViewModel
import com.example.noteappmultimodule.presentation.screens.home.HomeScreen
import com.example.noteappmultimodule.presentation.screens.home.HomeViewModel
import com.example.noteappmultimodule.presentation.screens.write.WriteScreen
import com.example.noteappmultimodule.presentation.screens.write.WriteViewModel
import com.example.noteappmultimodule.utils.Constants.APP_ID
import com.example.noteappmultimodule.utils.Constants.WRITE_SCREEN_ARGUMENT_KEY
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.rememberPagerState
import com.stevdzasan.messagebar.rememberMessageBarState
import com.stevdzasan.onetap.rememberOneTapSignInState
import io.realm.kotlin.mongodb.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalPagerApi::class)
@Composable
fun SetupNavGraph(
    startDestination: String, navController: NavHostController,
    onDataLoaded: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        authenticationRoute(
            navigateToHome = {
                navController.popBackStack()
                navController.navigate(Screen.Home.route)
            }
        )
        homeRoute(
            navigateToWrite = {
                navController.navigate(Screen.Write.route)
            },
            navigateToAuth = {
                navController.popBackStack()
                navController.navigate(Screen.Authentication.route)
            },
            onDataLoaded = onDataLoaded,
            navigateToWriteWithArgs = {
                navController.navigate(Screen.Write.passNoteId(noteId = it))
            }
        )
        writeRoute(onBackPressed = {
            navController.popBackStack()
        }, onDeleteConfirmed = {

        })
    }
}


fun NavGraphBuilder.authenticationRoute(navigateToHome: () -> Unit) {
    composable(route = Screen.Authentication.route) {
        val oneTapSignInState = rememberOneTapSignInState()
        val messageBarState = rememberMessageBarState()
        val viewModel: AuthenticationViewModel = viewModel()
        val loadingState by viewModel.loadingState
        val authenticated by viewModel.authenticated
        AuthenticationScreen(authenticated = authenticated,
            loadingState = loadingState,
            onButtonClicked = {
                oneTapSignInState.open()
                viewModel.setLoading(true)
            },
            oneTapState = oneTapSignInState,
            messageBarState = messageBarState,
            onSuccessfulFirebaseSignIn = { tokenId ->
                viewModel.signInWithMongoAtlas(
                    tokenId = tokenId,
                    oneSuccess = {
                        messageBarState.addSuccess(message = "Authentication Successful")
                        viewModel.setLoading(false)
                    },
                    onError = {
                        messageBarState.addError(it)
                        viewModel.setLoading(false)
                    }
                )

            }, onDialogDismissed = { message ->
                messageBarState.addError(Exception(message))
                viewModel.setLoading(false)

            },
            navigateToHome = navigateToHome,
            onFailedFirebaseSignIn = {
                messageBarState.addError(it)
                viewModel.setLoading(false)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.homeRoute(
    navigateToWrite: () -> Unit,
    navigateToAuth: () -> Unit,
    onDataLoaded: () -> Unit,
    navigateToWriteWithArgs: (String) -> Unit
) {
    composable(route = Screen.Home.route) {
        val viewModel: HomeViewModel = viewModel()
        val notes by viewModel.notes
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        var signOutDialogOpened by remember {
            mutableStateOf(false)
        }

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
            navigateToWriteWithArgs = navigateToWriteWithArgs
        )
        LaunchedEffect(key1 = Unit) {
            MongoDB.getAllNotes()
        }

        DisplayAlertDialog(
            title = "Sign Out",
            message = "Are you sure you want to Sign Out from your Google Account?",
            dialogOpened = signOutDialogOpened,
            onDialogClosed = { signOutDialogOpened = false },
            onYesClicked = {
                scope.launch(Dispatchers.IO) {
                    val user = App.create(APP_ID).currentUser
                    if (user != null) {
                        user.logOut()
                        withContext(Dispatchers.Main) {
                            navigateToAuth()
                        }
                    }
                }
            }
        )
    }
}

@ExperimentalPagerApi
fun NavGraphBuilder.writeRoute(
    onBackPressed: () -> Unit,
    onDeleteConfirmed: () -> Unit
) {
    composable(
        route = Screen.Write.route,
        arguments = listOf(navArgument(WRITE_SCREEN_ARGUMENT_KEY) {
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
                    onError = {
                        Toast.makeText(context, it, Toast.LENGTH_LONG)
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