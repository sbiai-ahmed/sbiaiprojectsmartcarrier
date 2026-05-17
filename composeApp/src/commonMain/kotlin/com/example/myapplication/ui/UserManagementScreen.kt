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
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    val state = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                Icon(Icons.Default.PersonAdd, contentDescription = "إضافة موظف")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading && users.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    state = state,
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(users, key = { it.userId }) { user ->
                        UserItem(user = user, onDelete = { viewModel.deleteUser(user.userId) })
                    }
                }
            }
            AppScrollbar(
                state = state,
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
            )
        }

        if (showAddDialog) {
            AddUserDialog(
                isLoading = isLoading,
                onDismiss = { showAddDialog = false },
                onAdd = { name, email, password, role ->
                    viewModel.registerEmployee(name, email, password, role) {
                        showAddDialog = false
                    }
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
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(8.dp),
                color = if (user.role == UserRole.ADMIN) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(
                    imageVector = if (user.role == UserRole.ADMIN) Icons.Default.AdminPanelSettings else Icons.Default.Badge,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp),
                    tint = if (user.role == UserRole.ADMIN) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(user.username, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(user.email, fontSize = 12.sp, color = Color.Gray)
                Text(
                    text = if (user.role == UserRole.ADMIN) "مدير" else "موظف",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            if (user.role != UserRole.ADMIN) { 
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
fun AddUserDialog(isLoading: Boolean, onDismiss: () -> Unit, onAdd: (String, String, String, UserRole) -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(UserRole.EMPLOYEE) }

    val emailRegex = remember { Regex("^[A-Za-z0-9+_.-]+@([A-Za-z0-9-]+\\.)+[A-Za-z]{2,}$") }
    val isEmailError = email.trim().isNotEmpty() && !emailRegex.matches(email.trim())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة موظف جديد للمحل") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name, 
                    onValueChange = { name = it }, 
                    label = { Text("الاسم الكامل") }, 
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = email, 
                    onValueChange = { email = it }, 
                    label = { Text("البريد الإلكتروني") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    isError = isEmailError,
                    supportingText = {
                        if (isEmailError) Text("يرجى إدخال بريد إلكتروني صحيح", color = Color.Red)
                    }
                )
                OutlinedTextField(
                    value = password, 
                    onValueChange = { password = it }, 
                    label = { Text("كلمة المرور للموظف") }, 
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                
                Text("نوع الصلاحية:", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = role == UserRole.EMPLOYEE, onClick = { role = UserRole.EMPLOYEE })
                    Text("موظف")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = role == UserRole.ADMIN, onClick = { role = UserRole.ADMIN })
                    Text("مدير مساعد")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank() && email.isNotBlank() && password.isNotBlank() && !isEmailError) onAdd(name, email, password, role) },
                enabled = !isLoading && name.isNotBlank() && email.isNotBlank() && password.isNotBlank() && !isEmailError
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                else Text("تسجيل الموظف")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) { Text("إلغاء") }
        }
    )
}
