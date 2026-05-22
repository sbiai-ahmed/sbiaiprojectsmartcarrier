package com.example.myapplication.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import android.print.PrintManager
import android.print.PrintAttributes
import androidx.core.content.FileProvider
import java.io.File
import java.lang.ref.WeakReference

@SuppressLint("StaticFieldLeak")
actual object PlatformUtils {
    private var context: Context? = null
    private var activityRef: WeakReference<Activity>? = null
    private var currentWebView: WebView? = null // لمنع الـ GC أثناء عملية الطباعة

    fun init(activity: Activity) {
        context = activity.applicationContext
        activityRef = WeakReference(activity)
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
            
            val activity = activityRef?.get()
            if (activity != null) {
                activity.startActivity(Intent.createChooser(intent, "تصدير التقرير"))
            } else {
                currentContext.startActivity(
                    Intent.createChooser(intent, "تصدير التقرير").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun printHtml(html: String, jobName: String) {
        val activity = activityRef?.get() ?: return
        
        Handler(Looper.getMainLooper()).post {
            try {
                // إنشاء WebView مع سياق النشاط (ضروري جداً)
                val webView = WebView(activity)
                currentWebView = webView
                
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        try {
                            val printManager = activity.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                            if (printManager != null) {
                                // إنشاء محول الطباعة من الـ WebView
                                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                                
                                // بدء عملية الطباعة
                                printManager.print(
                                    jobName, 
                                    printAdapter, 
                                    PrintAttributes.Builder().build()
                                )
                            }
                        } catch (e: Exception) {
                            println("Printing failed: ${e.message}")
                        }
                    }
                }
                
                // إعدادات الـ WebView لضمان عرض المحتوى بشكل صحيح
                webView.settings.defaultTextEncodingName = "UTF-8"
                
                // إضافة غطاء HTML كامل لضمان استقرار العرض
                val fullHtml = "<html><head><meta charset='utf-8'></head><body>$html</body></html>"
                
                // تحميل البيانات
                webView.loadDataWithBaseURL("file:///android_asset/", fullHtml, "text/html", "UTF-8", null)
            } catch (e: Exception) {
                println("Critical print error: ${e.message}")
            }
        }
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
