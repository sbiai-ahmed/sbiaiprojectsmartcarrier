package com.example.myapplication.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

@SuppressLint("StaticFieldLeak")
actual object PlatformUtils {
    private var context: Context? = null

    fun init(ctx: Context) {
        context = ctx.applicationContext
    }

    actual fun saveAndShareFile(fileName: String, content: String, mimeType: String) {
        val currentContext = context ?: return
        
        try {
            val cacheDir = File(currentContext.cacheDir, "reports")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            
            val file = File(cacheDir, fileName)
            file.writeText(content)
            
            val uri = FileProvider.getUriForFile(
                currentContext,
                "${currentContext.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            currentContext.startActivity(
                Intent.createChooser(intent, "تصدير التقرير").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun printHtml(html: String, jobName: String) {
        val currentContext = context ?: return
        val webView = android.webkit.WebView(currentContext)
        webView.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                val printManager = currentContext.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                printManager.print(
                    jobName, 
                    printAdapter, 
                    android.print.PrintAttributes.Builder().build()
                )
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    actual fun currentTimeMillis(): Long = System.currentTimeMillis()

    actual fun formatDate(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    actual fun getDeviceId(): String {
        val currentContext = context ?: return "unknown_android"
        return android.provider.Settings.Secure.getString(
            currentContext.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "unknown_id"
    }

    actual fun generateUUID(): String = java.util.UUID.randomUUID().toString()
}
