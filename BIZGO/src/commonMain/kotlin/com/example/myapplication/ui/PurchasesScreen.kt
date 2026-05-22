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
import com.example.myapplication.models.PurchaseRecord
import com.example.myapplication.ui.components.AppScrollbar
import com.example.myapplication.viewmodel.PurchasesViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchasesScreen(
    onBack: () -> Unit,
    viewModel: PurchasesViewModel = viewModel(factory = PurchasesViewModel.Factory)
) {
    val purchases by viewModel.purchases.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingPurchase by remember { mutableStateOf<PurchaseRecord?>(null) }
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val state = rememberLazyListState()

    val filteredPurchases = remember(searchQuery, purchases) {
        if (searchQuery.isBlank()) purchases
        else purchases.filter { 
            it.itemName.contains(searchQuery, ignoreCase = true) || 
            it.supplierName.contains(searchQuery, ignoreCase = true) 
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("سجل المشتريات", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "إضافة مشتريات")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                val stats = remember(purchases) {
                    val total = purchases.sumOf { it.totalCost }
                    val count = purchases.sumOf { it.quantity }
                    Pair(total, count)
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("إجمالي المشتريات", fontSize = 12.sp)
                            Text("${stats.first.toInt()} DA", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(0.6f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("إجمالي القطع", fontSize = 12.sp)
                            Text(stats.second.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث في المشتريات أو الموردين...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                if (filteredPurchases.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ShoppingCartCheckout, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                            Text(
                                if (searchQuery.isEmpty()) "لا توجد سجلات مشتريات" else "لم يتم العثور على نتائج", 
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = state,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredPurchases, key = { it.purchaseId }) { purchase ->
                            Box(modifier = Modifier.animateItem()) {
                                PurchaseItem(
                                    purchase = purchase,
                                    onEdit = { editingPurchase = purchase },
                                    onDelete = {
                                        viewModel.deletePurchase(purchase.purchaseId) { deleted ->
                                            scope.launch {
                                                val result = snackbarHostState.showSnackbar(
                                                    message = "تم حذف ${deleted.itemName}",
                                                    actionLabel = "تراجع",
                                                    duration = SnackbarDuration.Short
                                                )
                                                if (result == SnackbarResult.ActionPerformed) {
                                                    viewModel.restorePurchase(deleted)
                                                }
                                            }
                                        }
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

        if (showAddDialog) {
            AddPurchaseDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { purchase ->
                    viewModel.addPurchase(
                        purchase,
                        onComplete = {
                            showAddDialog = false
                            scope.launch { snackbarHostState.showSnackbar("تمت إضافة عملية الشراء بنجاح") }
                        },
                        onError = {
                            scope.launch { snackbarHostState.showSnackbar("خطأ في إضافة المشتريات") }
                        }
                    )
                }
            )
        }

        if (editingPurchase != null) {
            AddPurchaseDialog(
                purchase = editingPurchase,
                onDismiss = { editingPurchase = null },
                onAdd = { purchase ->
                    viewModel.updatePurchase(
                        purchase,
                        onComplete = {
                            editingPurchase = null
                            scope.launch { snackbarHostState.showSnackbar("تم تعديل عملية الشراء") }
                        },
                        onError = {
                            scope.launch { snackbarHostState.showSnackbar("خطأ في التعديل") }
                        }
                    )
                }
            )
        }
    }
}

@Composable
fun PurchaseItem(purchase: PurchaseRecord, onEdit: () -> Unit, onDelete: () -> Unit) {
    val dateText = remember(purchase.date) {
        com.example.myapplication.utils.PlatformUtils.formatDate(purchase.date)
    }
    val isAdmin = AppRepository.isAdmin
    val isEmployee = AppRepository.isEmployee

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(purchase.itemName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("التصنيف: ${purchase.category}", fontSize = 11.sp, color = Color.Gray)
                }
                
                if (isAdmin || isEmployee) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }

                if (isAdmin) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                    }
                }
                
                Badge(containerColor = MaterialTheme.colorScheme.primary) { 
                    Text("${purchase.quantity} قطعة", color = Color.White) 
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("المورد: ${purchase.supplierName}", fontSize = 12.sp, color = Color.DarkGray)
                Spacer(modifier = Modifier.weight(1f))
                Text(dateText, fontSize = 11.sp, color = Color.Gray)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("سعر الوحدة: ${purchase.unitCost.toInt()} DA", fontSize = 13.sp)
                Text("الإجمالي: ${purchase.totalCost.toInt()} DA", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun AddPurchaseDialog(purchase: PurchaseRecord? = null, onDismiss: () -> Unit, onAdd: (PurchaseRecord) -> Unit) {
    var name by remember { mutableStateOf(purchase?.itemName ?: "") }
    var category by remember { mutableStateOf(purchase?.category ?: "إكسسوارات") }
    var quantity by remember { mutableStateOf(purchase?.quantity?.toString() ?: "") }
    var unitCost by remember { mutableStateOf(purchase?.unitCost?.toString() ?: "") }
    var supplier by remember { mutableStateOf(purchase?.supplierName ?: "") }
    var isPaid by remember { mutableStateOf(purchase?.isPaid ?: true) }

    var nameError by remember { mutableStateOf(false) }
    var quantityError by remember { mutableStateOf(false) }
    var unitCostError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (purchase == null) "إضافة عملية شراء" else "تعديل عملية شراء") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        if (it.isNotBlank()) nameError = false
                    },
                    label = { Text("اسم المنتج") },
                    isError = nameError,
                    supportingText = { if (nameError) Text("يرجى إدخال اسم المنتج") }
                )
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("التصنيف") })
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { 
                        if (it.all { c -> c.isDigit() }) {
                            quantity = it
                            quantityError = false
                        }
                    },
                    label = { Text("الكمية") },
                    isError = quantityError,
                    supportingText = { if (quantityError) Text("يرجى إدخال كمية صحيحة") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = unitCost,
                    onValueChange = { 
                        if (it.matches(Regex("^\\d*\\.?\\d*$"))) {
                            unitCost = it
                            unitCostError = false
                        }
                    },
                    label = { Text("سعر الشراء للوحدة") },
                    isError = unitCostError,
                    supportingText = { if (unitCostError) Text("يرجى إدخال سعر صحيح") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = supplier, 
                    onValueChange = { supplier = it }, 
                    label = { Text("اسم المورد") },
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("تم تسديد المبلغ")
                    Switch(checked = isPaid, onCheckedChange = { isPaid = it })
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
                val qty = quantity.toIntOrNull() ?: 0
                val cost = unitCost.toDoubleOrNull() ?: 0.0
                
                nameError = name.isBlank()
                quantityError = qty <= 0
                unitCostError = cost <= 0.0

                if (!nameError && !quantityError && !unitCostError) {
                    onAdd(PurchaseRecord(
                        purchaseId = purchase?.purchaseId ?: "PUR-${com.example.myapplication.utils.PlatformUtils.currentTimeMillis()}",
                        itemName = name,
                        category = category,
                        quantity = qty,
                        unitCost = cost,
                        totalCost = cost * qty,
                        supplierName = supplier,
                        date = purchase?.date ?: com.example.myapplication.utils.PlatformUtils.currentTimeMillis(),
                        isPaid = isPaid
                    ))
                }
            }) {
                Text(if (purchase == null) "إضافة" else "حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
