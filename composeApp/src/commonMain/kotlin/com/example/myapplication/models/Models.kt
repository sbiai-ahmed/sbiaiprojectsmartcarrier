package com.example.myapplication.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
enum class UserRole { ADMIN, EMPLOYEE }

@Serializable
enum class RepairStatus {
    @SerialName("PENDING") PENDING,
    @SerialName("REPAIRING") REPAIRING,
    @SerialName("COMPLETED") COMPLETED,
    @SerialName("DELIVERED") DELIVERED,
    @SerialName("DELAYED") DELAYED, // تمت الإضافة بناءً على طلبك
    @SerialName("CANCELLED") CANCELLED
}

fun RepairStatus.toArabic(): String = when (this) {
    RepairStatus.PENDING -> "في الانتظار"
    RepairStatus.REPAIRING -> "قيد الصيانة"
    RepairStatus.COMPLETED -> "جاهز للاستلام"
    RepairStatus.DELIVERED -> "تم التسليم"
    RepairStatus.DELAYED -> "متأخر"
    RepairStatus.CANCELLED -> "ملغي"
}

@Serializable
data class User(
    @SerialName("userId") val userId: String,
    @SerialName("username") val username: String,
    @SerialName("role") val role: UserRole,
    @SerialName("email") val email: String,
    @SerialName("password") val password: String,
    @SerialName("isActive") val isActive: Boolean = true,
    @SerialName("pinCode") val pinCode: String = ""
)

@Serializable
data class PhoneDevice(
    @SerialName("deviceId") val deviceId: String,
    @SerialName("imei") val imei: String,
    @SerialName("modelName") val modelName: String,
    @SerialName("customerPhone") val customerPhone: String = "",
    @SerialName("issueDescription") val issueDescription: String,
    @SerialName("status") val status: RepairStatus,
    @SerialName("estimatedCost") val estimatedCost: Double,
    @SerialName("finalPrice") val finalPrice: Double = 0.0,
    @SerialName("customerName") val customerName: String,
    @SerialName("assignedTechnicianId") val assignedTechnicianId: String = "",
    @SerialName("entryDate") val entryDate: Long = 0L,
    @SerialName("warrantyUntil") val warrantyUntil: Long? = null
)

@Serializable
data class Product(
    @SerialName("productId") val productId: String,
    @SerialName("name") val name: String,
    @SerialName("category") val category: String,
    @SerialName("buyPrice") val buyPrice: Double,
    @SerialName("sellPrice") val sellPrice: Double,
    @SerialName("wholesalePrice") val wholesalePrice: Double = 0.0,
    @SerialName("stockQuantity") val stockQuantity: Int,
    @SerialName("minStockAlert") val minStockAlert: Int = 2,
    @SerialName("barcode") val barcode: String = ""
)

@Serializable
data class SaleRecord(
    @SerialName("saleId") val saleId: String,
    @SerialName("description") val description: String,
    @SerialName("amount") val amount: Double,
    @SerialName("profit") val profit: Double,
    @SerialName("soldAtPrice") val soldAtPrice: Double, // تمت الإضافة لحماية الموظف
    @SerialName("tvaAmount") val tvaAmount: Double = 0.0,
    @SerialName("date") val date: Long,
    @SerialName("processedBy") val processedBy: String,
    @SerialName("customerId") val customerId: String = ""
)

@Serializable
data class Expense(
    @SerialName("id") val id: String,
    @SerialName("description") val description: String,
    @SerialName("category") val category: String,
    @SerialName("amount") val amount: Double,
    @SerialName("date") val date: Long
)

@Serializable
data class ExpenseCategory(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String
)

@Serializable
data class AuditLog(
    @SerialName("logId") val logId: String,
    @SerialName("userId") val userId: String,
    @SerialName("userName") val userName: String,
    @SerialName("action") val action: String,
    @SerialName("targetType") val targetType: String,
    @SerialName("targetId") val targetId: String,
    @SerialName("timestamp") val timestamp: Long,
    @SerialName("details") val details: String
)

@Serializable
data class Supplier(
    @SerialName("supplierId") val supplierId: String,
    @SerialName("name") val name: String,
    @SerialName("phoneNumber") val phoneNumber: String,
    @SerialName("totalDebt") val totalDebt: Double
)

@Serializable
data class PurchaseRecord(
    @SerialName("purchaseId") val purchaseId: String,
    @SerialName("itemName") val itemName: String,
    @SerialName("category") val category: String,
    @SerialName("quantity") val quantity: Int,
    @SerialName("unitCost") val unitCost: Double,
    @SerialName("totalCost") val totalCost: Double,
    @SerialName("supplierName") val supplierName: String,
    @SerialName("date") val date: Long,
    @SerialName("isPaid") val isPaid: Boolean
)

@Serializable
data class Section(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("userId") val userId: String,
    @SerialName("iconName") val iconName: String
)

@Serializable
data class AppActivation(
    @SerialName("device_id") val deviceId: String,
    @SerialName("install_date") val installDate: Long,
    @SerialName("is_activated") val isActivated: Boolean
)
