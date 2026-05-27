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
    val pricePerUnit: Double,
    val id: String = product.productId // لتميز العناصر بشكل فريد
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun POSScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var cartItems by remember { mutableStateOf<List<CartItem>>(emptyList()) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var showProductDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showErrorMessage by remember { mutableStateOf("") }
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
    val grandTotal = totalAmount + totalTax

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

                if (showErrorMessage.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFEBEE)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                showErrorMessage,
                                color = Color(0xFFC62828),
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { showErrorMessage = "" },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredProducts) { product ->
                        ProductCard(
                            product = product,
                            onClick = {
                                selectedProduct = product
                                selectedPriceType = "normal"
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
                    cartItems.forEach { cartItem ->
                        CartItemCard(
                            cartItem = cartItem,
                            onQuantityChange = { newQuantity ->
                                if (newQuantity > 0) {
                                    cartItems = cartItems.map { item ->
                                        if (item.id == cartItem.id) item.copy(quantity = newQuantity) else item
                                    }
                                } else {
                                    cartItems = cartItems.filter { it.id != cartItem.id }
                                }
                            },
                            onRemove = { 
                                cartItems = cartItems.filter { it.id != cartItem.id }
                            }
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
                                "%.2f DA".format(grandTotal),
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
                    Icon(Icons.Default.Payment, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إتمام الدفع", fontWeight = FontWeight.Bold)
                }

                // زر مسح السلة
                OutlinedButton(
                    onClick = { cartItems = emptyList() },
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

    // حوار اختيار السعر والكمية
    if (showProductDialog && selectedProduct != null) {
        var quantityInput by remember { mutableStateOf("1") }
        var priceError by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showProductDialog = false },
            title = { Text(selectedProduct!!.name) },
            text = {
                Column {
                    Text("السعر العادي: ${selectedProduct!!.sellPrice} DA", fontSize = 12.sp)
                    Text("السعر الجملة: ${selectedProduct!!.wholesalePrice} DA", fontSize = 12.sp)
                    Text("المتاح: ${selectedProduct!!.stockQuantity} وحدة", fontSize = 12.sp)
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text("اختر نوع السعر:", fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.padding(top = 8.dp)) {
                        RadioButton(
                            selected = selectedPriceType == "normal",
                            onClick = { selectedPriceType = "normal" }
                        )
                        Text("عادي", modifier = Modifier.align(Alignment.CenterVertically))
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(
                            selected = selectedPriceType == "wholesale",
                            onClick = { selectedPriceType = "wholesale" }
                        )
                        Text("جملة", modifier = Modifier.align(Alignment.CenterVertically))
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = quantityInput,
                        onValueChange = { 
                            quantityInput = it
                            priceError = ""
                        },
                        label = { Text("الكمية") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    if (priceError.isNotEmpty()) {
                        Text(
                            priceError,
                            color = Color(0xFFC62828),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val quantity = quantityInput.toIntOrNull()
                    when {
                        quantity == null || quantity <= 0 -> {
                            priceError = "الرجاء إدخال كمية صحيحة"
                        }
                        quantity > selectedProduct!!.stockQuantity -> {
                            priceError = "الكمية تتجاوز المتاح (${selectedProduct!!.stockQuantity} وحدة)"
                        }
                        else -> {
                            val price = if (selectedPriceType == "normal") {
                                selectedProduct!!.sellPrice
                            } else {
                                selectedProduct!!.wholesalePrice
                            }
                            
                            val newItem = CartItem(
                                selectedProduct!!,
                                quantity,
                                price
                            )
                            
                            val existingItem = cartItems.find { it.id == selectedProduct!!.productId }
                            cartItems = if (existingItem != null) {
                                cartItems.map { item ->
                                    if (item.id == selectedProduct!!.productId) {
                                        item.copy(quantity = item.quantity + quantity)
                                    } else {
                                        item
                                    }
                                }
                            } else {
                                cartItems + newItem
                            }
                            showProductDialog = false
                        }
                    }
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
            totalAmount = grandTotal,
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
                    cartItems = emptyList()
                    showPaymentDialog = false
                    showErrorMessage = "تم إتمام الدفع بنجاح!"
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
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp), tint = Color.Red)
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
                horizontalArrangement = Arrangement.Center,
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
                Text(cartItem.quantity.toString(), fontSize = 11.sp, modifier = Modifier.padding(horizontal = 12.dp))
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
    var paymentMethod by remember { mutableStateOf("cash") } // cash, card
    var changeAmount by remember { mutableStateOf(0.0) }
    var paymentError by remember { mutableStateOf("") }

    LaunchedEffect(paymentAmount) {
        val amount = paymentAmount.toDoubleOrNull() ?: 0.0
        changeAmount = (amount - totalAmount).coerceAtLeast(0.0)
        paymentError = if (amount < totalAmount) "المبلغ أقل من المطلوب" else ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("الدفع") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "المبلغ المطلوب: %.2f DA".format(totalAmount),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = paymentAmount,
                    onValueChange = { paymentAmount = it },
                    label = { Text("المبلغ المدفوع") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    isError = paymentError.isNotEmpty()
                )
                if (paymentError.isNotEmpty()) {
                    Text(
                        paymentError,
                        color = Color(0xFFC62828),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // عرض الرصيد
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = if (changeAmount >= 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("الرصيد:", fontWeight = FontWeight.Bold)
                        Text(
                            "%.2f DA".format(changeAmount),
                            fontWeight = FontWeight.Bold,
                            color = if (changeAmount >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text("طريقة الدفع:", fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.padding(top = 8.dp)) {
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
            Button(
                onClick = {
                    val amount = paymentAmount.toDoubleOrNull() ?: 0.0
                    if (amount >= totalAmount) {
                        onConfirm(amount)
                    }
                },
                enabled = paymentError.isEmpty() && paymentAmount.toDoubleOrNull() ?: 0.0 >= totalAmount
            ) {
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
