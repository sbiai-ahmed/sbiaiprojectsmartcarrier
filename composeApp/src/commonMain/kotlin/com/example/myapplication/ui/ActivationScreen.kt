package com.example.myapplication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.database.AppRepository
import kotlinx.coroutines.launch

@Composable
fun ProfessionalActivationScreen() {
    var code by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var isActivating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)) 
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(100.dp),
            shape = CircleShape,
            color = Color(0xFFFFEBEE)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.padding(24.dp).size(48.dp),
                tint = Color(0xFFD32F2F)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "النسخة التجريبية انتهت",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF212121)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "للاستمرار في استخدام النظام وإدارة عملياتك، يرجى إدخال كود التفعيل الذي حصلت عليه من المبرمج.",
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            color = Color(0xFF757575)
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = code,
            onValueChange = { 
                code = it
                isError = false 
            },
            placeholder = { Text("مثال: sbiai XXXX XXXX") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            isError = isError,
            trailingIcon = {
                if (isError) Icon(Icons.Default.Error, "Error", tint = Color.Red)
            }
        )

        if (isError) {
            Text(
                text = "الكود غير صحيح، يرجى التحقق وإعادة المحاولة",
                color = Color.Red,
                modifier = Modifier.padding(top = 8.dp),
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                isActivating = true
                scope.launch {
                    val success = AppRepository.activateApp(code)
                    if (!success) {
                        isError = true
                    }
                    isActivating = false
                }
            },
            enabled = !isActivating && code.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
        ) {
            if (isActivating) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("تفعيل النسخة الكاملة", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = {
            val phone = "213667186465"
            val message = "مرحبا أخي سبيعي، من فضلك أريد الحصول على كود تفعيل تطبيق PROTELE"
            val encodedMsg = message.replace(" ", "%20").replace("،", "%D8%8C")
            val url = "https://wa.me/$phone?text=$encodedMsg"
            try {
                uriHandler.openUri(url)
            } catch (_: Exception) {
                // في حال الفشل
            }
        }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("طلب كود التفعيل من المبرمج عبر واتساب")
            }
        }
    }
}
