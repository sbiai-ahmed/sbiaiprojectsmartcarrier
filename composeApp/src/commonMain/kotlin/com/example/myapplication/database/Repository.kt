package com.example.myapplication.database

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.example.myapplication.models.*
import com.example.myapplication.utils.PlatformUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.query.Columns

object AppRepository {
    private var database: AppDatabase? = null
    val dao get() = database?.dao()
    private val repositoryScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    var currentUser: User? = null

    val isAdmin: Boolean get() = currentUser?.role == UserRole.ADMIN
    val isEmployee: Boolean get() = (currentUser?.role == UserRole.EMPLOYEE) || (currentUser?.role == UserRole.ADMIN)

    // حالة التطبيق والتنبيهات
    val isLocked: MutableState<Boolean> = mutableStateOf(false)
    val errorFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)

    fun init(db: AppDatabase) {
        database = db
        repositoryScope.launch {
            try {
                // مزامنة أولية إذا كان هناك مستخدم مسجل
                if (SupabaseManager.client.auth.currentSessionOrNull() != null) {
                    pullDataFromCloud()
                }
                startPeriodicSync()
            } catch (e: Exception) {
                println("Sync Error: ${e.message}")
            }
        }
    }

    private fun startPeriodicSync() {
        repositoryScope.launch {
            while (true) {
                delay(60000)
                if (currentUser != null) {
                    pushDataToCloud()
                    pullDataFromCloud()
                }
            }
        }
    }

    suspend fun pushDataToCloud() {
        val client = SupabaseManager.client
        val d = dao ?: return
        try {
            d.getUnsyncedSales().forEach { client.postgrest["sales"].upsert(it.toModel()); d.markSaleSynced(it.saleId) }
            d.getUnsyncedDevices().forEach { client.postgrest["phone_devices"].upsert(it.toModel()); d.markDeviceSynced(it.deviceId) }
            d.getUnsyncedProducts().forEach { client.postgrest["products"].upsert(it.toModel()); d.markProductSynced(it.productId) }
            d.getUnsyncedExpenses().forEach { client.postgrest["expenses"].upsert(it.toModel()); d.markExpenseSynced(it.id) }
        } catch (_: Exception) {}
    }

    suspend fun pullDataFromCloud() {
        val client = SupabaseManager.client
        val shopId = currentUser?.shopId ?: return
        val tables = listOf("users", "products", "phone_devices", "sales", "expenses", "expense_categories", "suppliers")
        tables.forEach { tableName ->
            try {
                val results = client.postgrest[tableName].select {
                    filter { eq("shop_id", shopId) }
                }
                when (tableName) {
                    "users" -> results.decodeList<User>().forEach { dao?.insertUser(it.toEntity(true)) }
                    "phone_devices" -> results.decodeList<PhoneDevice>().forEach { dao?.insertDevice(it.toEntity(true)) }
                    "products" -> results.decodeList<Product>().forEach { dao?.insertProduct(it.toEntity(true)) }
                    "sales" -> results.decodeList<SaleRecord>().forEach { dao?.insertSale(it.toEntity(true)) }
                    "expenses" -> results.decodeList<Expense>().forEach { dao?.insertExpense(it.toEntity(true)) }
                    "expense_categories" -> results.decodeList<ExpenseCategory>().forEach { dao?.insertCategory(it.toEntity(true)) }
                    "suppliers" -> results.decodeList<Supplier>().forEach { dao?.insertSupplier(it.toEntity(true)) }
                }
            } catch (_: Exception) {}
        }
    }

    // --- Authentication & Registration ---

    suspend fun loginUser(email: String, pass: String): Boolean {
        return try {
            val client = SupabaseManager.client
            client.auth.signInWith(Email) {
                this.email = email
                this.password = pass
            }
            
            // جلب بيانات المستخدم من جدول users للحصول على shopId والـ role
            val userProfile = client.postgrest["users"].select {
                filter { eq("email", email) }
            }.decodeSingle<User>()
            
            currentUser = userProfile
            pullDataFromCloud()
            true
        } catch (e: Exception) {
            errorFlow.emit("فشل تسجيل الدخول: ${e.message}")
            false
        }
    }

    suspend fun registerNewManager(email: String, pass: String, name: String): Boolean {
        return try {
            val client = SupabaseManager.client
            val authUser = client.auth.signUpWith(Email) {
                this.email = email
                this.password = pass
            }
            
            val newUserId = authUser?.id ?: throw Exception("تعذر جلب معرّف المستخدم")
            val newShopId = "SHOP-${PlatformUtils.currentTimeMillis()}-${(100..999).random()}"
            
            val newManager = User(
                userId = newUserId,
                username = name,
                role = UserRole.ADMIN,
                email = email,
                password = pass,
                shopId = newShopId
            )
            
            client.postgrest["users"].insert(newManager)
            dao?.insertUser(newManager.toEntity(true))
            currentUser = newManager
            true
        } catch (e: Exception) {
            errorFlow.emit("خطأ أثناء إنشاء حساب المدير: ${e.message}")
            false
        }
    }

    suspend fun registerNewEmployee(email: String, pass: String, name: String): Boolean {
        return try {
            val client = SupabaseManager.client
            val currentShopId = currentUser?.shopId ?: throw Exception("يجب تسجيل الدخول كمدير أولاً")
            
            val authUser = client.auth.signUpWith(Email) {
                this.email = email
                this.password = pass
            }
            
            val newEmployeeId = authUser?.id ?: throw Exception("تعذر إنشاء حساب الموظف")
            
            val newEmployee = User(
                userId = newEmployeeId,
                username = name,
                role = UserRole.EMPLOYEE,
                email = email,
                password = pass,
                shopId = currentShopId
            )
            
            client.postgrest["users"].insert(newEmployee)
            dao?.insertUser(newEmployee.toEntity(true))
            true
        } catch (e: Exception) {
            errorFlow.emit("فشل إضافة الموظف: ${e.message}")
            false
        }
    }

    // --- صيانة ---
    suspend fun addDevice(device: PhoneDevice) {
        val deviceWithShop = device.copy(shopId = currentUser?.shopId ?: "")
        dao?.insertDevice(deviceWithShop.toEntity(false))
        try { SupabaseManager.client.postgrest["phone_devices"].upsert(deviceWithShop) } catch (_: Exception) {}
        addAuditLog("إضافة جهاز", "صيانة", device.deviceId, device.modelName)
    }

    suspend fun updateDeviceStatus(deviceId: String, newStatus: RepairStatus) {
        dao?.updateDeviceStatus(deviceId, newStatus.name)
        addAuditLog("تحديث حالة", "صيانة", deviceId, "إلى: ${newStatus.name}")
    }

    suspend fun updateDevice(device: PhoneDevice) {
        val deviceWithShop = device.copy(shopId = currentUser?.shopId ?: "")
        dao?.updateDevice(deviceWithShop.toEntity(false))
        try { SupabaseManager.client.postgrest["phone_devices"].upsert(deviceWithShop) } catch (_: Exception) {}
    }

    suspend fun deleteDevice(deviceId: String) {
        dao?.deleteDevice(deviceId)
        try { SupabaseManager.client.postgrest["phone_devices"].delete { filter { eq("deviceId", deviceId) } } } catch (_: Exception) {}
    }

    // --- منتجات ---
    suspend fun addProduct(product: Product) {
        val productWithShop = product.copy(shopId = currentUser?.shopId ?: "")
        dao?.insertProduct(productWithShop.toEntity(false))
        try { SupabaseManager.client.postgrest["products"].upsert(productWithShop) } catch (_: Exception) {}
        addAuditLog("إضافة منتج", "مخزن", product.productId, product.name)
    }

    suspend fun updateProduct(product: Product) {
        val productWithShop = product.copy(shopId = currentUser?.shopId ?: "")
        dao?.insertProduct(productWithShop.toEntity(false))
        try { SupabaseManager.client.postgrest["products"].upsert(productWithShop) } catch (_: Exception) {}
    }

    suspend fun deleteProduct(productId: String) {
        dao?.deleteProduct(productId)
        try { SupabaseManager.client.postgrest["products"].delete { filter { eq("productId", productId) } } } catch (_: Exception) {}
    }

    // --- مستخدمين ---
    suspend fun addUser(user: User) {
        val userWithShop = user.copy(shopId = currentUser?.shopId ?: "")
        dao?.insertUser(userWithShop.toEntity(false))
        try { SupabaseManager.client.postgrest["users"].upsert(userWithShop) } catch (_: Exception) {}
    }

    suspend fun deleteUser(userId: String) {
        dao?.deleteUser(userId)
        try { SupabaseManager.client.postgrest["users"].delete { filter { eq("userId", userId) } } } catch (_: Exception) {}
    }

    suspend fun updateUser(user: User) {
        val userWithShop = user.copy(shopId = currentUser?.shopId ?: "")
        dao?.insertUser(userWithShop.toEntity(false))
        try { SupabaseManager.client.postgrest["users"].upsert(userWithShop) } catch (_: Exception) {}
    }

    // --- مبيعات ---
    suspend fun makeSale(product: Product, quantity: Int, actualPrice: Double) {
        if (product.stockQuantity >= quantity) {
            val updatedProduct = product.copy(stockQuantity = product.stockQuantity - quantity)
            dao?.insertProduct(updatedProduct.toEntity(false))

            val sale = SaleRecord(
                saleId = "SALE-${PlatformUtils.currentTimeMillis()}",
                description = "بيع: ${product.name}",
                amount = actualPrice * quantity,
                profit = (actualPrice - product.buyPrice) * quantity,
                soldAtPrice = actualPrice,
                tvaAmount = (actualPrice * quantity) * 0.19,
                date = PlatformUtils.currentTimeMillis(),
                processedBy = currentUser?.username ?: "نظام",
                customerId = "",
                shopId = currentUser?.shopId ?: ""
            )
            dao?.insertSale(sale.toEntity(false))
            try { SupabaseManager.client.postgrest["sales"].upsert(sale) } catch (_: Exception) {}
            addAuditLog("بيع", "مبيعات", sale.saleId, sale.description)
        }
    }

    suspend fun updateSale(sale: SaleRecord) {
        val saleWithShop = sale.copy(shopId = currentUser?.shopId ?: "")
        dao?.insertSale(saleWithShop.toEntity(false))
        try { SupabaseManager.client.postgrest["sales"].upsert(saleWithShop) } catch (_: Exception) {}
    }

    suspend fun deleteSale(saleId: String) {
        dao?.deleteSale(saleId)
        try { SupabaseManager.client.postgrest["sales"].delete { filter { eq("saleId", saleId) } } } catch (_: Exception) {}
    }

    suspend fun restoreSale(sale: SaleRecord) {
        val saleWithShop = sale.copy(shopId = currentUser?.shopId ?: "")
        dao?.insertSale(saleWithShop.toEntity(false))
        try { SupabaseManager.client.postgrest["sales"].upsert(saleWithShop) } catch (_: Exception) {}
    }

    // --- مصاريف ---
    suspend fun addExpense(expense: Expense) {
        val expenseWithShop = expense.copy(shopId = currentUser?.shopId ?: "")
        dao?.insertExpense(expenseWithShop.toEntity(false))
        try { SupabaseManager.client.postgrest["expenses"].upsert(expenseWithShop) } catch (_: Exception) {}
        addAuditLog("مصروف", "مالية", expense.id, expense.description)
    }

    suspend fun deleteExpense(id: String) {
        dao?.deleteExpense(id)
        try { SupabaseManager.client.postgrest["expenses"].delete { filter { eq("id", id) } } } catch (_: Exception) {}
    }

    suspend fun addExpenseCategory(category: ExpenseCategory) {
        val categoryWithShop = category.copy(shopId = currentUser?.shopId ?: "")
        dao?.insertCategory(categoryWithShop.toEntity(false))
        try { SupabaseManager.client.postgrest["expense_categories"].upsert(categoryWithShop) } catch (_: Exception) {}
    }

    suspend fun deleteExpenseCategory(id: String) {
        dao?.deleteCategory(id)
        try { SupabaseManager.client.postgrest["expense_categories"].delete { filter { eq("id", id) } } } catch (_: Exception) {}
    }

    // --- مشتريات ---
    suspend fun addPurchase(purchase: PurchaseRecord) {
        val purchaseWithShop = purchase.copy(shopId = currentUser?.shopId ?: "")
        dao?.insertPurchase(purchaseWithShop.toEntity(false))
        try { SupabaseManager.client.postgrest["purchases"].upsert(purchaseWithShop) } catch (_: Exception) {}
        addAuditLog("مشتريات", "مالية", purchase.purchaseId, purchase.itemName)
    }

    suspend fun updatePurchase(purchase: PurchaseRecord) {
        val purchaseWithShop = purchase.copy(shopId = currentUser?.shopId ?: "")
        dao?.insertPurchase(purchaseWithShop.toEntity(false))
        try { SupabaseManager.client.postgrest["purchases"].upsert(purchaseWithShop) } catch (_: Exception) {}
    }

    suspend fun deletePurchase(purchaseId: String) {
        dao?.deletePurchase(purchaseId)
        try { SupabaseManager.client.postgrest["purchases"].delete { filter { eq("purchaseId", purchaseId) } } } catch (_: Exception) {}
    }

    // --- موردين ---
    suspend fun addSupplier(supplier: Supplier) {
        val supplierWithShop = supplier.copy(shopId = currentUser?.shopId ?: "")
        dao?.insertSupplier(supplierWithShop.toEntity(false))
        try { SupabaseManager.client.postgrest["suppliers"].upsert(supplierWithShop) } catch (_: Exception) {}
    }

    suspend fun updateSupplier(supplier: Supplier) {
        val supplierWithShop = supplier.copy(shopId = currentUser?.shopId ?: "")
        dao?.insertSupplier(supplierWithShop.toEntity(false))
        try { SupabaseManager.client.postgrest["suppliers"].upsert(supplierWithShop) } catch (_: Exception) {}
    }

    suspend fun deleteSupplier(id: String) {
        dao?.deleteSupplier(id)
        try { SupabaseManager.client.postgrest["suppliers"].delete { filter { eq("supplierId", id) } } } catch (_: Exception) {}
    }

    suspend fun addSupplierPayment(supplierId: String, amount: Double) {
        dao?.updateSupplierDebt(supplierId, amount)
    }

    // --- تفعيل ---
    suspend fun activateApp(code: String): Boolean {
        return if (code == "sbiai 2024 full") {
            isLocked.value = false
            true
        } else false
    }

    suspend fun addAuditLog(action: String, targetType: String, targetId: String, details: String) {
        val log = AuditLog(
            logId = "LOG-${PlatformUtils.currentTimeMillis()}",
            userId = currentUser?.userId ?: "SYSTEM",
            userName = currentUser?.username ?: "نظام",
            action = action,
            targetType = targetType,
            targetId = targetId,
            timestamp = PlatformUtils.currentTimeMillis(),
            details = details,
            shopId = currentUser?.shopId ?: ""
        )
        dao?.insertAuditLog(log.toEntity(false))
        try { SupabaseManager.client.postgrest["audit_logs"].upsert(log) } catch (_: Exception) {}
    }

    // Flows
    val devices: Flow<List<PhoneDevice>> get() = dao?.getAllDevices()?.map { it.map { e -> e.toModel() } } ?: flowOf(emptyList())
    val products: Flow<List<Product>> get() = dao?.getAllProducts()?.map { it.map { e -> e.toModel() } } ?: flowOf(emptyList())
    val sales: Flow<List<SaleRecord>> get() = dao?.getAllSales()?.map { it.map { e -> e.toModel() } } ?: flowOf(emptyList())
    val expenses: Flow<List<Expense>> get() = dao?.getAllExpenses()?.map { it.map { e -> e.toModel() } } ?: flowOf(emptyList())
    val users: Flow<List<User>> get() = dao?.getAllUsers()?.map { it.map { e -> e.toModel() } } ?: flowOf(emptyList())
    val auditLogs: Flow<List<AuditLog>> get() = dao?.getAllAuditLogs()?.map { it.map { e -> e.toModel() } } ?: flowOf(emptyList())
    val purchases: Flow<List<PurchaseRecord>> get() = dao?.getAllPurchases()?.map { it.map { e -> e.toModel() } } ?: flowOf(emptyList())
    val suppliers: Flow<List<Supplier>> get() = dao?.getAllSuppliers()?.map { it.map { e -> e.toModel() } } ?: flowOf(emptyList())
    val expenseCategories: Flow<List<ExpenseCategory>> get() = dao?.getAllCategories()?.map { it.map { e -> e.toModel() } } ?: flowOf(emptyList())
}

