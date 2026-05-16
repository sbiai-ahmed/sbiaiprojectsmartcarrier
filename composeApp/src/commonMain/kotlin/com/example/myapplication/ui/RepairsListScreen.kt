package com.example.myapplication.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.database.AppRepository
import com.example.myapplication.models.PhoneDevice
import com.example.myapplication.models.RepairStatus
import com.example.myapplication.models.toArabic
import com.example.myapplication.ui.components.AppScrollbar
import com.example.myapplication.utils.PlatformUtils
import com.example.myapplication.viewmodel.RepairsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepairsListScreen(
    onBack: () -> Unit,
    onEditDevice: (String) -> Unit,
    viewModel: RepairsViewModel = viewModel(factory = RepairsViewModel.Factory),
) {
    val devicesState = viewModel.devices.collectAsState()
    val devices = devicesState.value
    val error by viewModel.errorMessage.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("الكل") }
    val state = rememberLazyListState()
    val currentTime = remember { PlatformUtils.currentTimeMillis() }
    val fifteenDaysInMs = 15L * 24 * 60 * 60 * 1000L
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val filteredDevices = remember(searchQuery, devices, selectedFilter) {
        devices.filter { device ->
            val matchesSearch = device.deviceId.contains(searchQuery, ignoreCase = true) || 
                               device.customerName.contains(searchQuery, ignoreCase = true) ||
                               device.modelName.contains(searchQuery, ignoreCase = true)
            
            val matchesFilter = when (selectedFilter) {
                "الكل" -> true
                "قيد الصيانة" -> device.status == RepairStatus.REPAIRING
                "جاهز" -> device.status == RepairStatus.COMPLETED
                "متأخر" -> device.status == RepairStatus.DELAYED || (currentTime - device.entryDate > fifteenDaysInMs && device.status != RepairStatus.DELIVERED)
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("قائمة الإصلاحات", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                QuickStatsRow(devices, currentTime, fifteenDaysInMs)

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث في القائمة...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                FilterChipsRow(selectedFilter) { selectedFilter = it }

                if (filteredDevices.isEmpty()) {
                    EmptyRepairsState(searchQuery.isNotEmpty())
                } else {
                    LazyColumn(
                        state = state,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(filteredDevices, key = { it.deviceId }) { device ->
                            Box(modifier = Modifier.animateItem()) {
                                RepairItem(
                                    device = device,
                                    currentTime = currentTime,
                                    fifteenDaysInMs = fifteenDaysInMs,
                                    onEdit = { onEditDevice(device.deviceId) },
                                    onDelete = {
                                        viewModel.deleteDevice(device.deviceId) { deleted ->
                                            scope.launch {
                                                val result = snackbarHostState.showSnackbar(
                                                    message = "تم حذف ${deleted.modelName}",
                                                    actionLabel = "تراجع",
                                                    duration = SnackbarDuration.Short
                                                )
                                                if (result == SnackbarResult.ActionPerformed) {
                                                    viewModel.restoreDevice(deleted)
                                                }
                                            }
                                        }
                                    },
                                    onStatusChange = { newStatus ->
                                        viewModel.updateStatus(device.deviceId, newStatus)
                                    }
                                )
                            }
                        }
                    }
                }
            }
            AppScrollbar(
                state = state,
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
            )
        }
    }
}

@Composable
fun QuickStatsRow(devices: List<PhoneDevice>, currentTime: Long, fifteenDaysInMs: Long) {
    val repairingCount = devices.count { it.status == RepairStatus.REPAIRING }
    val delayedCount = devices.count { 
        (it.status == RepairStatus.DELAYED) || (it.status != RepairStatus.DELIVERED && (currentTime - it.entryDate) > fifteenDaysInMs) 
    }
    val expectedRevenue = devices.asSequence().filter { it.status != RepairStatus.CANCELLED }.sumOf { it.estimatedCost }

    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MiniStatCard("قيد الصيانة", repairingCount.toString(), Icons.Default.Build, Color(0xFF2196F3), Modifier.weight(1f))
        MiniStatCard("متأخر", delayedCount.toString(), Icons.Default.Warning, Color(0xFFF44336), Modifier.weight(1f))
        MiniStatCard("الإيرادات", "${expectedRevenue.toInt()} DA", Icons.Default.Payments, Color(0xFF4CAF50), Modifier.weight(1.2f))
    }
}

