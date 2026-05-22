package com.example.myapplication

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.myapplication.database.AppDatabase
import com.example.myapplication.database.AppRepository
import com.example.myapplication.models.User
import com.example.myapplication.models.UserRole
import com.example.myapplication.ui.AppNavigation
import com.example.myapplication.ui.ProfessionalActivationScreen
import kotlinx.coroutines.flow.first

@Composable
fun App(database: AppDatabase) {
    var isReady by remember { mutableStateOf(value = false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val isLocked by AppRepository.isLocked

    LaunchedEffect(database) {
        AppRepository.init(database)
        isReady = true
    }

    LaunchedEffect(Unit) {
        AppRepository.errorFlow.collect { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long,
                withDismissAction = true
            )
        }
    }

    MaterialTheme {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (!isReady) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (isLocked) {
                    ProfessionalActivationScreen()
                } else {
                    AppNavigation()
                }
            }
        }
    }
}
