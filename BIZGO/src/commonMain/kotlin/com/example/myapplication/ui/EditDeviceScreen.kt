package com.example.myapplication.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.viewmodel.RepairsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDeviceScreen(
    deviceId: String, 
    viewModel: RepairsViewModel = viewModel(factory = RepairsViewModel.Factory),
    onSaveSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val devices by viewModel.devices.collectAsState()
    val device = devices.find { it.deviceId == deviceId }

    if (device == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var customerName by remember { mutableStateOf(device.customerName) }
    var phoneModel by remember { mutableStateOf(device.modelName) }
    var issueDescription by remember { mutableStateOf(device.issueDescription) }
    var estimatedCost by remember { mutableStateOf(device.estimatedCost.toString()) }
    
    var customerNameError by remember { mutableStateOf(false) }
    var phoneModelError by remember { mutableStateOf(false) }
    var estimatedCostError by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("تعديل بيانات الجهاز", fontWeight = FontWeight.Bold) },
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
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("تعديل طلب رقم: ${device.deviceId}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = customerName,
                onValueChange = { 
                    customerName = it
                    customerNameError = false
                },
                label = { Text("اسم الزبون") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                isError = customerNameError,
                supportingText = { if (customerNameError) Text("يرجى إدخال اسم الزبون") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = phoneModel,
                onValueChange = { 
                    phoneModel = it
                    phoneModelError = false
                },
                label = { Text("نوع الهاتف") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                isError = phoneModelError,
                supportingText = { if (phoneModelError) Text("يرجى إدخال نوع الهاتف") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = issueDescription,
                onValueChange = { issueDescription = it },
                label = { Text("وصف المشكلة") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = estimatedCost,
                onValueChange = { 
                    estimatedCost = it
                    estimatedCostError = false
                },
                label = { Text("التكلفة التقديرية") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                isError = estimatedCostError,
                supportingText = { if (estimatedCostError) Text("يرجى إدخال مبلغ صحيح") }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { 
                    val cost = estimatedCost.toDoubleOrNull()
                    customerNameError = customerName.isBlank()
                    phoneModelError = phoneModel.isBlank()
                    estimatedCostError = cost == null

                    if (!customerNameError && !phoneModelError && !estimatedCostError) {
                        val updated = device.copy(
                            customerName = customerName,
                            modelName = phoneModel,
                            issueDescription = issueDescription,
                            estimatedCost = cost!!
                        )
                        viewModel.updateDevice(updated, onSaveSuccess)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("حفظ التغييرات", fontWeight = FontWeight.Bold)
            }
        }
    }
}
