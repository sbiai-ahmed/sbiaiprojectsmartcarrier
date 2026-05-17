package com.example.myapplication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.database.AppRepository
import com.example.myapplication.models.User
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoginSuccess: (User) -> Unit, onNavigateToRegister: () -> Unit) {
    val scope = rememberCoroutineScope()
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Regex للتحقق من تنسيق البريد الإلكتروني
    val emailRegex = remember { Regex("^[A-Za-z0-9+_.-]+@([A-Za-z0-9-]+\\.)+[A-Za-z]{2,}\$") }
    val isEmailError = emailInput.trim().isNotEmpty() && !emailRegex.matches(emailInput.trim())

    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Smartphone,
                        "Logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(50.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "PRO TELEcom",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("صيانة وبيع الهواتف الذكية", fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(32.dp))

                if (errorMessage != null) {
                    Text(errorMessage!!, color = Color.Red, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { 
                        emailInput = it
                        errorMessage = null 
                    },
                    placeholder = { Text("البريد الإلكتروني") },
                    leadingIcon = { Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    isError = isEmailError || errorMessage != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    supportingText = { 
                        if (isEmailError) Text("تنسيق البريد الإلكتروني غير صحيح", color = Color.Red) 
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { 
                        passwordInput = it
                        errorMessage = null 
                    },
                    placeholder = { Text("كلمة المرور") },
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(image, null, tint = Color.Gray)
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = errorMessage != null
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val enteredEmail = emailInput.trim().lowercase()
                        val enteredPassword = passwordInput.trim()
                        
                        if (enteredEmail.isBlank() || enteredPassword.isBlank()) {
                            errorMessage = "يرجى ملء جميع الحقول"
                            return@Button
                        }

                        isLoading = true
                        errorMessage = null
                        
                        scope.launch {
                            try {
                                val success = AppRepository.loginUser(enteredEmail, enteredPassword)
                                if (success) {
                                    val user = AppRepository.currentUser
                                    if (user != null) {
                                        onLoginSuccess(user)
                                    } else {
                                        errorMessage = "فشل في استرداد بيانات المستخدم"
                                    }
                                } else {
                                    errorMessage = "كلمة المرور أو البريد الإلكتروني غير صحيح"
                                }
                            } catch (e: Exception) {
                                errorMessage = "خطأ في الاتصال: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading && !isEmailError,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("تسجيل الدخول", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(onClick = onNavigateToRegister) {
                    Text("ليس لديك حساب؟ إنشاء حساب مدير جديد", color = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(24.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "SBIAI AHMED",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary.copy(0.8f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone, null, modifier = Modifier.size(10.dp), tint = Color.Gray)
                        Spacer(Modifier.width(4.dp))
                        Text("0667186465", fontSize = 10.sp, color = Color.Gray)
                    }
                    Text("v1.0.0", fontSize = 9.sp, color = Color.LightGray)
                }
            }
        }
    }
}
