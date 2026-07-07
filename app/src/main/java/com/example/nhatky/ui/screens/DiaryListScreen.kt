package com.example.nhatky.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nhatky.ui.components.CreationHub
import com.example.nhatky.viewmodel.AuthViewModel
import com.example.nhatky.viewmodel.DiaryUiState
import com.example.nhatky.viewmodel.DiaryViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryListScreen(
    authViewModel: AuthViewModel,
    diaryViewModel: DiaryViewModel,
    onAddDiary: () -> Unit,
    onOpenWall: (String) -> Unit,
    onTakePhoto: () -> Unit,
    onPickPhoto: (String) -> Unit,
) {
    val context = LocalContext.current
    val user by authViewModel.currentUser.collectAsState()
    val uiState by diaryViewModel.uiState.collectAsState()
    val searchQuery by diaryViewModel.searchQuery.collectAsState()

    var showCreationHub by remember { mutableStateOf(false) }

    // Gọi thư viện hỗ trợ cả Ảnh và Video, sau đó chuyển hết sang màn hình Preview/Edit
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            val encodedUri = Uri.encode(it.toString())
            onPickPhoto(encodedUri)
        }
    }

    LaunchedEffect(user) {
        user?.uid?.let { diaryViewModel.loadDiaries(it) }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(top = 16.dp, bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Kỷ niệm mỗi ngày",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-1).sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Nhấn vào mỗi ngày để xem chi tiết",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { authViewModel.logout() },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .size(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                SearchBarModern(query = searchQuery) { query ->
                    user?.uid?.let { uid -> diaryViewModel.onSearchQueryChange(query, uid) }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreationHub = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create", modifier = Modifier.size(32.dp))
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .navigationBarsPadding()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = uiState) {
                is DiaryUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is DiaryUiState.SuccessGrouped -> {
                    if (state.groupedDiaries.isEmpty()) {
                        EmptyStateModern()
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 100.dp, start = 20.dp, end = 20.dp, top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(state.groupedDiaries.keys.toList().sortedDescending()) { dateKey ->
                                DailySummaryCard(
                                    dateKey = dateKey,
                                    entryCount = state.groupedDiaries[dateKey]?.size ?: 0,
                                    onClick = { onOpenWall(dateKey) }
                                )
                            }
                        }
                    }
                }
                is DiaryUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Lỗi: ${state.message}", color = MaterialTheme.colorScheme.error)
                    }
                }
                else -> {}
            }
        }
    }

    if (showCreationHub) {
        CreationHub(
            onWriteText = { onAddDiary() },
            onTakePhoto = { onTakePhoto() },
            onImportPhoto = {
                mediaPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
            },
            onDraw = {
                val blankUri = createBlankImageUri(context)
                val encodedUri = Uri.encode(blankUri.toString())
                onPickPhoto(encodedUri)
            },
            onDismiss = { showCreationHub = false }
        )
    }
}

@Composable
fun DailySummaryCard(dateKey: String, entryCount: Int, onClick: () -> Unit) {
    val displayDate = remember(dateKey) {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("EEEE, d MMMM", Locale("vi", "VN"))
            val date = inputFormat.parse(dateKey)
            date?.let { outputFormat.format(it) } ?: dateKey
        } catch (_: Exception) { dateKey }
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = displayDate, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                Text(text = "$entryCount kỷ niệm", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun SearchBarModern(query: String, onQueryChange: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Tìm kiếm kỷ niệm...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
            ),
            singleLine = true
        )
    }
}

@Composable
fun EmptyStateModern() {
    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Favorite, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        Spacer(modifier = Modifier.height(24.dp))
        Text("Chưa có kỷ niệm nào", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

fun createBlankImageUri(context: Context): Uri {
    val width = 1080
    val height = 1440
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)

    val file = File(context.cacheDir, "blank_canvas_${System.currentTimeMillis()}.jpg")
    val out = FileOutputStream(file)
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
    out.flush()
    out.close()

    return Uri.fromFile(file)
}