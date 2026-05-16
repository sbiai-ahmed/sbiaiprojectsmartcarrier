package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
actual fun BarcodeScannerView(
    modifier: Modifier,
    onBarcodeScanned: (String) -> Unit,
    onClose: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(modifier = Modifier.padding(32.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("محاكاة الماسح الضوئي (Desktop)", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("أدخل رقم الباركود يدوياً") }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row {
                    Button(onClick = { if (text.isNotBlank()) onBarcodeScanned(text) }) {
                        Text("تأكيد")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onClose) {
                        Text("إلغاء")
                    }
                }
            }
        }
    }
}
