package com.example.myapplication.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.example.myapplication.models.Expense
import com.example.myapplication.ui.components.AppScrollbar
import com.example.myapplication.utils.PlatformUtils
import com.example.myapplication.viewmodel.ExpensesViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    onBack: () -> Unit,
    viewModel: ExpensesViewModel = viewModel(factory = ExpensesViewModel.Factory)
) {
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val dateFilter by viewModel.dateFilter.collectAsState()
    
    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<Expense?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val state = rememberLazyListState()

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("إدارة المصاريف", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleManageCategories(true) }) {
                        Icon(Icons.Default.Settings, contentDescription = "إدارة التصنيفات")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.toggleAddDialog(true) }) {
                Icon(Icons.Default.Add, contentDescription = "إضافة مصروف")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // الفلاتر
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("الكل", "اليوم", "هذا الشهر", "مخصص").forEach { filter ->
                        FilterChip(
                            selected = dateFilter == filter,
                            onClick = { 
                                if (filter == "مخصص") viewModel.toggleRangeDialog(true)
                                else viewModel.setDateFilter(filter)
                            },
                            label = { Text(filter) },
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }

                // الإحصائيات
                val stats = remember(expenses) {
                    Pair(expenses.sumOf { it.amount }, expenses.size)
                }

                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("إجمالي المصاريف", fontSize = 12.sp)
                            Text("${stats.first.toInt()} DA", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Card(modifier = Modifier.weight(0.6f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("عدد العمليات", fontSize = 12.sp)
                            Text("${stats.second}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // الرسم البياني
                AnimatedVisibility(
                    visible = expenses.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    val distribution = remember(expenses) {
                        expenses.groupBy { it.category }.mapValues { it.value.sumOf { exp -> exp.amount } }
                    }
                    Card(modifier = Modifier.fillMaxWidth().height(150.dp).padding(bottom = 16.dp)) {
                        Box(modifier = Modifier.padding(8.dp).fillMaxSize(), contentAlignment = Alignment.Center) {
                            com.example.myapplication.ui.components.PieChart(data = distribution)
                        }
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("بحث...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, null)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                LazyColumn(state = state, modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(expenses, key = { it.id }) { expense ->
                        ExpenseItem(
                            expense = expense,
                            onEdit = { editingExpense = expense },
                            onDelete = { showDeleteConfirm = expense }
                        )
                    }
                }
            }
            AppScrollbar(state = state, modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight())
        }

        if (uiState.showAddDialog) {
            AddExpenseDialog(
                categories = categories.map { it.name },
                onDismiss = { viewModel.toggleAddDialog(false) },
                onAdd = { desc, cat, amt ->
                    val newExpense = Expense(
                        id = "EXP-${PlatformUtils.currentTimeMillis()}",
                        description = desc,
                        category = cat,
                        amount = amt,
                        date = PlatformUtils.currentTimeMillis()
                    )
                    viewModel.addExpense(newExpense)
                }
            )
        }
        
        if (editingExpense != null) {
            AddExpenseDialog(
                expense = editingExpense,
                categories = categories.map { it.name },
                onDismiss = { editingExpense = null },
                onAdd = { desc, cat, amt ->
                    editingExpense?.let {
                        viewModel.updateExpense(it.copy(description = desc, category = cat, amount = amt))
                    }
                    editingExpense = null
                }
            )
        }

        if (uiState.showManageCategories) {
            ManageCategoriesDialog(categories, { viewModel.toggleManageCategories(false) }, { viewModel.addCategory(it) }, { viewModel.deleteCategory(it) })
        }

        if (uiState.showRangeDialog) {
            CustomRangeDialog({ viewModel.toggleRangeDialog(false) }, { s, e -> viewModel.setCustomRange(s, e); viewModel.toggleRangeDialog(false) })
        }

        if (showDeleteConfirm != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = null },
                title = { Text("تأكيد الحذف") },
                text = { Text("هل أنت متأكد من رغبتك في حذف ${showDeleteConfirm?.description}؟") },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteConfirm?.let { deleted ->
                            viewModel.deleteExpense(deleted.id) {
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "تم حذف المصروف",
                                        actionLabel = "تراجع"
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.restoreExpense(deleted)
                                    }
                                }
                            }
                        }
                        showDeleteConfirm = null
                    }) { Text("حذف", color = Color.Red) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = null }) { Text("إلغاء") }
                }
            )
        }
    }
}