// --- Mappers ---
fun PhoneDeviceEntity.toModel() = PhoneDevice(deviceId, imei, modelName, customerPhone, issueDescription, RepairStatus.valueOf(status), estimatedCost, finalPrice, customerName, assignedTechnicianId, entryDate, warrantyUntil, shopId)
fun PhoneDevice.toEntity(synced: Boolean) = PhoneDeviceEntity(deviceId, customerName, customerPhone, modelName, imei, issueDescription, status.name, estimatedCost, finalPrice, entryDate, warrantyUntil, assignedTechnicianId, synced, shopId)

fun ProductEntity.toModel() = Product(productId, name, category, buyPrice, sellPrice, wholesalePrice, stockQuantity, minStockAlert, barcode, shopId)
fun Product.toEntity(synced: Boolean) = ProductEntity(productId, name, category, buyPrice, sellPrice, wholesalePrice, stockQuantity, minStockAlert, barcode, synced, shopId)

fun SaleEntity.toModel() = SaleRecord(saleId, description, amount, profit, soldAtPrice, tvaAmount, date, processedBy, customerId, shopId)
fun SaleRecord.toEntity(synced: Boolean) = SaleEntity(saleId, description, amount, profit, soldAtPrice, tvaAmount, date, processedBy, customerId, synced, shopId)

