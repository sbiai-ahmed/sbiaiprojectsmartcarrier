package com.example.myapplication.database

import androidx.room.Room
import androidx.room.RoomDatabase

fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    return Room.inMemoryDatabaseBuilder<AppDatabase>()
}
