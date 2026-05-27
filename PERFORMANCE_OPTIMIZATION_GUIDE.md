# 📊 دليل تحسين الأداء وحل مشاكل Runtime

## 🎯 تحليل الأداء والمشاكل المحتملة

---

## ⚠️ **المشاكل المحتملة المكتشفة:**

### 1. 🔴 **Memory Leaks و State Management**

#### المشكلة:
```kotlin
// ❌ في Repository.kt - تسريب الذاكرة المحتمل
val errorFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)

// ❌ في DashboardViewModel - عمليات حسابية ثقيلة بدون caching
val dashboardState = combine(sales, repairs, products, expenses) { sales, repairs, products, expenses ->
    // عمليات حسابية معقدة تتكرر بدون تخزين مؤقت
    val netProfit = ...
    val weeklyRevenue = ...
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = DashboardData())
```

#### الحل:
```kotlin
// ✅ استخدام Buffer محدود
val errorFlow = MutableSharedFlow<String>(
    replay = 0,
    extraBufferCapacity = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
)

// ✅ Memoization للحسابات الثقيلة
private val cachedDashboardData = mutableMapOf<String, DashboardData>()

val dashboardState = combine(sales, repairs, products, expenses) { sales, repairs, products, expenses ->
    val cacheKey = "${sales.size}-${repairs.size}-${products.size}"
    
    if (cacheKey !in cachedDashboardData) {
        cachedDashboardData[cacheKey] = calculateDashboard(sales, repairs, products, expenses)
    }
    
    cachedDashboardData[cacheKey]!!
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = DashboardData())
```

---

### 2. 🔴 **النسخ العميقة (Deep Copies) غير الضرورية**

#### المشكلة:
```kotlin
// ❌ في POSScreen.kt - نسخ غير ضرورية من الـ Lists
cartItems = cartItems.map { item ->
    if (item.id == cartItem.id) item.copy(quantity = newQuantity) else item
}

// ❌ في InventoryScreen.kt - حسابات متكررة
val total = products.sumOf { it.stockQuantity }
val lowStock = products.count { it.stockQuantity < it.minStockAlert }
val totalValue = products.sumOf { it.buyPrice * it.stockQuantity }
```

#### الحل:
```kotlin
// ✅ استخدام Mutable Collections
private var cartItems by remember { mutableStateOf<List<CartItem>>(emptyList()) }

// ✅ عند التحديث، تحديث نسخة واحدة فقط
fun updateCartItem(id: String, newQuantity: Int) {
    val index = cartItems.indexOfFirst { it.id == id }
    if (index >= 0) {
        cartItems = cartItems.toMutableList().apply {
            set(index, get(index).copy(quantity = newQuantity))
        }
    }
}

// ✅ استخدام remember مع derivedStateOf
val inventoryStats = remember(products) {
    Products.groupBy { it.category }
        .mapValues { (_, items) ->
            Triple(
                items.sumOf { it.stockQuantity },
                items.count { it.stockQuantity < it.minStockAlert },
                items.sumOf { it.buyPrice * it.stockQuantity }
            )
        }
}
```

---

### 3. 🔴 **عمليات Coroutine غير المُدارة**

#### المشكلة:
```kotlin
// ❌ في LoginScreen.kt - إمكانية تسريب الـ Coroutines
scope.launch {
    try {
        // عملية طويلة
        val uid = client.auth.currentSessionOrNull()?.user?.id
    } catch (e: Throwable) {
        // قد لا يتم إلغاء العملية عند الخروج من الـ Screen
    }
}

// ❌ في PlatformUtils.android.kt - WebView بدون cleanup
val webView = WebView(context)
webView.loadDataWithBaseURL(...)
// لا يوجد webView.destroy() عند الانتهاء
```

#### الحل:
```kotlin
// ✅ استخدام DisposableEffect
DisposableEffect(Unit) {
    val job = scope.launch {
        try {
            val uid = client.auth.currentSessionOrNull()?.user?.id
        } catch (e: Throwable) {
            errorMessage = e.message ?: "خطأ غير متوقع"
        }
    }
    
    onDispose {
        job.cancel()
    }
}

// ✅ في WebView - استخدام cleanup
var webView: WebView? = null
DisposableEffect(Unit) {
    val context = LocalContext.current
    webView = WebView(context).apply {
        settings.apply {
            defaultTextEncodingName = "UTF-8"
            javaScriptEnabled = false
        }
        loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null)
    }
    
    onDispose {
        webView?.destroy()
        webView = null
    }
}
```

---

### 4. 🟡 **LazyColumn بدون Keys**

#### المشكلة:
```kotlin
// ❌ بدون keys - إعادة تصيير غير ضرورية
LazyColumn {
    items(products) { product ->
        ProductCard(product)
    }
}
```

#### الحل:
```kotlin
// ✅ مع Keys - تحسين الأداء
LazyColumn {
    items(
        items = products,
        key = { it.productId }, // تحديد المفتاح الفريد
        contentType = { "ProductCard" }
    ) { product ->
        ProductCard(product)
    }
}
```

---

### 5. 🟡 **Database Queries بدون Pagination**

