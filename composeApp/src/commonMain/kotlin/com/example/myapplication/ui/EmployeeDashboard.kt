package com.example.myapplication.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeDashboard(onLogout: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("واجهة الموظف (Employee)") })
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("مرحباً بك، هنا يمكنك إدارة عمليات الصيانة والاستلام.", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onLogout) {
                Text("تسجيل الخروج")
            }
        }
    }
}
