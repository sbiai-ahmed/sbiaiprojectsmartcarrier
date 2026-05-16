package com.example.myapplication.database

import androidx.room.*
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "phone_devices")
data class PhoneDeviceEntity(
    @PrimaryKey val deviceId: String,
    val customerName: String,
    val customerPhone: String,
    val modelName: String,
    val imei: String,
    val issueDescription: String,
    val status: String,
    val estimatedCost: Double,
    val finalPrice: Double,
    val entryDate: Long,
    val warrantyUntil: Long?,
    val assignedTechnicianId: String,
    val isSynced: Boolean = false,
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val productId: String,
    val name: String,
    val category: String,
    val buyPrice: Double,
    val sellPrice: Double,
    val wholesalePrice: Double,
    val stockQuantity: Int,
    val minStockAlert: Int,
    val barcode: String,
    val isSynced: Boolean = false,
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val description: String,
    val category: String,
    val amount: Double,
    val date: Long,
    val isSynced: Boolean = false,
)

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey val saleId: String,
    val description: String,
    val amount: Double,
    val profit: Double,
    val soldAtPrice: Double, // تمت الإضافة لحماية الموظف
    val tvaAmount: Double,
    val date: Long,
    val processedBy: String,
    val customerId: String,
    val isSynced: Boolean = false,
)

@Entity(tableName = "purchases")
data class PurchaseEntity(
    @PrimaryKey val purchaseId: String,
    val itemName: String,
    val category: String,
    val quantity: Int,
    val unitCost: Double,
    val totalCost: Double,
    val supplierName: String,
    val date: Long,
    val isPaid: Boolean,
    val isSynced: Boolean = false,
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val username: String,
    val role: String,
    val email: String,
    val password: String,
    val pinCode: String,
    val isActive: Boolean,
    val isSynced: Boolean = false,
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey val logId: String,
    val userId: String,
    val userName: String,
    val action: String,
    val targetType: String,
    val targetId: String,
    val timestamp: Long,
    val details: String,
    val isSynced: Boolean = false,
)

@Entity(tableName = "expense_categories")
data class ExpenseCategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isSynced: Boolean = false,
)

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey val supplierId: String,
    val name: String,
    val phoneNumber: String,
    val totalDebt: Double,
    val isSynced: Boolean = false,
)

@Entity(tableName = "sections")
data class SectionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val userId: String,
    val iconName: String,
    val isSynced: Boolean = false,
)

@Dao
interface AppDao {
    // --- صيانة ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: PhoneDeviceEntity)
    @Query("SELECT * FROM phone_devices ORDER BY entryDate DESC")
    fun getAllDevices(): Flow<List<PhoneDeviceEntity>>
    @Query("SELECT * FROM phone_devices WHERE isSynced = 0")
    suspend fun getUnsyncedDevices(): List<PhoneDeviceEntity>
    @Query("UPDATE phone_devices SET isSynced = 1 WHERE deviceId = :id")
    suspend fun markDeviceSynced(id: String)
    @Query("UPDATE phone_devices SET status = :newStatus, isSynced = 0 WHERE deviceId = :id")
    suspend fun updateDeviceStatus(id: String, newStatus: String)
    @Query("DELETE FROM phone_devices WHERE deviceId = :id")
    suspend fun deleteDevice(id: String)
    @Query("UPDATE phone_devices SET isSynced = 0 WHERE deviceId = :id")
    suspend fun markDeviceUnsynced(id: String)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateDevice(device: PhoneDeviceEntity)

    // --- منتجات ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)
    @Query("SELECT * FROM products")
    fun getAllProducts(): Flow<List<ProductEntity>>
    @Query("SELECT * FROM products WHERE isSynced = 0")
    suspend fun getUnsyncedProducts(): List<ProductEntity>
    @Query("UPDATE products SET isSynced = 1 WHERE productId = :id")
    suspend fun markProductSynced(id: String)
    @Query("DELETE FROM products WHERE productId = :id")
    suspend fun deleteProduct(id: String)

    // --- مبيعات ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity)
    @Query("SELECT * FROM sales ORDER BY date DESC")
    fun getAllSales(): Flow<List<SaleEntity>>
    @Query("SELECT * FROM sales WHERE isSynced = 0")
    suspend fun getUnsyncedSales(): List<SaleEntity>
    @Query("UPDATE sales SET isSynced = 1 WHERE saleId = :id")
    suspend fun markSaleSynced(id: String)
    @Query("DELETE FROM sales WHERE saleId = :id")
    suspend fun deleteSale(id: String)