#### المشكلة:
```kotlin
// ❌ جلب جميع المنتجات دفعة واحدة
val allProducts = dao.getAllProducts() // قد يصل إلى آلاف الصفوف
```

#### الحل:
```kotlin
// ✅ استخدام Pagination
fun getProductsPaginated(page: Int, pageSize: Int = 20): Flow<List<Product>> {
    val offset = (page - 1) * pageSize
    return dao.getProductsPaginated(pageSize, offset)
}

// في UI:
var currentPage by remember { mutableIntStateOf(1) }
val products by remember(currentPage) { 
    viewModel.getProductsPaginated(currentPage)
}.collectAsState(initial = emptyList())
```

---

## 📦 **تحسينات حجم APK:**

### 1. **تفعيل ProGuard/R8:**

```gradle
android {
    buildTypes {
        release {
            isMinifyEnabled = true  // ✅ تفعيل
            isShrinkResources = true
            
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

### 2. **تقليل حجم الـ Assets:**

```kotlin
// ✅ في build.gradle.kts
android {
    packagingOptions {
        exclude "META-INF/proguard/androidx-*.pro"
        exclude "META-INF/*.kotlin_module"
        exclude "**/BuildConfig.class"
    }
}
```

### 3. **تقليل عدد Language Variants:**

```gradle
android {
    defaultConfig {
        // ✅ تحديد اللغات المدعومة
        resConfigs("en", "ar")
        
        // ✅ تقليل دعم ABIs
        ndk {
            abiFilters("arm64-v8a", "armeabi-v7a")
        }
    }
}
```

---

## ⚡ **تحسينات الأداء:**

### 1. **تحسين Compose Performance:**

```kotlin
// ✅ استخدام MutableInteractionSource بحذر
val interactionSource = remember { MutableInteractionSource() }

// ✅ استخدام stable classes
@Immutable
data class StableProduct(
    val id: String,
    val name: String,
    val price: Double
)

// ✅ استخدام remember للدوال الثقيلة
val filteredProducts = remember(searchQuery, products) {
    products.filter { 
        it.name.contains(searchQuery, ignoreCase = true)
    }
}
```

### 2. **تحسين Database Queries:**

```kotlin
// ❌ بطيء - Join في Kotlin
val result = products.map { product ->
    val sales = sales.filter { it.productId == product.id }
    product to sales
}

// ✅ سريع - Join في SQL
@Query("""
    SELECT p.*, COUNT(s.saleId) as saleCount
    FROM products p
    LEFT JOIN sales s ON p.productId = s.productId
    GROUP BY p.productId
""")
suspend fun getProductsWithSalesCount(): List<ProductWithSalesCount>
```

### 3. **تحسين Network Calls:**

```kotlin
// ✅ استخدام Retry Logic مع Exponential Backoff
suspend inline fun <reified T> retryWithBackoff(
    maxAttempts: Int = 3,
    initialDelay: Long = 100L,
    crossinline block: suspend () -> T
): T {
    var lastException: Exception? = null
    var currentDelay = initialDelay
    
    repeat(maxAttempts) {
        try {
            return block()
        } catch (e: Exception) {
            lastException = e
            delay(currentDelay)
            currentDelay *= 2
        }
    }
    
    throw lastException ?: Exception("Failed after $maxAttempts attempts")
}
```

---

## 🛡️ **فحص Runtime Errors:**

### 1. **Null Safety:**

```kotlin
// ❌ خطر
val user = AppRepository.currentUser
val email = user.email // قد يكون null!

// ✅ آمن
val email = AppRepository.currentUser?.email ?: "unknown@example.com"
```

### 2. **Exception Handling:**

```kotlin
// ✅ معالجة شاملة للأخطاء
try {
    val result = someNetworkCall()
    updateUI(result)
} catch (e: NetworkException) {
    showNetworkError()
} catch (e: SerializationException) {
    showParsingError()
} catch (e: Exception) {
    showGenericError(e)
}
```

### 3. **Configuration Changes:**

```kotlin
// ✅ استخدام rememberSaveable لحفظ الحالة
var searchQuery by rememberSaveable { mutableStateOf("") }

// ✅ يحتفظ بالبيانات بعد rotation
```

---

## 📋 **checklist نهائي:**

- [ ] تفعيل ProGuard/R8
- [ ] إضافة Keys إلى LazyColumn Items
- [ ] تطبيق Pagination للقوائم الطويلة
- [ ] إصلاح تسريب الـ Coroutines
- [ ] تحسين Database Queries
- [ ] إضافة Retry Logic للـ Network Calls
- [ ] تحسين State Management
- [ ] اختبار على أجهزة بطيئة
- [ ] قياس الأداء باستخدام Android Profiler

---

## 🔗 **الموارد:**

- [Jetpack Compose Performance](https://developer.android.com/develop/ui/compose/performance)
- [Android R8/ProGuard](https://developer.android.com/studio/build/shrink-code)
- [Kotlin Coroutines Best Practices](https://kotlinlang.org/docs/coroutines-basics.html)
- [Room Database Best Practices](https://developer.android.com/training/data-storage/room/relationships)
