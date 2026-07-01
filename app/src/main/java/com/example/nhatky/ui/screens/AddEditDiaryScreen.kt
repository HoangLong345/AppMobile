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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
    var tagsString by remember { mutableStateOf("") }
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var existingMediaUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(value = false) }

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
                tagsString = diary.tags.joinToString(", ")
                existingMediaUrls = diary.mediaUrls
            }
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        if (diaryId == null) "Viết Nhật Ký" else "Chỉnh Sửa",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    ) 
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.padding(start = 8.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(end = 16.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        TextButton(
                            onClick = {
                                user?.uid?.let { uid ->
                                    isLoading = true
                                    val tags = tagsString.split(",").asSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
                                    diaryViewModel.addOrUpdateDiary(
                                        diaryId = diaryId,
                                        userId = uid,
                                        title = title,
                                        content = content,
                                        mood = mood,
                                        tags = tags,
                                        imageUris = selectedImageUris,
                                        existingMediaUrls = existingMediaUrls
                                    ) { success ->
                                        isLoading = false
                                        if (success) onBack()
                                    }
                                }
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Lưu", style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold))
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Bạn cảm thấy thế nào?",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            MoodSelector(
                selectedMood = mood
            ) {
                mood = it
            }

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    TextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("Tiêu đề của bạn...", style = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    TextField(
                        value = content,
                        onValueChange = { content = it },
                        placeholder = { Text("Hôm nay của bạn có gì đặc biệt không?", style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 350.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                OutlinedTextField(
                    value = tagsString,
                    onValueChange = { tagsString = it },
                    label = { Text("Thêm thẻ (vd: gia đình, du lịch...)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Hình ảnh kỷ niệm", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    FilledTonalIconButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add media")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                AnimatedVisibility(
                    visible = (existingMediaUrls + selectedImageUris).isNotEmpty(),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().height(130.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(existingMediaUrls) { url ->
                            MediaThumbnail(model = url) {
                                existingMediaUrls -= url
                            }
                        }
                        items(selectedImageUris) { uri ->
                            MediaThumbnail(model = uri) {
                                selectedImageUris -= uri
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun MoodSelector(selectedMood: String, onMoodSelected: (String) -> Unit) {
    val moods = listOf(
        "Vui" to "😊",
        "Bình thường" to "😐",
        "Buồn" to "😔",
        "Tức giận" to "😠"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        moods.forEach { (name, emoji) ->
            val isSelected = selectedMood == name
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.2f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "MoodScale"
            )
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onMoodSelected(name) }
                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .scale(scale)
            ) {
                Text(text = emoji, fontSize = 36.sp)
                AnimatedVisibility(visible = isSelected) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun MediaThumbnail(model: Any, onRemove: () -> Unit) {
    var isRemoving by remember { mutableStateOf(value = false) }
    
    AnimatedVisibility(
        visible = !isRemoving,
        exit = fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.5f)
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(RoundedCornerShape(20.dp))
        ) {
            AsyncImage(
                model = model,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(24.dp)
                    .clickable { 
                        isRemoving = true
                        onRemove() 
                    },
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.5f)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.padding(4.dp))
            }
        }
    }
    
    // Explicitly use isRemoving to avoid warning, though AnimatedVisibility handles it
    if (isRemoving) {
        SideEffect {
            // isRemoving logic
        }
    }
}
