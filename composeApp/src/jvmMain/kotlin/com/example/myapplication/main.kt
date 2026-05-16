package com.example.myapplication

import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.myapplication.database.getDatabaseBuilder
import com.example.myapplication.database.getRoomDatabase

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Phone Management",
    ) {
        val db = remember {
            val builder = getDatabaseBuilder()
            getRoomDatabase(builder)
        }
        App(database = db)
    }
}
