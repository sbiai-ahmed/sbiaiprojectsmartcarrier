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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.database.AppRepository
import com.example.myapplication.models.Product
import com.example.myapplication.ui.components.AppScrollbar
import com.example.myapplication.viewmodel.InventoryViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    onBack: () -> Unit, 
    viewModel: InventoryViewModel = viewModel(factory = InventoryViewModel.Factory)
) {
    val products by viewModel.products.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val state = rememberLazyListState()
    
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }

    val filteredProducts = remember(searchQuery, products) {
        products.filter { 
            it.name.contains(searchQuery, ignoreCase = true) || 
            it.category.contains(searchQuery, ignoreCase = true) 
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("إدارة المخزن (السلع)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "إضافة منتج")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                val stats = remember(products) {
                    val total = products.sumOf { it.stockQuantity }
                    val lowStock = products.count { it.stockQuantity < it.minStockAlert }
                    val totalValue = products.sumOf { it.buyPrice * it.stockQuantity }
                    Triple(total, lowStock, totalValue)
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCardSmall(title = "إجمالي القطع", value = "${stats.first}", color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                    StatCardSmall(title = "نقص المخزن", value = "${stats.second}", color = Color.Red, modifier = Modifier.weight(1f))
                    StatCardSmall(title = "قيمة المخزن", value = "${stats.third.toInt()} DA", color = Color(0xFF4CAF50), modifier = Modifier.weight(1.2f))
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث في المخزن...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                if (filteredProducts.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(if (searchQuery.isEmpty()) "المخزن فارغ" else "لا توجد نتائج", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        state = state,
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredProducts, key = { it.productId }) { product ->
                            Box(modifier = Modifier.animateItem()) {
                                InventoryItem(
                                    product = product,
                                    onEdit = { editingProduct = product },
                                    onDelete = {
                                        viewModel.deleteProduct(product.productId) { deleted: Product ->
                                            scope.launch {
                                                val result = snackbarHostState.showSnackbar(
                                                    message = "تم حذف ${deleted.name}",
                                                    actionLabel = "تراجع",
                                                    duration = SnackbarDuration.Short
                                                )
                                                if (result == SnackbarResult.ActionPerformed) {
                                                    viewModel.restoreProduct(deleted)
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
            ProductDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { product ->
                    viewModel.addProduct(product)
                    showAddDialog = false
                }
            )
        }

        if (editingProduct != null) {
            ProductDialog(
                product = editingProduct,
                onDismiss = { editingProduct = null },
                onConfirm = { product ->
                    viewModel.updateProduct(product)
                    editingProduct = null
                }
            )
        }
    }
}

@Composable
fun StatCardSmall(title: String, value: String, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 9.sp, color = Color.Gray, maxLines = 1)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun InventoryItem(product: Product, onEdit: () -> Unit, onDelete: () -> Unit) {
    val isLowStock = product.stockQuantity < product.minStockAlert
    val isAdmin = AppRepository.isAdmin
    val isEmployee = AppRepository.isEmployee

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (isLowStock) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isLowStock) Icons.Default.Warning else Icons.Default.Inventory, 
                    contentDescription = null, 
                    tint = if (isLowStock) Color.Red else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(product.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(product.category, fontSize = 11.sp, color = Color.Gray)
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
                    
                    Badge(containerColor = if (isLowStock) Color.Red else MaterialTheme.colorScheme.secondary) {
                        Text("${product.stockQuantity} قطعة", color = Color.White)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("شراء: ${product.buyPrice.toInt()} DA", fontSize = 13.sp)
                Text("بيع: ${product.sellPrice.toInt()} DA", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun ProductDialog(product: Product? = null, onDismiss: () -> Unit, onConfirm: (Product) -> Unit) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var category by remember { mutableStateOf(product?.category ?: "إكسسوارات") }
    var buyPrice by remember { mutableStateOf(product?.buyPrice?.toString() ?: "") }
    var sellPrice by remember { mutableStateOf(product?.sellPrice?.toString() ?: "") }
    var quantity by remember { mutableStateOf(product?.stockQuantity?.toString() ?: "") }
    var minStock by remember { mutableStateOf(product?.minStockAlert?.toString() ?: "5") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (product == null) "إضافة منتج جديد" else "تعديل المنتج") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم المنتج") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("التصنيف") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = buyPrice, 
                        onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*$"))) buyPrice = it }, 
                        label = { Text("سعر الشراء") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = sellPrice, 
                        onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*$"))) sellPrice = it }, 
                        label = { Text("سعر البيع") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantity, 
                        onValueChange = { if (it.all { c -> c.isDigit() }) quantity = it }, 
                        label = { Text("الكمية") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = minStock, 
                        onValueChange = { if (it.all { c -> c.isDigit() }) minStock = it }, 
                        label = { Text("تنبيه النقص") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
                if (name.isNotBlank()) {
                    onConfirm(Product(
                        productId = product?.productId ?: "P${com.example.myapplication.utils.PlatformUtils.currentTimeMillis()}",
                        name = name,
                        category = category,
                        buyPrice = buyPrice.toDoubleOrNull() ?: 0.0,
                        sellPrice = sellPrice.toDoubleOrNull() ?: 0.0,
                        stockQuantity = quantity.toIntOrNull() ?: 0,
                        minStockAlert = minStock.toIntOrNull() ?: 5
                    ))
                }
            }) {
                Text(if (product == null) "إضافة" else "حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
