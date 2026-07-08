package com.example.nhatky.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nhatky.ui.utils.checkAndRequestDrivePermission
import com.example.nhatky.ui.utils.rememberDrivePermissionLauncher
import com.example.nhatky.viewmodel.AuthViewModel
import com.example.nhatky.viewmodel.DiaryViewModel
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDiaryScreen(
    diaryId: String?,
    authViewModel: AuthViewModel,
    diaryViewModel: DiaryViewModel,
    onBack: () -> Unit,
) {
    val user by authViewModel.currentUser.collectAsState()
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var mood by remember { mutableStateOf("Bình thường") }
    var isLoading by remember { mutableStateOf(false) }

    val drivePermissionLauncher = rememberDrivePermissionLauncher { success ->
        if (success) {
            // Re-trigger save or just notify
            Toast.makeText(context, "Đã cấp quyền Google Drive. Vui lòng nhấn Lưu lại.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Cần quyền Google Drive để lưu ảnh/video.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(diaryId) {
        if (diaryId != null) {
            isLoading = true
            val diary = diaryViewModel.getDiaryById(diaryId)
            if (diary != null) {
                title = diary.title
                content = diary.content
                mood = diary.mood
            }
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (diaryId == null) "Viết nhật ký" else "Chỉnh sửa",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Serif,
                            color = Color.Black
                        )
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            checkAndRequestDrivePermission(context, drivePermissionLauncher) {
                                user?.uid?.let { uid ->
                                    isLoading = true
                                    diaryViewModel.addOrUpdateDiary(
                                        diaryId = diaryId,
                                        userId = uid,
                                        title = title,
                                        content = content,
                                        mood = mood,
                                        tags = emptyList(),
                                        imageUris = emptyList(),
                                        existingMediaUrls = emptyList()
                                    ) { success ->
                                        isLoading = false
                                        if (success) onBack()
                                    }
                                }
                            }
                        },
                        enabled = !isLoading && content.isNotEmpty()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(
                                "Lưu", 
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (content.isNotEmpty()) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color(0xFFFEFBF3) // Paper background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(horizontal = 24.dp)
        ) {
            // FIXED HEADER AREA
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = "Tâm trạng hiện tại",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                MoodSelectorModern(selectedMood = mood) { mood = it }
                
                Spacer(modifier = Modifier.height(24.dp))

                TextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { 
                        Text(
                            "Tiêu đề của bạn...", 
                            style = MaterialTheme.typography.headlineSmall.copy(color = Color.Gray.copy(alpha = 0.5f))
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = Color.Black
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    ),
                    singleLine = true
                )
                
                HorizontalDivider(
                    color = Color.Black.copy(alpha = 0.1f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            // SCROLLABLE CONTENT AREA
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                TextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { 
                        Text(
                            "Hôm nay của bạn thế nào?", 
                            style = MaterialTheme.typography.bodyLarge.copy(color = Color.Gray.copy(alpha = 0.5f))
                        ) 
                    },
                    modifier = Modifier.fillMaxSize(),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 32.sp,
                        fontFamily = FontFamily.Serif,
                        color = Color.Black
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun MoodSelectorModern(selectedMood: String, onMoodSelected: (String) -> Unit) {
    val moods = listOf(
        "Vui" to "😊", 
        "Bình thường" to "😐", 
        "Buồn" to "😔", 
        "Tức giận" to "😠"
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        moods.forEach { (name, emoji) ->
            val isSelected = selectedMood == name
            Surface(
                onClick = { onMoodSelected(name) },
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                modifier = Modifier.weight(1f),
                border = if (isSelected) 
                    androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
                else 
                    androidx.compose.foundation.BorderStroke(1.dp, Color.Black.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = emoji, fontSize = 28.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.6f),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
