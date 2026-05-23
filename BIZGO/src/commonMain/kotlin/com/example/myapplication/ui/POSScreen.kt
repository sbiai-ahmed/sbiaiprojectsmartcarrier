package com.example.myapplication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.database.AppRepository
import com.example.myapplication.models.Product
import com.example.myapplication.utils.PlatformUtils
import kotlinx.coroutines.launch

data class CartItem(
    val product: Product,
    var quantity: Int,
    val pricePerUnit: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun POSScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var cartItems by remember { mutableStateOf<MutableList<CartItem>>(mutableListOf()) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var showProductDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedPriceType by remember { mutableStateOf("normal") } // normal, wholesale

    LaunchedEffect(Unit) {
        AppRepository.products.collect { productList ->
            products = productList
        }
    }

    val filteredProducts = products.filter { product ->
        product.name.contains(searchQuery, ignoreCase = true) || 
        product.barcode.contains(searchQuery, ignoreCase = true)
    }

    val totalAmount = cartItems.sumOf { it.quantity * it.pricePerUnit }
    val totalTax = totalAmount * 0.19

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("نقطة البيع (POS)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // القسم الأيسر - المنتجات
            Column(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث عن منتج...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    shape = RoundedCornerShape(8.dp)
                )

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredProducts) { product ->
                        ProductCard(
                            product = product,
                            onClick = {
                                selectedProduct = product
                                showProductDialog = true
                            }
                        )
                    }
                }
            }

            // الفاصل
            Divider(modifier = Modifier.width(1.dp).fillMaxHeight())

            // القسم الأيمن - السلة والدفع
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFFF5F5F5))
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "السلة",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(8.dp)
                )

                // عناصر السلة
                if (cartItems.isEmpty()) {
                    Text(
                        "السلة فارغة",
                        color = Color.Gray,
                        modifier = Modifier.padding(24.dp)
                    )
                } else {
                    cartItems.forEachIndexed { index, cartItem ->
                        CartItemCard(
                            cartItem = cartItem,
                            onQuantityChange = { newQuantity ->
                                if (newQuantity > 0) {
                                    cartItems[index].quantity = newQuantity
                                } else {
                                    cartItems.removeAt(index)
                                }
                            },
                            onRemove = { cartItems.removeAt(index) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ملخص الفاتورة
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("المجموع:")
                            Text("%.2f DA".format(totalAmount), fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الضريبة (19%):")
                            Text("%.2f DA".format(totalTax), fontWeight = FontWeight.Bold)
                        }
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الإجمالي:", fontWeight = FontWeight.Bold)
                            Text(
                                "%.2f DA".format(totalAmount + totalTax),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // زر الدفع
                Button(
                    onClick = { 
                        if (cartItems.isNotEmpty()) {
                            showPaymentDialog = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = cartItems.isNotEmpty(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PaymentOutlined, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إتمام الدفع", fontWeight = FontWeight.Bold)
                }

                // زر مسح السلة
                OutlinedButton(
                    onClick = { cartItems.clear() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(top = 8.dp),
                    enabled = cartItems.isNotEmpty(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Clear, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("مسح السلة")
                }
            }
        }
    }

    // حوار اختيار السعر
    if (showProductDialog && selectedProduct != null) {
        AlertDialog(
            onDismissRequest = { showProductDialog = false },
            title = { Text(selectedProduct!!.name) },
            text = {
                Column {
                    Text("السعر العادي: ${selectedProduct!!.sellPrice} DA")
                    Text("السعر الجملة: ${selectedProduct!!.wholesalePrice} DA")
                    Text("المتاح: ${selectedProduct!!.stockQuantity} وحدة")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val newItem = CartItem(
                        selectedProduct!!,
                        1,
                        selectedProduct!!.sellPrice
                    )
                    val existingItem = cartItems.find { it.product.productId == selectedProduct!!.productId }
                    if (existingItem != null) {
                        existingItem.quantity++
                    } else {
                        cartItems.add(newItem)
                    }
                    showProductDialog = false
                }) {
                    Text("إضافة للسلة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProductDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // حوار الدفع
    if (showPaymentDialog) {
        PaymentDialog(
            totalAmount = totalAmount + totalTax,
            onConfirm = { paymentAmount ->
                scope.launch {
                    // تسجيل كل عملية بيع
                    cartItems.forEach { cartItem ->
                        AppRepository.makeSale(
                            cartItem.product,
                            cartItem.quantity,
                            cartItem.pricePerUnit
                        )
                    }
                    cartItems.clear()
                    showPaymentDialog = false
                }
            },
            onDismiss = { showPaymentDialog = false }
        )
    }
}

@Composable
fun ProductCard(
    product: Product,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            Text(product.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("سعر البيع: ${product.sellPrice} DA", fontSize = 12.sp)
                    Text("المتاح: ${product.stockQuantity}", fontSize = 12.sp, color = Color.Gray)
                }
                Badge(
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Text(product.category)
                }
            }
        }
    }
}

@Composable
fun CartItemCard(
    cartItem: CartItem,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(8.dp).fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    cartItem.product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${cartItem.pricePerUnit} DA x ${cartItem.quantity}", fontSize = 11.sp)
                Text(
                    "%.2f DA".format(cartItem.quantity * cartItem.pricePerUnit),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { onQuantityChange(cartItem.quantity - 1) },
                    modifier = Modifier.size(28.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("-", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Text(cartItem.quantity.toString(), fontSize = 11.sp)
                Button(
                    onClick = { onQuantityChange(cartItem.quantity + 1) },
                    modifier = Modifier.size(28.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("+", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun PaymentDialog(
    totalAmount: Double,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var paymentAmount by remember { mutableStateOf(totalAmount.toString()) }
    var paymentMethod by remember { mutableStateOf("cash") } // cash, card, check

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("الدفع") },
        text = {
            Column {
                Text("المبلغ المطلوب: %.2f DA".format(totalAmount), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = paymentAmount,
                    onValueChange = { paymentAmount = it },
                    label = { Text("المبلغ المدفوع") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("طريقة الدفع:", fontWeight = FontWeight.Bold)
                Row {
                    RadioButton(
                        selected = paymentMethod == "cash",
                        onClick = { paymentMethod = "cash" }
                    )
                    Text("نقد", modifier = Modifier.align(Alignment.CenterVertically))
                }
                Row {
                    RadioButton(
                        selected = paymentMethod == "card",
                        onClick = { paymentMethod = "card" }
                    )
                    Text("بطاقة", modifier = Modifier.align(Alignment.CenterVertically))
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(paymentAmount.toDoubleOrNull() ?: totalAmount)
            }) {
                Text("تأكيد الدفع")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
