# ProGuard rules for BIZGO App

# ============ Kotlin & Coroutines ============
-keepclassmembers class kotlinx.coroutines.** {
    *;
}

# ============ Compose & Material ============
-keep class androidx.compose.** { *; }
-keep class androidx.material3.** { *; }
-keepclasseswithmembernames class androidx.compose.** { *; }

# ============ Serialization ============
-keepclassmembers class com.example.myapplication.models.** {
    public <init>();
    public <fields>;
}

-keepclassmembers class com.example.myapplication.database.** {
    public <init>();
    public <fields>;
}

# ============ Room Database ============
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class **

# ============ Supabase ============
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.client.** { *; }
-keepclassmembers class io.ktor.client.** { *; }

# ============ Network & JSON ============
-keep class okhttp3.** { *; }
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class com.google.gson.** { *; }

# ============ MLKit Barcode Scanning ============
-keep class com.google.mlkit.** { *; }
-keepclassmembers class com.google.android.gms.** { *; }

# ============ Remove unused resources ============
-dontwarn kotlin.**
-dontwarn kotlinx.**
-dontwarn org.jetbrains.annotations.**
-dontwarn java.lang.invoke.StringConcatFactory

# ============ Optimization ============
-optimizationpasses 5
-dontusemixedcaseclassnames
-verbose

# ============ Keep main classes ============
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.view.View
-keep public class * extends androidx.compose.ui.Composable
