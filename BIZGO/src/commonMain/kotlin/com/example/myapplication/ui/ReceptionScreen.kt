package com.example.myapplication.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.database.AppRepository
import com.example.myapplication.models.PhoneDevice
import com.example.myapplication.models.RepairStatus
import com.example.myapplication.ui.components.BarcodeScannerView
import com.example.myapplication.ui.components.AppScrollbar
import com.example.myapplication.utils.PlatformUtils
import com.example.myapplication.utils.InvoiceGenerator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceptionScreen(
    onLogout: () -> Unit, 
    onNavigateToSearch: () -> Unit, 
    onNavigateToList: () -> Unit, 
    onNavigateToSales: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var customerName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var phoneModel by remember { mutableStateOf("") }
    var imei by remember { mutableStateOf("") }
    var issueDescription by remember { mutableStateOf("") }
    var estimatedCost by remember { mutableStateOf("") }
    
    var isScanningImei by remember { mutableStateOf(false) }

    // توليد رقم تسلسلي تلقائي
    val orderId by remember { 
        mutableStateOf("BON-${(0..999999).random().toString().padStart(6, '0')}") 
    }

    if (isScanningImei) {
        BarcodeScannerView(
            modifier = Modifier.fillMaxSize(),
            onBarcodeScanned = { result ->
                imei = result
                isScanningImei = false
            },
            onClose = { 
                isScanningImei = false 
            }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Column {
                            Text("استقبال جهاز جديد", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("رقم الطلب: $orderId", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToSales) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "مبيعات")
                        }
                        IconButton(onClick = onNavigateToList) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "قائمة الأجهزة")
                        }
                        IconButton(onClick = onNavigateToSearch) {
                            Icon(Icons.Default.Search, contentDescription = "بحث")
                        }
                        IconButton(onClick = onLogout) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "تسجيل الخروج")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            }
        ) { padding ->
            val scrollState = rememberScrollState()
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // بطاقة عرض رقم الطلب بشكل بارز
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("رقم التذكرة (Serial Number)", fontSize = 14.sp)
                                Text(orderId, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            }
                            Row {
                                IconButton(onClick = { 
                                    if (customerName.isNotBlank() && phoneModel.isNotBlank()) {
                                        val currentDevice = PhoneDevice(
                                            deviceId = orderId,
                                            imei = imei,
                                            modelName = phoneModel,
                                            customerPhone = phoneNumber,
                                            issueDescription = issueDescription,
                                            status = RepairStatus.PENDING,
                                            estimatedCost = estimatedCost.toDoubleOrNull() ?: 0.0,
                                            customerName = customerName,
                                            assignedTechnicianId = "Employee 1",
                                            entryDate = PlatformUtils.currentTimeMillis()
                                        )
                                        val htmlContent = InvoiceGenerator.generateDeviceReceipt(currentDevice)
                                        PlatformUtils.printHtml(htmlContent, "receipt_${orderId}")
                                        
                                        scope.launch {
                                            AppRepository.addAuditLog("طباعة وصل استلام", "جهاز صيانة", orderId, "تمت طباعة وصل للزبون: $customerName")
                                        }
                                    }
                                }) {
                                    Icon(Icons.Default.Print, contentDescription = "طباعة", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                                }
                                Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Text(
                        text = "الأقسام السريعة",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start).padding(bottom = 12.dp)
                    )

                    // زر قسم المبيعات الجديد (بارز للمشرف)
                    Button(
                        onClick = onNavigateToSales,
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(30.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("قسم المبيعات (POS)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("تسجيل بيع سلع، إكسسوارات، أو قطع غيار", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "بيانات الزبون والجهاز",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start).padding(bottom = 20.dp)
                    )

                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("اسم الزبون الكامل") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("رقم الهاتف") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = imei,
                        onValueChange = { imei = it },
                        label = { Text("رقم IMEI") },
                        leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { isScanningImei = true }) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = "مسح IMEI")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = phoneModel,
                        onValueChange = { phoneModel = it },
                        label = { Text("نوع الهاتف (مثال: iPhone 13)") },
                        leadingIcon = { Icon(Icons.Default.Smartphone, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = issueDescription,
                        onValueChange = { issueDescription = it },
                        label = { Text("وصف المشكلة / العطل") },
                        leadingIcon = { Icon(Icons.Default.Build, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = estimatedCost,
                        onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*$"))) estimatedCost = it },
                        label = { Text("التكلفة التقديرية (DA)") },
                        leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { 
                            if (customerName.isNotBlank() && phoneModel.isNotBlank()) {
                                scope.launch {
                                    val newDevice = PhoneDevice(
                                        deviceId = orderId,
                                        imei = imei,
                                        modelName = phoneModel,
                                        customerPhone = phoneNumber,
                                        issueDescription = issueDescription,
                                        status = RepairStatus.PENDING,
                                        estimatedCost = estimatedCost.toDoubleOrNull() ?: 0.0,
                                        customerName = customerName,
                                        assignedTechnicianId = "Employee 1",
                                        entryDate = PlatformUtils.currentTimeMillis()
                                    )
                                    AppRepository.addDevice(newDevice)
                                    customerName = ""
                                    phoneNumber = ""
                                    phoneModel = ""
                                    imei = ""
                                    issueDescription = ""
                                    estimatedCost = ""
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("حفظ البيانات وتوليد رقم الطلب", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                AppScrollbar(
                    state = scrollState,
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                )
            }
        }
    }
}
