package com.example.myapplication.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

fun getDatabaseBuilder(ctx: Context): RoomDatabase.Builder<AppDatabase> {
    return Room.inMemoryDatabaseBuilder<AppDatabase>(
        context = ctx.applicationContext
    )
}
