package com.example.myapplication.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.components.*
import com.example.myapplication.viewmodel.AdminReportsViewModel
import com.example.myapplication.viewmodel.ProfitData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onBack: () -> Unit, 
    viewModel: AdminReportsViewModel = viewModel(factory = AdminReportsViewModel.Factory)
) {
    val stats by viewModel.profitStats.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    
    var showRangeDialog by remember { mutableStateOf(false) }
    var showExplanationDialog by remember { mutableStateOf(false) }
    val state = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تقارير الأرباح والإدارة", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // شريط الفلاتر الزمنية
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf("الكل", "اليوم", "هذا الأسبوع", "هذا الشهر", "مخصص")
                    items(filters) { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { 
                                if (filter == "مخصص") showRangeDialog = true
                                else viewModel.setFilter(filter)
                            },
                            label = { Text(filter) },
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }

                LazyColumn(
                    state = state,
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // البطاقة الأولى: ملخص الميزانية
                    item {
                        ProfitHighlightCard(stats.netProfit, stats.totalRevenue) {
                            showExplanationDialog = true
                        }
                    }

                    // البطاقة الثانية: تفاصيل الإيرادات والأرباح
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SmallInfoCard("أرباح المبيعات", "${stats.salesProfit.toInt()} DA", Color(0xFF4CAF50), Modifier.weight(1f))
                            SmallInfoCard("إيراد الصيانة", "${stats.repairRevenue.toInt()} DA", Color(0xFF2196F3), Modifier.weight(1f))
                        }
                    }

                    // تفصيل التكاليف (المصاريف والمشتريات)
                    item {
                        ReportCard(title = "المصاريف والمشتريات") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("إجمالي المصاريف:", fontSize = 14.sp)
                                    Text("${stats.expenseTotal.toInt()} DA", color = Color.Red, fontWeight = FontWeight.Bold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("إجمالي المشتريات:", fontSize = 14.sp)
                                    Text("${stats.purchaseTotal.toInt()} DA", color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // قائمة المصاريف التفصيلية
                    if (stats.expensesList.isNotEmpty()) {
                        item { Text("تفاصيل المصاريف للفترة", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                        items(stats.expensesList) { expense ->
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))) {
                                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text(expense.description, fontWeight = FontWeight.SemiBold)
                                        Text(expense.category, fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Text("${expense.amount.toInt()} DA", color = Color.Red, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // إحصائيات العمليات
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("ملخص النشاط", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                ActivityRow("أجهزة تم إصلاحها", stats.repairCount.toString(), Icons.Default.Build)
                                ActivityRow("عمليات بيع مكتملة", stats.salesCount.toString(), Icons.Default.ShoppingCart)
                            }
                        }
                    }
                    
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
            
            AppScrollbar(state = state, modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight())
        }

        if (showRangeDialog) {
            CustomRangeDialog(
                onDismiss = { showRangeDialog = false },
                onConfirm = { start, end ->
                    viewModel.setCustomRange(start, end)
                    showRangeDialog = false
                }
            )
        }

        if (showExplanationDialog) {
            ProfitExplanationDialog(stats) { showExplanationDialog = false }
        }
    }
}

@Composable
fun ProfitHighlightCard(profit: Double, totalRevenue: Double, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = if (profit >= 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("صافي الربح الحقيقي للفترة ℹ️", fontSize = 14.sp, color = Color.Gray)
            Text("${profit.toInt()} DA", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = if (profit >= 0) Color(0xFF2E7D32) else Color(0xFFC62828))
            Spacer(modifier = Modifier.height(8.dp))
            Text("إجمالي المداخيل: ${totalRevenue.toInt()} DA", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(if (profit >= 0) "أداء ممتاز 📈" else "تحذير: المصاريف تفوق الأرباح ⚠️", fontSize = 12.sp)
        }
    }
}

@Composable
fun ProfitExplanationDialog(stats: ProfitData, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("كيف تم حساب الأرباح؟") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("المعادلة:", fontWeight = FontWeight.Bold)
                Text("الصافي = (أرباح المنتجات + إيراد الصيانة) - المصاريف الكلية", fontSize = 13.sp)
                
                HorizontalDivider()
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("أرباح المنتجات:")
                    Text("+ ${stats.salesProfit.toInt()} DA", color = Color(0xFF2E7D32))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("إيرادات الصيانة:")
                    Text("+ ${stats.repairRevenue.toInt()} DA", color = Color(0xFF2E7D32))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("إجمالي المصاريف:")
                    Text("- ${stats.expenseTotal.toInt()} DA", color = Color.Red)
                }
                
                HorizontalDivider()
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("الصافي النهائي:", fontWeight = FontWeight.Bold)
                    Text("${stats.netProfit.toInt()} DA", fontWeight = FontWeight.ExtraBold, color = if (stats.netProfit >= 0) Color(0xFF2E7D32) else Color(0xFFC62828))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("فهمت") } }
    )
}

@Composable
fun SmallInfoCard(title: String, value: String, color: Color, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontSize = 11.sp, color = Color.Gray)
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun ActivityRow(label: String, value: String, icon: ImageVector) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, modifier = Modifier.weight(1f), fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ReportCard(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}
