package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.example.myapplication.database.getDatabaseBuilder
import com.example.myapplication.database.getRoomDatabase
import com.example.myapplication.utils.PlatformUtils

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        // تهيئة أدوات النظام
        PlatformUtils.init(this)

        setContent {
            val db = remember {
                val builder = getDatabaseBuilder(applicationContext)
                getRoomDatabase(builder)
            }
            App(database = db)
        }
    }
}
