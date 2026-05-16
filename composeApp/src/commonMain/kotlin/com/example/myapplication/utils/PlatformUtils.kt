package com.example.myapplication.utils

expect object PlatformUtils {
    fun saveAndShareFile(fileName: String, content: String, mimeType: String)
    fun printHtml(html: String, jobName: String) // إضافة ميزة الطباعة
    fun currentTimeMillis(): Long
    fun formatDate(timestamp: Long): String
    fun getDeviceId(): String
}
