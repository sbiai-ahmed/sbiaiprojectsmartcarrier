package com.example.myapplication.utils

import java.io.File

actual object PlatformUtils {
    actual fun saveAndShareFile(fileName: String, content: String, mimeType: String) {
        val userHome = System.getProperty("user.home")
        val downloadsDir = File(userHome, "Downloads")
        if (!downloadsDir.exists()) downloadsDir.mkdirs()
        
        val file = File(downloadsDir, fileName)
        file.writeText(content)
        println("File saved to: ${file.absolutePath}")
        
        try {
            java.awt.Desktop.getDesktop().open(downloadsDir)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun printHtml(html: String, jobName: String) {
        // في نسخة الكمبيوتر، نقوم بحفظ الملف كـ HTML وفتحه في المتصفح ليتمكن المستخدم من طباعته
        saveAndShareFile("$jobName.html", html, "text/html")
    }

    actual fun currentTimeMillis(): Long = System.currentTimeMillis()

    actual fun formatDate(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    actual fun getDeviceId(): String {
        return System.getProperty("user.name") + "_" + System.getProperty("os.name")
    }
}
