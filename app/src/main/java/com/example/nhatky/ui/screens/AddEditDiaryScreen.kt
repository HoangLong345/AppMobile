package com.example.nhatky.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.nhatky.ui.components.PaperSurface
import com.example.nhatky.ui.components.PolaroidFrame
import com.example.nhatky.viewmodel.AuthViewModel
import com.example.nhatky.viewmodel.DiaryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDiaryScreen(
    diaryId: String?,
    authViewModel: AuthViewModel,
    diaryViewModel: DiaryViewModel,
    onBack: () -> Unit,
) {
    val user by authViewModel.currentUser.collectAsState()
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var mood by remember { mutableStateOf("Bình thường") }
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var existingMediaUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
    ) { uris ->
        selectedImageUris += uris
    }

    LaunchedEffect(diaryId) {
        if (diaryId != null) {
            isLoading = true
            val diary = diaryViewModel.getDiaryById(diaryId)
            if (diary != null) {
                title = diary.title
                content = diary.content
                mood = diary.mood
                existingMediaUrls = diary.mediaUrls
            }
            isLoading = false
        }
    }

    PaperSurface(showGrid = false) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            if (diaryId == null) "Kỷ niệm mới" else "Chỉnh sửa",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                user?.uid?.let { uid ->
                                    isLoading = true
                                    diaryViewModel.addOrUpdateDiary(
                                        diaryId = diaryId,
                                        userId = uid,
                                        title = title,
                                        content = content,
                                        mood = mood,
                                        tags = emptyList(),
                                        imageUris = selectedImageUris,
                                        existingMediaUrls = existingMediaUrls
                                    ) { success ->
                                        isLoading = false
                                        if (success) onBack()
                                    }
                                }
                            }
                        ) {
                            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            else Text("Lưu", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    MoodSelectorSmall(selectedMood = mood) { mood = it }
                    
                    Spacer(modifier = Modifier.height(32.dp))

                    TextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("Tiêu đề...", style = MaterialTheme.typography.headlineMedium.copy(color = Color.LightGray)) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    TextField(
                        value = content,
                        onValueChange = { content = it },
                        placeholder = { Text("Bạn đang nghĩ gì?", style = MaterialTheme.typography.bodyLarge.copy(color = Color.Gray)) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 400.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Media Display
                    if ((existingMediaUrls + selectedImageUris).isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(bottom = 120.dp)
                        ) {
                            items(existingMediaUrls) { url ->
                                PolaroidFrame(imageUrl = url, modifier = Modifier.width(150.dp))
                            }
                            items(selectedImageUris) { uri ->
                                PolaroidFrame(imageUrl = uri.toString(), modifier = Modifier.width(150.dp))
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(120.dp))
                    }
                }

                // Floating Action Dock
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .shadow(elevation = 12.dp, shape = RoundedCornerShape(32.dp)),
                        color = Color.White,
                        shape = RoundedCornerShape(32.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            DockIcon(icon = Icons.Default.Edit, onClick = { /* Drawing tool */ })
                            DockIcon(icon = Icons.Default.AccountBox, onClick = { /* Camera alternative */ })
                            DockIcon(icon = Icons.Default.Face, onClick = { imagePickerLauncher.launch("image/*") })
                            DockIcon(icon = Icons.Default.Menu, onClick = { /* Text tool */ })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DockIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .background(Color.Transparent, CircleShape)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
    }
}

@Composable
fun MoodSelectorSmall(selectedMood: String, onMoodSelected: (String) -> Unit) {
    val moods = listOf("Vui" to "😊", "Bình thường" to "😐", "Buồn" to "😔", "Tức giận" to "😠")
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        moods.forEach { (name, emoji) ->
            val isSelected = selectedMood == name
            Text(
                text = emoji,
                fontSize = if (isSelected) 32.sp else 24.sp,
                modifier = Modifier
                    .clickable { onMoodSelected(name) }
                    .alpha(if (isSelected) 1f else 0.5f)
            )
        }
    }
}
