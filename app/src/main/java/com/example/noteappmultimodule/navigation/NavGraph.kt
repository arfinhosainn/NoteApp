package com.example.noteappmultimodule.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.noteappmultimodule.presentation.screens.auth.AuthenticationScreen
import com.example.noteappmultimodule.presentation.screens.auth.AuthenticationViewModel
import com.example.noteappmultimodule.utils.Constants.WRITE_SCREEN_ARGUMENT_KEY
import com.stevdzasan.messagebar.rememberMessageBarState
import com.stevdzasan.onetap.rememberOneTapSignInState

@Composable
fun SetupNavGraph(
    startDestination: String, navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        authenticationRoute()
        homeRoute()
        writeRoute()

    }


}


fun NavGraphBuilder.authenticationRoute() {
    composable(route = Screen.Authentication.route) {
        val oneTapSignInState = rememberOneTapSignInState()
        val messageBarState = rememberMessageBarState()
        val viewModel: AuthenticationViewModel = viewModel()
        val loadingState by viewModel.loadingState
        AuthenticationScreen(
            loadingState = loadingState,
            onButtonClicked = {
                oneTapSignInState.open()
                viewModel.setLoading(true)
            },
            oneTapState = oneTapSignInState,
            messageBarState = messageBarState,
            onTokenIdReceived = { tokenId ->
                viewModel.signInWithMongoAtlas(
                    tokenId = tokenId,
                    oneSuccess = {
                        if (it) {
                            messageBarState.addSuccess(message = "Authentication Successful")
                            viewModel.setLoading(false)
                        }
                    },
                    onError = {
                        messageBarState.addError(it)
                    }
                )

            }, onDialogDismissed = { message ->
                messageBarState.addError(Exception(message))

            }
        )
    }
}

fun NavGraphBuilder.homeRoute() {
    composable(route = Screen.Home.route) {

    }
}

fun NavGraphBuilder.writeRoute() {
    composable(
        route = Screen.Write.route,
        arguments = listOf(navArgument(WRITE_SCREEN_ARGUMENT_KEY) {
            type = NavType.StringType
            defaultValue = null
            nullable = true
        })
    ) {

    }
}