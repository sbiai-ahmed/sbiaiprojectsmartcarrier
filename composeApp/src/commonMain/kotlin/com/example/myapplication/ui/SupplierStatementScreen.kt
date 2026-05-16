package com.example.myapplication.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.database.AppRepository
import com.example.myapplication.models.Supplier
import com.example.myapplication.ui.components.AppScrollbar
import com.example.myapplication.utils.PlatformUtils
import com.example.myapplication.viewmodel.SupplierViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierStatementScreen(
    onBack: () -> Unit,
    viewModel: SupplierViewModel = viewModel(factory = SupplierViewModel.Factory)
) {
    val suppliers by viewModel.suppliers.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    
    var selectedSupplier by remember { mutableStateOf<Supplier?>(null) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showAddSupplierDialog by remember { mutableStateOf(false) }
    var editingSupplier by remember { mutableStateOf<Supplier?>(null) }
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val state = rememberLazyListState()

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("كشف حساب الموردين", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSupplierDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "إضافة مورد")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("قائمة الموردين والديون المستحقة", fontSize = 16.sp, color = Color.Gray)
                
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    state = state,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(suppliers, key = { it.supplierId }) { supplier ->
                        Box(modifier = Modifier.animateItem()) {
                            SupplierItem(
                                supplier = supplier,
                                onPayClick = {
                                    selectedSupplier = supplier
                                    showPaymentDialog = true
                                },
                                onEdit = { editingSupplier = supplier },
                                onDelete = {
                                    viewModel.deleteSupplier(supplier.supplierId) { deleted ->
                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "تم حذف ${deleted.name}",
                                                actionLabel = "تراجع",
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.addSupplier(deleted)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                    
                    if (suppliers.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                Text("لا يوجد موردين مسجلين", color = Color.Gray)
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

        if (showPaymentDialog && selectedSupplier != null) {
            PaymentDialog(
                supplier = selectedSupplier!!,
                onDismiss = { showPaymentDialog = false },
                onConfirm = { amount ->
                    viewModel.addSupplierPayment(selectedSupplier!!.supplierId, amount)
                    showPaymentDialog = false
                }
            )
        }

        if (showAddSupplierDialog) {
            SupplierDialog(
                onDismiss = { showAddSupplierDialog = false },
                onConfirm = { name, phone, debt ->
                    val newSupplier = Supplier(
                        supplierId = "SUP-${PlatformUtils.currentTimeMillis() % 10000}",
                        name = name,
                        phoneNumber = phone,
                        totalDebt = debt
                    )
                    viewModel.addSupplier(newSupplier)
                    showAddSupplierDialog = false
                }
            )
        }

        if (editingSupplier != null) {
            SupplierDialog(
                supplier = editingSupplier,
                onDismiss = { editingSupplier = null },
                onConfirm = { name, phone, debt ->
                    viewModel.updateSupplier(editingSupplier!!.copy(name = name, phoneNumber = phone, totalDebt = debt))
                    editingSupplier = null
                }
            )
        }
    }
}

@Composable
fun SupplierDialog(supplier: Supplier? = null, onDismiss: () -> Unit, onConfirm: (String, String, Double) -> Unit) {
    var name by remember { mutableStateOf(supplier?.name ?: "") }
    var phone by remember { mutableStateOf(supplier?.phoneNumber ?: "") }
    var debt by remember { mutableStateOf(supplier?.totalDebt?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (supplier == null) "إضافة مورد جديد" else "تعديل المورد") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم المورد") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("رقم الهاتف") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                OutlinedTextField(value = debt, onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*$"))) debt = it }, label = { Text("الدين الحالي") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
        },
        confirmButton = {
            Button(onClick = { 
                if (name.isNotBlank()) {
                    onConfirm(name, phone, debt.toDoubleOrNull() ?: 0.0)
                }
            }) {
                Text(if (supplier == null) "إضافة" else "حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun SupplierItem(supplier: Supplier, onPayClick: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val isAdmin = AppRepository.isAdmin
    val isEmployee = AppRepository.isEmployee

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(supplier.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("الهاتف: ${supplier.phoneNumber}", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "الدين الحالي: ${supplier.totalDebt.toInt()} DA",
                    fontWeight = FontWeight.ExtraBold,
                    color = if (supplier.totalDebt > 0) Color.Red else Color(0xFF4CAF50)
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isAdmin || isEmployee) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
                if (isAdmin) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                    }
                }
                
                if (supplier.totalDebt > 0) {
                    Button(onClick = onPayClick, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)), shape = RoundedCornerShape(8.dp)) {
                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تسديد", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentDialog(supplier: Supplier, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var amount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسديد دفعة للمورد") },
        text = {
            Column {
                Text("المورد: ${supplier.name}")
                Text("الدين الكلي: ${supplier.totalDebt.toInt()} DA", color = Color.Red)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*$"))) amount = it },
                    label = { Text("المبلغ المراد دفعه (DA)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            Button(onClick = { 
                val amt = amount.toDoubleOrNull() ?: 0.0
                if (amt > 0) onConfirm(amt)
            }) {
                Text("تأكيد الدفع")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
