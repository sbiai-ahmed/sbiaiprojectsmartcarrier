package com.example.myapplication.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.viewmodel.MonthlyReport

@Composable
fun GroupedBarChart(data: List<MonthlyReport>) {
    val maxVal = data.maxOfOrNull { maxOf(it.revenue, it.expenses) }?.takeIf { it > 0 } ?: 1.0
    
    Column(modifier = Modifier.fillMaxWidth().height(200.dp)) {
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEach { report ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .fillMaxHeight((report.revenue / maxVal).toFloat().coerceAtLeast(0.05f))
                                .background(Color(0xFF4CAF50))
                        )
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .fillMaxHeight((report.expenses / maxVal).toFloat().coerceAtLeast(0.05f))
                                .background(Color(0xFFF44336))
                        )
                    }
                    Text(report.month, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        
        Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendItem("إيرادات", Color(0xFF4CAF50))
            LegendItem("مصاريف", Color(0xFFF44336))
        }
    }
}

@Composable
fun LineChart(data: List<MonthlyReport>) {
    val profits = data.map { it.profit }
    val maxProfit = profits.maxOrNull()?.takeIf { it > 0 } ?: 1.0
    val minProfit = profits.minOrNull() ?: 0.0
    val range = (maxProfit - minProfit).takeIf { it > 0 } ?: 1.0

    Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
        val width = size.width
        val height = size.height
        val spacing = width / (data.size - 1).coerceAtLeast(1)

        val path = Path()
        data.forEachIndexed { index, report ->
            val x = index * spacing
            val y = height - ((report.profit - minProfit) / range * height).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            drawCircle(Color(0xFF6200EE), radius = 4.dp.toPx(), center = Offset(x, y))
        }
        drawPath(path, Color(0xFF6200EE), style = Stroke(width = 2.dp.toPx()))
    }
}

@Composable
fun PieChart(data: Map<String, Double>) {
    val total = data.values.sum().takeIf { it > 0 } ?: 1.0
    val colors = listOf(Color(0xFF2196F3), Color(0xFFFF9800), Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF00BCD4))

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(120.dp)) {
            var startAngle = 0f
            data.entries.forEachIndexed { index, entry ->
                val sweepAngle = (entry.value / total * 360f).toFloat()
                drawArc(
                    color = colors[index % colors.size],
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    style = Fill
                )
                startAngle += sweepAngle
            }
        }
        
        Column(modifier = Modifier.padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            data.entries.forEachIndexed { index, entry ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(colors[index % colors.size], CircleShape))
                    Text(" ${entry.key}: ${entry.value.toInt()} DA", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).background(color))
        Text(" $label", fontSize = 11.sp)
    }
}
