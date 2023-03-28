package com.example.noteappmultimodule.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.noteappmultimodule.presentation.screens.auth.AuthenticationScreen
import com.example.noteappmultimodule.presentation.screens.auth.AuthenticationViewModel
import com.example.noteappmultimodule.utils.Constants.APP_ID
import com.example.noteappmultimodule.utils.Constants.WRITE_SCREEN_ARGUMENT_KEY
import com.stevdzasan.messagebar.rememberMessageBarState
import com.stevdzasan.onetap.rememberOneTapSignInState
import io.realm.kotlin.mongodb.App
import kotlinx.coroutines.launch

@Composable
fun SetupNavGraph(
    startDestination: String, navController: NavHostController
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
        homeRoute()
        writeRoute()

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
            onTokenIdReceived = { tokenId ->
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
            navigateToHome = navigateToHome
        )
    }
}

fun NavGraphBuilder.homeRoute() {
    composable(route = Screen.Home.route) {
        val scope = rememberCoroutineScope()
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Button(onClick = {

                scope.launch {
                    App.create(APP_ID).currentUser?.logOut()
                }
            }
            )
            { Text(text = "Logged Out") }
        }
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