@Composable
fun MiniStatCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Text(title, fontSize = 10.sp, color = Color.Gray)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun FilterChipsRow(selected: String, onSelect: (String) -> Unit) {
    val filters = listOf("الكل", "قيد الصيانة", "جاهز", "متأخر")
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelect(filter) },
                label = { Text(filter) },
                leadingIcon = if (selected == filter) {
                    { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                } else null,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
fun EmptyRepairsState(isSearch: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                if (isSearch) Icons.Default.SearchOff else Icons.Default.Inventory,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.LightGray
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                if (isSearch) "لا توجد أجهزة مطابقة للبحث" else "قائمة الإصلاحات فارغة",
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun RepairItem(
    device: PhoneDevice, 
    currentTime: Long, 
    fifteenDaysInMs: Long,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStatusChange: (RepairStatus) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val isAdmin = AppRepository.isAdmin
    val isEmployee = AppRepository.isEmployee
    
    val isOverdue = (device.status != RepairStatus.DELIVERED) && 
                   (currentTime - device.entryDate) > fifteenDaysInMs

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOverdue || device.status == RepairStatus.DELAYED) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surface
        ),
        border = if (isOverdue || device.status == RepairStatus.DELAYED) BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)) else BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (isOverdue || device.status == RepairStatus.DELAYED) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تنبيه: هذا الجهاز متأخر (أكثر من 15 يوم)!", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Smartphone, null, Modifier.padding(8.dp), MaterialTheme.colorScheme.primary)
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(device.deviceId, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(device.modelName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (device.status == RepairStatus.COMPLETED) {
                        IconButton(
                            onClick = {
                                val message = "مرحباً ${device.customerName}، جهازك ${device.modelName} جاهز للاستلام في محلنا."
                                val cleanMsg = message.replace(" ", "%20")
                                val url = "https://wa.me/?text=$cleanMsg"
                                try { uriHandler.openUri(url) } catch (_: Exception) {}
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "WhatsApp", tint = Color(0xFF25D366))
                        }
                    }

                    // أزرار التعديل والحذف - تعطيل إذا كان Admin للمشاهدة فقط حسب الطلب
                    if (isEmployee && !isAdmin) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    
                    if (isAdmin) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red.copy(alpha = 0.7f))
                        }
                    }

                    Box {
                        // الموظف فقط من يغير الحالة، المدير للمشاهدة
                        StatusBadge(
                            status = device.status, 
                            onClick = { if (isEmployee && !isAdmin) showMenu = true }
                        )
                        if (isEmployee && !isAdmin) {
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                RepairStatus.entries.forEach { status ->
                                    DropdownMenuItem(
                                        text = { Text(status.toArabic()) },
                                        onClick = { onStatusChange(status); showMenu = false }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("الزبون", fontSize = 11.sp, color = Color.Gray)
                    Text(device.customerName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                
                device.warrantyUntil?.let {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("الضمان", fontSize = 11.sp, color = Color.Gray)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VerifiedUser, null, Modifier.size(14.dp), Color(0xFF4CAF50))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("فعال", fontSize = 12.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("التكلفة", fontSize = 11.sp, color = Color.Gray)
                    Text("${device.estimatedCost.toInt()} DA", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: RepairStatus, onClick: () -> Unit) {
    Surface(
        color = status.toColor().copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = status.toArabic(),
            color = status.toColor(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

fun RepairStatus.toColor(): Color = when (this) {
    RepairStatus.PENDING -> Color(0xFFFF9800)
    RepairStatus.REPAIRING -> Color(0xFF2196F3)
    RepairStatus.COMPLETED -> Color(0xFF4CAF50)
    RepairStatus.DELIVERED -> Color(0xFF9E9E9E)
    RepairStatus.DELAYED -> Color(0xFFC62828)
    RepairStatus.CANCELLED -> Color(0xFFF44336)
}
