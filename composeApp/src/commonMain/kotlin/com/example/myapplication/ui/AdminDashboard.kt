package com.example.myapplication.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.database.AppRepository
import com.example.myapplication.ui.components.*
import com.example.myapplication.viewmodel.DashboardViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboard(
    onLogout: () -> Unit, 
    onNavigateToUsers: () -> Unit, 
    onNavigateToReports: () -> Unit, 
    onNavigateToExpenses: () -> Unit, 
    onNavigateToInventory: () -> Unit, 
    onNavigateToPurchases: () -> Unit,
    onNavigateToSuppliers: () -> Unit,
    onNavigateToSales: () -> Unit,
    viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory),
) {
    val dashboardData by viewModel.dashboardState.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()
    val state = rememberLazyListState()
    var showProfileDialog by remember { mutableStateOf(false) }
    var showProfitDetailDialog by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("لوحة تحكم المسؤول", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { showProfileDialog = true }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "الملف الشخصي")
                    }
                },
                actions = {
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
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                state = state,
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
            ) {
                item {
                    // بطاقة معلومات المحل (shopId)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("كود المحل الخاص بك (لربط الموظفين):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                Text(
                                    text = AppRepository.currentUser?.shopId ?: "غير متوفر",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    letterSpacing = 2.sp
                                )
                            }
                            IconButton(onClick = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("قم بنسخ الكود يدوياً ومشاركته")
                                }
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", tint = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }

                item {
                    Text("إحصائيات اليوم", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            title = "صافي الأرباح",
                            value = "${dashboardData.netProfit.toInt()} DA",
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            color = if (dashboardData.netProfit >= 0) Color(0xFF4CAF50) else Color.Red,
                            modifier = Modifier.weight(1f),
                            onClick = { showProfitDetailDialog = true }
                        )
                        StatCard(
                            title = "قيد الصيانة",
                            value = dashboardData.repairingCount.toString(),
                            icon = Icons.Default.Build,
                            color = Color(0xFFFF9800),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (dashboardData.lowStockCount > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("تنبيه المخزن", fontWeight = FontWeight.Bold, color = Color.Red, fontSize = 14.sp)
                                    Text("يوجد ${dashboardData.lowStockCount} منتجات أوشكت على النفاذ!", fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                TextButton(onClick = onNavigateToInventory) {
                                    Text("عرض", color = Color.Red, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onNavigateToUsers,
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.People, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("الموظفين", fontSize = 12.sp)
                            }

                            Button(
                                onClick = onNavigateToReports,
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                            ) {
                                Icon(Icons.Default.Assessment, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("التقارير", fontSize = 12.sp)
                            }
                        }

                        Button(
                            onClick = onNavigateToSales,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Icon(Icons.Default.Monitor, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("مراقبة المبيعات الحية (Live)")
                        }

                        Button(
                            onClick = onNavigateToExpenses,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
                        ) {
                            Icon(Icons.Default.Payments, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("إدارة المصاريف (كراء، كهرباء...)")
                        }

                        Button(
                            onClick = onNavigateToInventory,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7))
                        ) {
                            Icon(Icons.Default.Inventory, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("إدارة المخزن والسلع")
                        }

                        Button(
                            onClick = onNavigateToPurchases,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009688))
                        ) {
                            Icon(Icons.Default.ShoppingBag, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("سجل المبيعات (Stock In)")
                        }

                        Button(
                            onClick = onNavigateToSuppliers,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
                        ) {
                            Icon(Icons.Default.RequestQuote, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("كشف حساب الموردين (الديون)")
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("التحليل السريع (الأسبوع والمصاريف)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth().height(160.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    SimpleBarChart(dashboardData.weeklyRevenue)
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    if (dashboardData.expenseDistribution.isNotEmpty()) {
                                        PieChart(dashboardData.expenseDistribution)
                                    } else {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("لا توجد مصاريف حالية", fontSize = 10.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text("آخر العمليات", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                items(auditLogs) { log ->
                    RecentActivityItem(log)
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "SBIAI AHMED",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Phone,
                                null,
                                modifier = Modifier.size(10.dp),
                                tint = Color.Gray.copy(alpha = 0.5f)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "0667186465",
                                fontSize = 10.sp,
                                color = Color.Gray.copy(alpha = 0.5f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            if (showProfileDialog) {
                AdminProfileDialog(
                    user = AppRepository.currentUser,
                    onDismiss = { showProfileDialog = false },
                    onSave = { updatedUser ->
                        viewModel.updateProfile(updatedUser) { success ->
                            if (success) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("تم تحديث البيانات بنجاح")
                                }
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("فشل التحديث، تأكد من الاتصال")
                                }
                            }
                        }
                        showProfileDialog = false
                    }
                )
            }

            if (showProfitDetailDialog) {
                ProfitDetailDialog(
                    totalRevenue = dashboardData.totalRevenue,
                    totalExpenses = dashboardData.totalExpenses,
                    netProfit = dashboardData.netProfit,
                    onDismiss = { showProfitDetailDialog = false }
                )
            }

            AppScrollbar(
                state = state,
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
            )
        }
    }
}

@Composable
fun ProfitDetailDialog(totalRevenue: Double, totalExpenses: Double, netProfit: Double, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تفاصيل الأرباح") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("إجمالي الإيرادات:")
                    Text("${totalRevenue.toInt()} DA", fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("إجمالي المصاريف:")
                    Text("- ${totalExpenses.toInt()} DA", color = Color.Red)
                }
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("صافي الربح:", fontWeight = FontWeight.Bold)
                    Text("${netProfit.toInt()} DA", fontWeight = FontWeight.ExtraBold, color = if (netProfit >= 0) Color(0xFF2E7D32) else Color.Red)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } }
    )
}

@Composable
fun StatCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier, onClick: (() -> Unit)? = null) {
    Card(
        modifier = modifier.then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = color)
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontSize = 12.sp, color = Color.Gray)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SimpleBarChart(weeklyRevenue: List<Double>) {
    val maxRevenue = weeklyRevenue.maxOrNull()?.takeIf { it > 0 } ?: 1.0
    Row(
        modifier = Modifier.fillMaxWidth().height(140.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        if (weeklyRevenue.isEmpty()) {
            repeat(7) {
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .height(10.dp)
                        .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                )
            }
        } else {
            weeklyRevenue.forEach { revenue ->
                val heightFactor = (revenue / maxRevenue).toFloat().coerceIn(0.1f, 1f)
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .fillMaxHeight(heightFactor)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                )
            }
        }
    }
}

@Composable
fun RecentActivityItem(log: com.example.myapplication.models.AuditLog) {
    val timeText = remember(log.timestamp) {
        com.example.myapplication.utils.PlatformUtils.formatDate(log.timestamp)
    }
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(8.dp),
            color = when(log.targetType) {
                "مالية" -> Color(0xFFE91E63).copy(alpha = 0.1f)
                "مبيعات" -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                "صيانة" -> Color(0xFF2196F3).copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.primaryContainer
            }
        ) {
            val icon = when(log.targetType) {
                "مالية" -> Icons.Default.Payments
                "مبيعات" -> Icons.Default.ShoppingCart
                "صيانة" -> Icons.Default.Build
                else -> Icons.Default.Info
            }
            Icon(icon, contentDescription = null, modifier = Modifier.padding(8.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(log.action, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(log.details, fontSize = 12.sp, color = Color.Gray, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Email, null, modifier = Modifier.size(10.dp), tint = Color.Gray)
                Spacer(Modifier.width(4.dp))
                Text(log.userId, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Icon(Icons.Default.Schedule, null, modifier = Modifier.size(10.dp), tint = Color.Gray)
                Spacer(Modifier.width(4.dp))
                Text(timeText, fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun AdminProfileDialog(
    user: com.example.myapplication.models.User?,
    onDismiss: () -> Unit,
    onSave: (com.example.myapplication.models.User) -> Unit
) {
    var username by remember { mutableStateOf(user?.username ?: "") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    var password by remember { mutableStateOf(user?.password ?: "") }

    val emailRegex = remember { Regex("^[A-Za-z0-9+_.-]+@([A-Za-z0-9-]+\\.)+[A-Za-z]{2,}$") }
    val isEmailError = email.trim().isNotEmpty() && !emailRegex.matches(email.trim())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إعدادات الملف الشخصي") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("اسم المستخدم") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = email, 
                    onValueChange = { email = it }, 
                    label = { Text("البريد الإلكتروني") }, 
                    modifier = Modifier.fillMaxWidth(),
                    isError = isEmailError,
                    supportingText = {
                        if (isEmailError) Text("تنسيق البريد الإلكتروني غير صحيح", color = Color.Red)
                    }
                )
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("كلمة المرور") }, modifier = Modifier.fillMaxWidth(), visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    user?.let {
                        onSave(it.copy(username = username, email = email, password = password))
                    }
                },
                enabled = username.isNotBlank() && email.isNotBlank() && !isEmailError
            ) { Text("حفظ التغييرات") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}
