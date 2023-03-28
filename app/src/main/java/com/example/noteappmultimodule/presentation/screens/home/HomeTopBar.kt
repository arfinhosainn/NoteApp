package com.example.noteappmultimodule.presentation.screens.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(onMenuClicked: () -> Unit) {

    TopAppBar(
        navigationIcon = {
            IconButton(onClick = { onMenuClicked() }) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Hamburger Menu",
                    tint = MaterialTheme.colorScheme.onSurface
                )

            }
        },
        title = {
            Text(text = "Note")
        }, actions = {
            IconButton(onClick = { onMenuClicked }) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Date Icon",
                    tint = MaterialTheme.colorScheme.onSurface
                )

            }
        }

    )

}