@Composable
fun ManageCategoriesDialog(categories: List<com.example.myapplication.models.ExpenseCategory>, onDismiss: () -> Unit, onAdd: (String) -> Unit, onDelete: (String) -> Unit) {
    var newCatName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إدارة التصنيفات") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = newCatName, onValueChange = { newCatName = it }, label = { Text("تصنيف جديد") }, modifier = Modifier.weight(1f), singleLine = true)
                    IconButton(onClick = { if (newCatName.isNotBlank()) { onAdd(newCatName); newCatName = "" } }) {
                        Icon(Icons.Default.AddCircle, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn {
                    items(categories) { cat ->
                        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(cat.name)
                            IconButton(onClick = { onDelete(cat.id) }) { Icon(Icons.Default.Delete, null, tint = Color.Red.copy(0.6f)) }
                        }
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } }
    )
}

@Composable
fun CustomRangeDialog(onDismiss: () -> Unit, onConfirm: (Long, Long) -> Unit) {
    var sD by remember { mutableStateOf("") }
    var sM by remember { mutableStateOf("") }
    var sY by remember { mutableStateOf("") }
    var eD by remember { mutableStateOf("") }
    var eM by remember { mutableStateOf("") }
    var eY by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تحديد فترة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (hasError) {
                    Text("يرجى التأكد من صحة التواريخ", color = Color.Red, fontSize = 12.sp)
                }
                Text("من:", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DateInputField(sD, { sD = it; hasError = false }, "يوم", Modifier.weight(1f))
                    DateInputField(sM, { sM = it; hasError = false }, "شهر", Modifier.weight(1f))
                    DateInputField(sY, { sY = it; hasError = false }, "سنة", Modifier.weight(1.5f))
                }
                Text("إلى:", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DateInputField(eD, { eD = it; hasError = false }, "يوم", Modifier.weight(1f))
                    DateInputField(eM, { eM = it; hasError = false }, "شهر", Modifier.weight(1f))
                    DateInputField(eY, { eY = it; hasError = false }, "سنة", Modifier.weight(1.5f))
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                try {
                    val startDate = LocalDate(sY.toInt(), sM.toInt(), sD.toInt())
                    val endDate = LocalDate(eY.toInt(), eM.toInt(), eD.toInt())
                    
                    val startMillis = startDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                    val endMillis = endDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds() + 86399999L
                    
                    if (endMillis < startMillis) {
                        hasError = true
                    } else {
                        onConfirm(startMillis, endMillis)
                    }
                } catch (_: Exception) {
                    hasError = true
                }
            }) { Text("تطبيق") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
fun DateInputField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 4) onValueChange(it) },
        label = { Text(label, fontSize = 10.sp) },
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
    )
}

@Composable
fun ExpenseItem(expense: Expense, onEdit: () -> Unit, onDelete: () -> Unit) {
    val isAdmin = AppRepository.isAdmin
    
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(expense.description, fontWeight = FontWeight.SemiBold)
                Text(expense.category, fontSize = 11.sp, color = Color.Gray)
                Row {
                    Text("${expense.amount.toInt()} DA", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text(PlatformUtils.formatDate(expense.date), fontSize = 11.sp, color = Color.Gray)
                }
            }
            
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }

            if (isAdmin) {
                IconButton(onClick = onDelete) { 
                    Icon(Icons.Default.Delete, null, tint = Color.Red.copy(0.7f), modifier = Modifier.size(20.dp)) 
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    expense: Expense? = null,
    categories: List<String>, 
    onDismiss: () -> Unit, 
    onAdd: (String, String, Double) -> Unit
) {
    val displayCategories = categories.ifEmpty { listOf("أخرى") }
    var desc by remember { mutableStateOf(expense?.description ?: "") }
    var cat by remember { mutableStateOf(expense?.category ?: displayCategories.first()) }
    var amt by remember { mutableStateOf(expense?.amount?.toString() ?: "") }
    var expanded by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (expense == null) "إضافة مصروف" else "تعديل مصروف") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("الوصف") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = cat,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("التصنيف") },
                        trailingIcon = { 
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        displayCategories.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    cat = selectionOption
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amt,
                    onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*$"))) amt = it },
                    label = { Text("المبلغ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val a = amt.toDoubleOrNull()
                if (desc.isNotBlank() && a != null) onAdd(desc, cat, a)
            }) { Text(if (expense == null) "إضافة" else "حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}
