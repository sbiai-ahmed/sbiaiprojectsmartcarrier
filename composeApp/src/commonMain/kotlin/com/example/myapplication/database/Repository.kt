package com.example.myapplication.database

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.example.myapplication.models.*
import com.example.myapplication.utils.PlatformUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import io.github.jan.supabase.postgrest.postgrest

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
                pullDataFromCloud()
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
                pushDataToCloud()
                pullDataFromCloud()
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
        val d = dao ?: return
        val tables = listOf("users", "products", "phone_devices", "sales", "expenses", "expense_categories", "suppliers")
        tables.forEach { tableName ->
            try {
                when (tableName) {
                    "users" -> client.postgrest[tableName].select().decodeList<User>().forEach { d.insertUser(it.toEntity(true)) }
                    "phone_devices" -> client.postgrest[tableName].select().decodeList<PhoneDevice>().forEach { d.insertDevice(it.toEntity(true)) }
                    "products" -> client.postgrest[tableName].select().decodeList<Product>().forEach { d.insertProduct(it.toEntity(true)) }
                    "sales" -> client.postgrest[tableName].select().decodeList<SaleRecord>().forEach { d.insertSale(it.toEntity(true)) }
                    "expenses" -> client.postgrest[tableName].select().decodeList<Expense>().forEach { d.insertExpense(it.toEntity(true)) }
                    "expense_categories" -> client.postgrest[tableName].select().decodeList<ExpenseCategory>().forEach { d.insertCategory(it.toEntity(true)) }
                    "suppliers" -> client.postgrest[tableName].select().decodeList<Supplier>().forEach { d.insertSupplier(it.toEntity(true)) }
                }
            } catch (_: Exception) {}
        }
    }

    // --- صيانة ---
    suspend fun addDevice(device: PhoneDevice) {
        dao?.insertDevice(device.toEntity(false))
        try { SupabaseManager.client.postgrest["phone_devices"].upsert(device) } catch (_: Exception) {}
        addAuditLog("إضافة جهاز", "صيانة", device.deviceId, device.modelName)
    }

    suspend fun updateDeviceStatus(deviceId: String, newStatus: RepairStatus) {
        dao?.updateDeviceStatus(deviceId, newStatus.name)
        addAuditLog("تحديث حالة", "صيانة", deviceId, "إلى: ${newStatus.name}")
    }

    suspend fun updateDevice(device: PhoneDevice) {
        dao?.updateDevice(device.toEntity(false))
        try { SupabaseManager.client.postgrest["phone_devices"].upsert(device) } catch (_: Exception) {}
    }

    suspend fun deleteDevice(deviceId: String) {
        dao?.deleteDevice(deviceId)
        try { SupabaseManager.client.postgrest["phone_devices"].delete { filter { eq("deviceId", deviceId) } } } catch (_: Exception) {}
    }

    // --- منتجات ---
    suspend fun addProduct(product: Product) {
        dao?.insertProduct(product.toEntity(false))
        try { SupabaseManager.client.postgrest["products"].upsert(product) } catch (_: Exception) {}
        addAuditLog("إضافة منتج", "مخزن", product.productId, product.name)
    }

    suspend fun updateProduct(product: Product) {
        dao?.insertProduct(product.toEntity(false))
        try { SupabaseManager.client.postgrest["products"].upsert(product) } catch (_: Exception) {}
    }

    suspend fun deleteProduct(productId: String) {
        dao?.deleteProduct(productId)
        try { SupabaseManager.client.postgrest["products"].delete { filter { eq("productId", productId) } } } catch (_: Exception) {}
    }

    // --- مستخدمين ---
    suspend fun addUser(user: User) {
        dao?.insertUser(user.toEntity(false))
        try { SupabaseManager.client.postgrest["users"].upsert(user) } catch (_: Exception) {}
    }

    suspend fun deleteUser(userId: String) {
        dao?.deleteUser(userId)
        try { SupabaseManager.client.postgrest["users"].delete { filter { eq("userId", userId) } } } catch (_: Exception) {}
    }

    suspend fun updateUser(user: User) {
        dao?.insertUser(user.toEntity(false))
        try { SupabaseManager.client.postgrest["users"].upsert(user) } catch (_: Exception) {}
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
                customerId = ""
            )
            dao?.insertSale(sale.toEntity(false))
            try { SupabaseManager.client.postgrest["sales"].upsert(sale) } catch (_: Exception) {}
            addAuditLog("بيع", "مبيعات", sale.saleId, sale.description)
        }
    }

    suspend fun updateSale(sale: SaleRecord) {
        dao?.insertSale(sale.toEntity(false))
        try { SupabaseManager.client.postgrest["sales"].upsert(sale) } catch (_: Exception) {}
    }

    suspend fun deleteSale(saleId: String) {
        dao?.deleteSale(saleId)
        try { SupabaseManager.client.postgrest["sales"].delete { filter { eq("saleId", saleId) } } } catch (_: Exception) {}
    }

    suspend fun restoreSale(sale: SaleRecord) {
        dao?.insertSale(sale.toEntity(false))
        try { SupabaseManager.client.postgrest["sales"].upsert(sale) } catch (_: Exception) {}
    }

    // --- مصاريف ---
    suspend fun addExpense(expense: Expense) {
        dao?.insertExpense(expense.toEntity(false))
        try { SupabaseManager.client.postgrest["expenses"].upsert(expense) } catch (_: Exception) {}
        addAuditLog("مصروف", "مالية", expense.id, expense.description)
    }

    suspend fun deleteExpense(id: String) {
        dao?.deleteExpense(id)
        try { SupabaseManager.client.postgrest["expenses"].delete { filter { eq("id", id) } } } catch (_: Exception) {}
    }

    suspend fun addExpenseCategory(category: ExpenseCategory) {
        dao?.insertCategory(category.toEntity(false))
        try { SupabaseManager.client.postgrest["expense_categories"].upsert(category) } catch (_: Exception) {}
    }

    suspend fun deleteExpenseCategory(id: String) {
        dao?.deleteCategory(id)
        try { SupabaseManager.client.postgrest["expense_categories"].delete { filter { eq("id", id) } } } catch (_: Exception) {}
    }

    // --- مشتريات ---
    suspend fun addPurchase(purchase: PurchaseRecord) {
        dao?.insertPurchase(purchase.toEntity(false))
        try { SupabaseManager.client.postgrest["purchases"].upsert(purchase) } catch (_: Exception) {}
        addAuditLog("مشتريات", "مالية", purchase.purchaseId, purchase.itemName)
    }

    suspend fun updatePurchase(purchase: PurchaseRecord) {
        dao?.insertPurchase(purchase.toEntity(false))
        try { SupabaseManager.client.postgrest["purchases"].upsert(purchase) } catch (_: Exception) {}
    }

    suspend fun deletePurchase(purchaseId: String) {
        dao?.deletePurchase(purchaseId)
        try { SupabaseManager.client.postgrest["purchases"].delete { filter { eq("purchaseId", purchaseId) } } } catch (_: Exception) {}
    }

    // --- موردين ---
    suspend fun addSupplier(supplier: Supplier) {
        dao?.insertSupplier(supplier.toEntity(false))
        try { SupabaseManager.client.postgrest["suppliers"].upsert(supplier) } catch (_: Exception) {}
    }

    suspend fun updateSupplier(supplier: Supplier) {
        dao?.insertSupplier(supplier.toEntity(false))
        try { SupabaseManager.client.postgrest["suppliers"].upsert(supplier) } catch (_: Exception) {}
    }

    suspend fun deleteSupplier(id: String) {
        dao?.deleteSupplier(id)
        try { SupabaseManager.client.postgrest["suppliers"].delete { filter { eq("supplierId", id) } } } catch (_: Exception) {}
    }

    suspend fun addSupplierPayment(supplierId: String, amount: Double) {
        dao?.updateSupplierDebt(supplierId, amount)
        // يمكن أيضاً تحديث Supabase هنا إذا كان هناك حقل للدين
    }

    // --- تفعيل وولوج ---
    suspend fun activateApp(code: String): Boolean {
        return if (code == "sbiai 2024 full") {
            isLocked.value = false
            true
        } else false
    }

    suspend fun login(email: String, pass: String): User {
        val user = dao?.getAllUsers()?.first()?.find { it.email.equals(email, true) && it.password == pass }
        if (user != null) {
            currentUser = user.toModel()
            return currentUser!!
        } else {
            throw Exception("بيانات الدخول غير صحيحة")
        }
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
            details = details
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
    val suppliers: Flow<List<Supplier>> get() = dao?.getAllSuppliers()?.map { it.map { e -> e.toModel() } } ?: flowOf(emptyOf())
    val expenseCategories: Flow<List<ExpenseCategory>> get() = dao?.getAllCategories()?.map { it.map { e -> e.toModel() } } ?: flowOf(emptyList())
}

