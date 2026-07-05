package com.example.nhatky.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.nhatky.data.model.User
import com.example.nhatky.ui.components.AppBackground
import com.example.nhatky.ui.components.NhatKyButton
import com.example.nhatky.ui.components.NhatKyTextField
import com.example.nhatky.viewmodel.AuthViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(viewModel: AuthViewModel, onRegisterSuccess: () -> Unit, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    AppBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Tạo tài khoản mới", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                NhatKyTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Họ và tên",
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary) }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Box(modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }) {
                    NhatKyTextField(
                        value = dob,
                        onValueChange = { },
                        label = "Ngày sinh",
                        leadingIcon = { Icon(Icons.Default.DateRange, null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                NhatKyTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = "Số điện thoại",
                    leadingIcon = { Icon(Icons.Default.Phone, null, tint = MaterialTheme.colorScheme.primary) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                NhatKyTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
                    leadingIcon = { Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.primary) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                NhatKyTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Mật khẩu",
                    visualTransformation = PasswordVisualTransformation(),
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                NhatKyTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = "Nhập lại mật khẩu",
                    visualTransformation = PasswordVisualTransformation(),
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary) }
                )
                
                Spacer(modifier = Modifier.height(48.dp))

                NhatKyButton(
                    text = "Đăng ký",
                    isLoading = isLoading,
                    onClick = {
                        if (name.isBlank() || dob.isBlank() || phone.isBlank() || email.isBlank() || password.isBlank()) {
                            Toast.makeText(context, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                        } else if (password != confirmPassword) {
                            Toast.makeText(context, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show()
                        } else {
                            isLoading = true
                            val newUser = User(name = name, dob = dob, phoneNumber = phone, email = email)
                            viewModel.register(newUser, password) { success, error ->
                                isLoading = false
                                if (success) onRegisterSuccess()
                                else Toast.makeText(context, "Lỗi: $error", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            dob = formatter.format(Date(it))
                        }
                        showDatePicker = false
                    }
                ) { Text("Chọn") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Hủy") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