fun UserEntity.toModel() = User(userId, username, UserRole.valueOf(role), email, password, isActive, pinCode, shopId)
fun User.toEntity(synced: Boolean) = UserEntity(userId, username, role.name, email, password, pinCode, isActive, synced, shopId)

fun ExpenseEntity.toModel() = Expense(id, description, category, amount, date, shopId)
fun Expense.toEntity(synced: Boolean) = ExpenseEntity(id, description, category, amount, date, synced, shopId)

fun AuditLogEntity.toModel() = AuditLog(logId, userId, userName, action, targetType, targetId, timestamp, details, shopId)
fun AuditLog.toEntity(synced: Boolean) = AuditLogEntity(logId, userId, userName, action, targetType, targetId, timestamp, details, synced, shopId)

fun PurchaseEntity.toModel() = PurchaseRecord(purchaseId, itemName, category, quantity, unitCost, totalCost, supplierName, date, isPaid, shopId)
fun PurchaseRecord.toEntity(synced: Boolean) = PurchaseEntity(purchaseId, itemName, category, quantity, unitCost, totalCost, supplierName, date, isPaid, synced, shopId)

fun SupplierEntity.toModel() = Supplier(supplierId, name, phoneNumber, totalDebt, shopId)
fun Supplier.toEntity(synced: Boolean) = SupplierEntity(supplierId, name, phoneNumber, totalDebt, synced, shopId)

fun ExpenseCategoryEntity.toModel() = ExpenseCategory(id, name, shopId)
fun ExpenseCategory.toEntity(synced: Boolean) = ExpenseCategoryEntity(id, name, synced, shopId)

fun <T> emptyOf(): List<T> = emptyList()
