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
    onNavigateToSales: () -> Unit,
    onNavigateToPOS: () -> Unit // Added for POS
) {
    val scope = rememberCoroutineScope()
    var customerName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var phoneModel by remember { mutableStateOf("") }
    var imei by remember { mutableStateOf("") }
    var issueDescription by remember { mutableStateOf("") }
    var estimatedCost by remember { mutableStateOf("") }
    var isScanningImei by remember { mutableStateOf(false) }

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
            // ... rest of the code remains unchanged
        }
    }
}
// ... rest of the file remains unchanged