// --- Mappers ---
fun PhoneDeviceEntity.toModel() = PhoneDevice(deviceId, imei, modelName, customerPhone, issueDescription, RepairStatus.valueOf(status), estimatedCost, finalPrice, customerName, assignedTechnicianId, entryDate, warrantyUntil)
fun PhoneDevice.toEntity(synced: Boolean) = PhoneDeviceEntity(deviceId, customerName, customerPhone, modelName, imei, issueDescription, status.name, estimatedCost, finalPrice, entryDate, warrantyUntil, assignedTechnicianId, synced)

fun ProductEntity.toModel() = Product(productId, name, category, buyPrice, sellPrice, wholesalePrice, stockQuantity, minStockAlert, barcode)
fun Product.toEntity(synced: Boolean) = ProductEntity(productId, name, category, buyPrice, sellPrice, wholesalePrice, stockQuantity, minStockAlert, barcode, synced)

fun SaleEntity.toModel() = SaleRecord(saleId, description, amount, profit, soldAtPrice, tvaAmount, date, processedBy, customerId)
fun SaleRecord.toEntity(synced: Boolean) = SaleEntity(saleId, description, amount, profit, soldAtPrice, tvaAmount, date, processedBy, customerId, synced)

fun UserEntity.toModel() = User(userId, username, UserRole.valueOf(role), email, password, isActive, pinCode)
fun User.toEntity(synced: Boolean) = UserEntity(userId, username, role.name, email, password, pinCode, isActive, synced)