    // --- موردين ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: SupplierEntity)
    @Query("SELECT * FROM suppliers")
    fun getAllSuppliers(): Flow<List<SupplierEntity>>
    @Query("SELECT * FROM suppliers WHERE isSynced = 0")
    suspend fun getUnsyncedSuppliers(): List<SupplierEntity>
    @Query("UPDATE suppliers SET isSynced = 1 WHERE supplierId = :id")
    suspend fun markSupplierSynced(id: String)
    @Query("UPDATE suppliers SET totalDebt = totalDebt - :amount, isSynced = 0 WHERE supplierId = :supplierId")
    suspend fun updateSupplierDebt(supplierId: String, amount: Double)
    @Query("DELETE FROM suppliers WHERE supplierId = :id")
    suspend fun deleteSupplier(id: String)

    // --- مصاريف ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>
    @Query("SELECT * FROM expenses WHERE isSynced = 0")
    suspend fun getUnsyncedExpenses(): List<ExpenseEntity>
    @Query("UPDATE expenses SET isSynced = 1 WHERE id = :id")
    suspend fun markExpenseSynced(id: String)
    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpense(id: String)

    // --- مستخدمين ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>
    @Query("SELECT * FROM users WHERE isSynced = 0")
    suspend fun getUnsyncedUsers(): List<UserEntity>
    @Query("UPDATE users SET isSynced = 1 WHERE userId = :id")
    suspend fun markUserSynced(id: String)
    @Query("DELETE FROM users WHERE userId = :id")
    suspend fun deleteUser(id: String)
    @Query("SELECT * FROM users WHERE pinCode = :pin AND isActive = 1 LIMIT 1")
    suspend fun getUserByPin(pin: String): UserEntity?

    // --- أخرى ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: PurchaseEntity)
    @Query("SELECT * FROM purchases WHERE isSynced = 0")
    suspend fun getUnsyncedPurchases(): List<PurchaseEntity>
    @Query("UPDATE purchases SET isSynced = 1 WHERE purchaseId = :id")
    suspend fun markPurchaseSynced(id: String)
    @Query("SELECT * FROM purchases ORDER BY date DESC")
    fun getAllPurchases(): Flow<List<PurchaseEntity>>
    @Query("DELETE FROM purchases WHERE purchaseId = :id")
    suspend fun deletePurchase(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: ExpenseCategoryEntity)
    @Query("SELECT * FROM expense_categories WHERE isSynced = 0")
    suspend fun getUnsyncedCategories(): List<ExpenseCategoryEntity>
    @Query("UPDATE expense_categories SET isSynced = 1 WHERE id = :id")
    suspend fun markCategorySynced(id: String)
    @Query("SELECT * FROM expense_categories")
    fun getAllCategories(): Flow<List<ExpenseCategoryEntity>>
    @Query("DELETE FROM expense_categories WHERE id = :id")
    suspend fun deleteCategory(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)
    @Query("SELECT * FROM audit_logs WHERE isSynced = 0")
    suspend fun getUnsyncedAuditLogs(): List<AuditLogEntity>
    @Query("UPDATE audit_logs SET isSynced = 1 WHERE logId = :id")
    suspend fun markAuditLogSynced(id: String)
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 200")
    fun getAllAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSection(section: SectionEntity)
    @Query("SELECT * FROM sections")
    fun getAllSections(): Flow<List<SectionEntity>>
    @Query("SELECT * FROM sections WHERE isSynced = 0")
    suspend fun getUnsyncedSections(): List<SectionEntity>
    @Query("UPDATE sections SET isSynced = 1 WHERE id = :id")
    suspend fun markSectionSynced(id: String)
}

@Database(
    entities = [
        PhoneDeviceEntity::class, 
        ProductEntity::class, 
        ExpenseEntity::class,
        SaleEntity::class,
        PurchaseEntity::class,
        UserEntity::class,
        AuditLogEntity::class,
        ExpenseCategoryEntity::class,
        SupplierEntity::class,
        SectionEntity::class
    ], 
    version = 9 // تم رفع الإصدار لإضافة الحقول الجديدة
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao
}

fun getRoomDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase {
    return builder
        .fallbackToDestructiveMigration(dropAllTables = true)
        .setDriver(BundledSQLiteDriver())
        .build()
}
