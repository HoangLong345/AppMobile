package com.example.nhatky.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nhatky.data.model.DiaryEntry
import com.example.nhatky.ui.components.PaperSurface
import com.example.nhatky.ui.components.PolaroidFrame
import com.example.nhatky.ui.components.WashiTape
import com.example.nhatky.ui.theme.WashiPink
import com.example.nhatky.viewmodel.AuthViewModel
import com.example.nhatky.viewmodel.DiaryUiState
import com.example.nhatky.viewmodel.DiaryViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryListScreen(
    authViewModel: AuthViewModel,
    diaryViewModel: DiaryViewModel,
    onAddDiary: () -> Unit,
    onEditDiary: (String) -> Unit,
) {
    val user by authViewModel.currentUser.collectAsState()
    val uiState by diaryViewModel.uiState.collectAsState()
    val searchQuery by diaryViewModel.searchQuery.collectAsState()

    LaunchedEffect(user) {
        user?.uid?.let { diaryViewModel.loadDiaries(it) }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Color.Transparent)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Cuốn sổ của bạn",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { authViewModel.logout() },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            .size(44.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                
                SearchBar(query = searchQuery) { query ->
                    user?.uid?.let { uid -> diaryViewModel.onSearchQueryChange(query, uid) }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddDiary,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 16.dp, end = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(32.dp))
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        PaperSurface(modifier = Modifier.padding(padding)) {
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = uiState,
                    transitionSpec = {
                        fadeIn(tween(400)) togetherWith fadeOut(tween(400))
                    },
                    label = "UIStateTransition"
                ) { state ->
                    when (state) {
                        is DiaryUiState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        is DiaryUiState.Success -> {
                            if (state.diaries.isEmpty()) {
                                EmptyStateView()
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(24.dp)
                                ) {
                                    itemsIndexed(state.diaries, key = { _, diary -> diary.id }) { index, diary ->
                                        var visible by remember { mutableStateOf(false) }
                                        LaunchedEffect(Unit) {
                                            delay(index * 100L)
                                            visible = true
                                        }
                                        
                                        AnimatedVisibility(
                                            visible = visible,
                                            enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 3 },
                                            exit = fadeOut(tween(600)),
                                        ) {
                                            DiaryScrapItem(
                                                diary = diary,
                                                index = index,
                                                onClick = { onEditDiary(diary.id) },
                                                onDelete = { diaryViewModel.deleteDiary(diary.id) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        is DiaryUiState.Error -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(text = "Lỗi: ${state.message}", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                
                // Page Curl Visual Hint (Bottom Right)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(60.dp)
                        .alpha(0.3f)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color.Transparent, Color.Gray),
                                start = androidx.compose.ui.geometry.Offset.Zero,
                                end = androidx.compose.ui.geometry.Offset.Infinite
                            )
                        )
                        .rotate(45f)
                )
            }
        }
    }
}

@Composable
fun DiaryScrapItem(
    diary: DiaryEntry,
    index: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val rotation = remember { (index % 3 - 1) * 2f } // Subtle random rotation
    val dateFormat = remember { SimpleDateFormat("EEEE, d MMMM, yyyy", Locale("vi", "VN")) }
    val timeString = dateFormat.format(Date(diary.timestamp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // "Taped" Note
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .rotate(rotation)
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.9f))
                    .clickable(onClick = onClick)
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = diary.title.uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(text = getMoodEmoji(diary.mood), fontSize = 24.sp)
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = diary.content,
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                    color = Color.DarkGray,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = timeString,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = Color.Gray
                    )
                )
            }

            // Decorative Tape
            WashiTape(
                color = WashiPink,
                rotation = -15f,
                modifier = Modifier.align(Alignment.TopCenter).offset(y = (-10).dp)
            )
            
            // Delete button "pinned"
            IconButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.BottomEnd).offset(x = 12.dp, y = 12.dp)
            ) {
                Icon(Icons.Default.Clear, contentDescription = "Delete", tint = Color.LightGray, modifier = Modifier.size(16.dp))
            }
        }
        
        if (diary.mediaUrls.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            PolaroidFrame(
                imageUrl = diary.mediaUrls.first(),
                rotation = -rotation * 2,
                modifier = Modifier
                    .width(180.dp)
                    .align(if (index % 2 == 0) Alignment.Start else Alignment.End)
            )
        }
    }
}

private fun getMoodEmoji(mood: String): String = when(mood) {
    "Vui" -> "😊"
    "Bình thường" -> "😐"
    "Buồn" -> "😔"
    "Tức giận" -> "😠"
    else -> "📝"
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Tìm kiếm kỷ niệm...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            singleLine = true
        )
    }
}

@Composable
fun EmptyStateView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.FavoriteBorder,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Trang giấy còn trống",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Hãy bắt đầu viết nên những kỷ niệm của bạn ngay hôm nay.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
