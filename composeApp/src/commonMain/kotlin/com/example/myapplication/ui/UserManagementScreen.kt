package com.example.myapplication.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.database.AppRepository
import com.example.myapplication.models.User
import com.example.myapplication.models.UserRole
import com.example.myapplication.ui.components.AppScrollbar
import com.example.myapplication.viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    onBack: () -> Unit,
    viewModel: UserViewModel = viewModel(factory = UserViewModel.Factory)
) {
    val users by viewModel.users.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    val state = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إدارة الموظفين", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "إضافة موظف")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                state = state,
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(users) { user ->
                    UserItem(user = user, onDelete = { viewModel.deleteUser(user.userId) })
                }
            }
            AppScrollbar(
                state = state,
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
            )
        }

        if (showAddDialog) {
            AddUserDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { name, email, password, role ->
                    val newUser = User(
                        userId = "USER-${kotlin.random.Random.nextInt(1000, 9999)}",
                        username = name,
                        role = role,
                        email = email,
                        password = password
                    )
                    viewModel.addUser(newUser)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun UserItem(user: User, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(user.username, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(user.email, fontSize = 14.sp, color = Color.Gray)
                Text(
                    text = if (user.role == UserRole.ADMIN) "مدير" else "موظف",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            if (user.role != UserRole.ADMIN) { // منع حذف المدير الأساسي
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red)
                }
            }
        }
    }
}

@Composable
fun AddUserDialog(onDismiss: () -> Unit, onAdd: (String, String, String, UserRole) -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(UserRole.EMPLOYEE) }

    val emailRegex = remember { Regex("^[A-Za-z0-9+_.-]+@([A-Za-z0-9-]+\\.)+[A-Za-z]{2,}$") }
    val isEmailError = email.trim().isNotEmpty() && !emailRegex.matches(email.trim())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة موظف جديد") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("الاسم") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = email, 
                    onValueChange = { email = it }, 
                    label = { Text("البريد الإلكتروني") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isEmailError,
                    supportingText = {
                        if (isEmailError) Text("يرجى إدخال بريد إلكتروني صحيح", color = Color.Red)
                    }
                )
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("كلمة المرور (الكود)") }, modifier = Modifier.fillMaxWidth())
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = role == UserRole.EMPLOYEE, onClick = { role = UserRole.EMPLOYEE })
                    Text("موظف")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = role == UserRole.ADMIN, onClick = { role = UserRole.ADMIN })
                    Text("مدير")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank() && email.isNotBlank() && password.isNotBlank() && !isEmailError) onAdd(name, email, password, role) },
                enabled = name.isNotBlank() && email.isNotBlank() && password.isNotBlank() && !isEmailError
            ) {
                Text("إضافة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
