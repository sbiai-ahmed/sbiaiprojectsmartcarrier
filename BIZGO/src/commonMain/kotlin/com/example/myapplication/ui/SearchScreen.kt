package com.example.myapplication.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.database.AppRepository
import com.example.myapplication.models.PhoneDevice
import com.example.myapplication.ui.components.BarcodeScannerView
import com.example.myapplication.utils.PlatformUtils
import com.example.myapplication.viewmodel.RepairsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit, 
    onEditDevice: (String) -> Unit,
    viewModel: RepairsViewModel = viewModel(factory = RepairsViewModel.Factory)
) {
    var searchQuery by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }
    val allDevicesState = AppRepository.devices.collectAsState(initial = emptyList())
    val allDevices = allDevicesState.value
    val error by viewModel.errorMessage.collectAsState()
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    
    val currentTime = remember { PlatformUtils.currentTimeMillis() }
    val fifteenDaysInMs = 15L * 24 * 60 * 60 * 1000L
    
    val filteredDevices = remember(searchQuery, allDevices) {
        if (searchQuery.isBlank()) emptyList<PhoneDevice>()
        else allDevices.filter { device ->
            device.deviceId.contains(searchQuery, ignoreCase = true) || 
            device.customerName.contains(searchQuery, ignoreCase = true) ||
            device.modelName.contains(searchQuery, ignoreCase = true) ||
            device.imei.contains(searchQuery, ignoreCase = true)
        }
    }
    
    if (isScanning) {
        BarcodeScannerView(
            modifier = Modifier.fillMaxSize(),
            onBarcodeScanned = { result ->
                searchQuery = result
                isScanning = false
            },
            onClose = { isScanning = false }
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("البحث عن جهاز", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("أدخل رقم BON أو اسم الزبون") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                trailingIcon = {
                    Row {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                        IconButton(onClick = { isScanning = true }) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "مسح باركود")
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (searchQuery.isEmpty()) {
                Icon(
                    Icons.Default.FindInPage,
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                    tint = Color.LightGray
                )
                Text(
                    "ابدأ بكتابة الرقم التسلسلي للبحث",
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 16.dp)
                )
            } else {
                if (filteredDevices.isEmpty()) {
                    Text(
                        "لم يتم العثور على نتائج لـ \"$searchQuery\"",
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
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
        }
    }
}
