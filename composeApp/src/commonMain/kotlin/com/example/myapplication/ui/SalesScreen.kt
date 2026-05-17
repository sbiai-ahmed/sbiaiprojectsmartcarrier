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
import kotlinx.coroutines.launch
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
import com.example.myapplication.models.SaleRecord
import com.example.myapplication.ui.components.AppScrollbar
import com.example.myapplication.viewmodel.SalesViewModel
import com.example.myapplication.utils.PlatformUtils
import com.example.myapplication.utils.InvoiceGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    onBack: () -> Unit, 
    viewModel: SalesViewModel = viewModel(factory = SalesViewModel.Factory)
) {
    val products by viewModel.products.collectAsState(initial = emptyList())
    val sales by viewModel.sales.collectAsState(initial = emptyList())
    val error by viewModel.errorMessage.collectAsState(initial = null)
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var editingSale by remember { mutableStateOf<SaleRecord?>(null) }
    var showAddSaleDialog by remember { mutableStateOf(false) }
    
    val state = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val filteredProducts = remember(searchQuery, products) {
        products.filter { product ->
            product.name.contains(searchQuery, ignoreCase = true) || 
            product.category.contains(searchQuery, ignoreCase = true) 
        }
    }

    val filteredSales = remember(searchQuery, sales) {
        sales.filter { sale ->
            sale.description.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("المبيعات", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        floatingActionButton = {
            // التحقق من الصلاحيات عبر AppRepository إن وجد أو تمكينها افتراضياً
            FloatingActionButton(
                onClick = { showAddSaleDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة مبيعات")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("البيع (POS)") },
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("سجل المبيعات") },
                    icon = { Icon(Icons.Default.History, contentDescription = null) }
                )
            }

            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (selectedTab == 1) {
                        val totalSales = remember(sales) { sales.sumOf { it.amount } }
                        val totalProfit = remember(sales) { sales.sumOf { it.profit } }
                        
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("إجمالي المبيعات", fontSize = 11.sp)
                                    Text("${totalSales.toInt()} DA", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("صافي الربح", fontSize = 11.sp, color = Color(0xFF2E7D32))
                                    Text("${totalProfit.toInt()} DA", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(if (selectedTab == 0) "ابحث عن منتج للبيع..." else "بحث في سجل المبيعات...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (selectedTab == 0) {
                        if (filteredProducts.isEmpty()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("لا توجد منتجات تطابق البحث", color = Color.Gray)
                            }
                        } else {
                            LazyColumn(
                                state = state,
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredProducts) { product ->
                                    ProductSaleItem(
                                        product = product, 
                                        onSale = { qty, price -> viewModel.makeSale(product, qty, price) }
                                    )
                                }
                            }
                        }
                    } else {
                        if (filteredSales.isEmpty()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("سجل المبيعات فارغ", color = Color.Gray)
                            }
                        } else {
                            LazyColumn(
                                state = state,
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filteredSales, key = { it.saleId }) { sale ->
                                    Box(modifier = Modifier.animateItem()) {
                                        SaleRecordItem(
                                            sale = sale,
                                            onEdit = { editingSale = sale },
                                            onDelete = {
                                                viewModel.deleteSale(sale) { deleted ->
                                                    scope.launch {
                                                        val result = snackbarHostState.showSnackbar(
                                                            message = "تم حذف العملية",
                                                            actionLabel = "تراجع",
                                                            duration = SnackbarDuration.Short
                                                        )
                                                        if (result == SnackbarResult.ActionPerformed) {
                                                            viewModel.restoreSale(deleted)
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
                }
                AppScrollbar(
                    state = state,
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                )
            }
        }

        if (showAddSaleDialog) {
            AddSaleDialog(
                products = products.filter { it.stockQuantity > 0 },
                onDismiss = { showAddSaleDialog = false },
                onConfirm = { product, qty, price ->
                    viewModel.makeSale(product, qty, price)
                    showAddSaleDialog = false
                }
            )
        }

        if (editingSale != null) {
            EditSaleDialog(
                sale = editingSale!!,
                onDismiss = { editingSale = null },
                onConfirm = { updatedSale ->
                    viewModel.updateSale(updatedSale)
                    editingSale = null
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSaleDialog(
    products: List<Product>,
    onDismiss: () -> Unit,
    onConfirm: (Product, Int, Double) -> Unit
) {
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var quantity by remember { mutableStateOf("1") }
    var soldAtPrice by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(selectedProduct) {
        selectedProduct?.let { soldAtPrice = it.sellPrice.toString() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسجيل عملية بيع جديدة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("اختر المنتج المتوفر:")
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedProduct?.name ?: "اضغط للاختيار",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        products.forEach { product ->
                            DropdownMenuItem(
                                text = { 
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(product.name)
                                        Text("${product.stockQuantity} متوفر", color = Color.Gray, fontSize = 12.sp)
                                    }
                                },
                                onClick = {
                                    selectedProduct = product
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { if (it.all { c -> c.isDigit() }) quantity = it },
                    label = { Text("الكمية") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = soldAtPrice,
                    onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*$"))) soldAtPrice = it },
                    label = { Text("سعر البيع الفعلي (للقطعة)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                selectedProduct?.let {
                    val q = quantity.toIntOrNull() ?: 0
                    val p = soldAtPrice.toDoubleOrNull() ?: 0.0
                    val total = q * p
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("تفاصيل الفاتورة:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("السعر الإجمالي: ${total.toInt()} DA", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val q = quantity.toIntOrNull() ?: 0
                    val p = soldAtPrice.toDoubleOrNull() ?: 0.0
                    if (selectedProduct != null && q > 0 && q <= (selectedProduct?.stockQuantity ?: 0) && p > 0) {
                        onConfirm(selectedProduct!!, q, p)
                    }
                },
                enabled = selectedProduct != null && (quantity.toIntOrNull() ?: 0) > 0 && soldAtPrice.isNotEmpty()
            ) { Text("تأكيد البيع") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun ProductSaleItem(product: Product, onSale: (Int, Double) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(product.category, fontSize = 12.sp, color = Color.Gray)
                Text("${product.sellPrice.toInt()} DA", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                Text("المتوفر: ${product.stockQuantity}", fontSize = 12.sp, color = if (product.stockQuantity < product.minStockAlert) Color.Red else Color.DarkGray)
            }
            
            Button(
                onClick = { showDialog = true },
                enabled = product.stockQuantity > 0,
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.AddShoppingCart, contentDescription = null)
                Text("بيع")
            }
        }
    }

    if (showDialog) {
        SellProductDialog(
            product = product,
            onDismiss = { showDialog = false },
            onConfirm = { qty, price ->
                onSale(qty, price)
                showDialog = false
            }
        )
    }
}

@Composable
fun SaleRecordItem(sale: SaleRecord, onEdit: () -> Unit, onDelete: () -> Unit) {
    rememberCoroutineScope()
    val dateText = remember(sale.date) {
        PlatformUtils.formatDate(sale.date)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(sale.description, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                    Spacer(Modifier.width(4.dp))
                    Text(sale.processedBy, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(12.dp))
                    Icon(Icons.Default.Schedule, null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                    Spacer(Modifier.width(4.dp))
                    Text(dateText, fontSize = 11.sp, color = Color.Gray)
                }
                Text("السعر المباع به: ${sale.soldAtPrice.toInt()} DA", fontSize = 11.sp, color = Color.DarkGray)
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${sale.amount.toInt()} DA", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                }
                
                IconButton(onClick = {
                    val htmlContent = InvoiceGenerator.generateSaleInvoice(sale)
                    PlatformUtils.printHtml(htmlContent, "sale_${sale.saleId}")
                }) {
                    Icon(Icons.Default.Print, contentDescription = "طباعة الفاتورة", tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun EditSaleDialog(sale: SaleRecord, onDismiss: () -> Unit, onConfirm: (SaleRecord) -> Unit) {
    var desc by remember { mutableStateOf(sale.description) }
    var amount by remember { mutableStateOf(sale.amount.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل سجل البيع") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("الوصف") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amount, onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*$"))) amount = it }, label = { Text("المبلغ") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amount.toDoubleOrNull() ?: 0.0
                onConfirm(sale.copy(description = desc, amount = amt))
            }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
fun SellProductDialog(product: Product, onDismiss: () -> Unit, onConfirm: (Int, Double) -> Unit) {
    var quantity by remember { mutableStateOf("1") }
    var soldAtPrice by remember { mutableStateOf(product.sellPrice.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تأكيد البيع") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("المنتج: ${product.name}")
                Text("السعر المقترح: ${product.sellPrice.toInt()} DA", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { if (it.all { char -> char.isDigit() }) quantity = it },
                    label = { Text("الكمية") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = soldAtPrice,
                    onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*$"))) soldAtPrice = it },
                    label = { Text("السعر الفعلي للبيع") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { 
                val q = quantity.toIntOrNull() ?: 0
                val p = soldAtPrice.toDoubleOrNull() ?: 0.0
                if (q > 0 && q <= product.stockQuantity && p > 0) onConfirm(q, p)
            }) {
                Text("بيع الآن")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