fun ExpenseEntity.toModel() = Expense(id, description, category, amount, date)
fun Expense.toEntity(synced: Boolean) = ExpenseEntity(id, description, category, amount, date, synced)

fun AuditLogEntity.toModel() = AuditLog(logId, userId, userName, action, targetType, targetId, timestamp, details)
fun AuditLog.toEntity(synced: Boolean) = AuditLogEntity(logId, userId, userName, action, targetType, targetId, timestamp, details, synced)

fun PurchaseEntity.toModel() = PurchaseRecord(purchaseId, itemName, category, quantity, unitCost, totalCost, supplierName, date, isPaid)
fun PurchaseRecord.toEntity(synced: Boolean) = PurchaseEntity(purchaseId, itemName, category, quantity, unitCost, totalCost, supplierName, date, isPaid, synced)

fun SupplierEntity.toModel() = Supplier(supplierId, name, phoneNumber, totalDebt)
fun Supplier.toEntity(synced: Boolean) = SupplierEntity(supplierId, name, phoneNumber, totalDebt, synced)

fun ExpenseCategoryEntity.toModel() = ExpenseCategory(id, name)
fun ExpenseCategory.toEntity(synced: Boolean) = ExpenseCategoryEntity(id, name, synced)

fun <T> emptyOf(): List<T> = emptyList()
