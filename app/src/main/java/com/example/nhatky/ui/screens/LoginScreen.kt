package com.example.nhatky.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.nhatky.R
import com.example.nhatky.ui.components.AppBackground
import com.example.nhatky.ui.components.NhatKyButton
import com.example.nhatky.ui.components.NhatKyTextField
import com.example.nhatky.ui.theme.SunsetGradientEnd
import com.example.nhatky.ui.theme.SunsetGradientStart
import com.example.nhatky.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    
    val context = LocalContext.current

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken != null) {
                    isLoading = true
                    viewModel.signInWithGoogle(idToken) { success, error ->
                        isLoading = false
                        if (success) onLoginSuccess()
                        else Toast.makeText(context, "Lỗi Firebase: $error", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                isLoading = false
                val errorMessage = if (e is ApiException) {
                    when (e.statusCode) {
                        10 -> "Lỗi cấu hình (DEVELOPER_ERROR). Hãy kiểm tra SHA-1 và google-services.json."
                        7 -> "Lỗi mạng. Vui lòng kiểm tra kết nối internet."
                        12500 -> "Lỗi phiên bản Google Play Services hoặc cấu hình không khớp."
                        else -> "Lỗi Google Sign In (${e.statusCode}): ${e.localizedMessage}"
                    }
                } else {
                    "Lỗi không xác định: ${e.localizedMessage}"
                }
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            }
        } else {
            isLoading = false
        }
    }

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo Circle
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .shadow(elevation = 12.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                    contentDescription = "App Logo",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Start Your Journal",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Đăng nhập để xem nhật ký của bạn",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 40.dp)
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(24.dp))
                    .background(Color.White, RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                NhatKyTextField(
                    value = identifier,
                    onValueChange = { identifier = it },
                    label = "Email hoặc Số điện thoại",
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                NhatKyTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Mật khẩu",
                    visualTransformation = PasswordVisualTransformation(),
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                )

                TextButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Quên mật khẩu?", color = MaterialTheme.colorScheme.secondary)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            NhatKyButton(
                text = "Đăng nhập",
                isLoading = isLoading,
                onClick = {
                    isLoading = true
                    viewModel.login(identifier, password) { success, error ->
                        isLoading = false
                        if (success) onLoginSuccess()
                        else Toast.makeText(context, "Lỗi: $error", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    isLoading = true
                    val webClientId = try {
                        val idRes = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
                        if (idRes != 0) context.getString(idRes) else null
                    } catch (_: Exception) { null }

                    if (webClientId == null) {
                        isLoading = false
                        Toast.makeText(context, "Lỗi cấu hình Google Sign-In.", Toast.LENGTH_LONG).show()
                    } else {
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(webClientId)
                            .requestEmail()
                            .build()
                        val googleSignInClient = GoogleSignIn.getClient(context, gso)
                        googleSignInClient.signOut().addOnCompleteListener {
                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Đăng nhập với Google", fontWeight = FontWeight.SemiBold)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            TextButton(onClick = onNavigateToRegister) {
                Text(text = "Chưa có tài khoản? Đăng ký ngay", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Khôi phục mật khẩu") },
            text = {
                Column {
                    Text("Nhập email của bạn để nhận mã xác nhận khôi phục mật khẩu.")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.sendPasswordResetEmail(resetEmail) { success, message ->
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            if (success) showResetDialog = false
                        }
                    }
                ) {
                    Text("Gửi mã")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Hủy")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }
}
