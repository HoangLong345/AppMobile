package com.example.nhatky.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.CachePolicy
import coil.size.Size
import com.example.nhatky.viewmodel.AuthViewModel
import com.example.nhatky.viewmodel.DiaryViewModel
import com.example.nhatky.data.model.DiaryEntry
import java.io.ByteArrayOutputStream
import android.content.ContentResolver
import android.webkit.MimeTypeMap
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.filled.Close

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryListScreen(authViewModel: AuthViewModel, diaryViewModel: DiaryViewModel) {
    val user by authViewModel.currentUser.collectAsState()
    val diaries by diaryViewModel.diaries.collectAsState()
    val searchQuery by diaryViewModel.searchQuery.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(user) {
        user?.uid?.let { diaryViewModel.loadDiaries(it) }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Nhật Ký của tôi") },
                    actions = {
                        IconButton(onClick = { authViewModel.logout() }) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                        }
                    }
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { user?.uid?.let { uid -> diaryViewModel.onSearchQueryChange(it, uid) } },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Tìm kiếm theo tiêu đề, nội dung, thẻ...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Diary")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(diaries) { diary ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(diary.title, style = MaterialTheme.typography.titleLarge)
                            IconButton(onClick = { diaryViewModel.deleteDiary(diary.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                        Text("Cảm xúc: ${diary.mood}", style = MaterialTheme.typography.bodySmall)
                        if (diary.tags.isNotEmpty()) {
                            Text("Thẻ: ${diary.tags.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(diary.content)
                        
                        diary.mediaUrls.forEach { url ->
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(url)
                                    .crossfade(true)
                                    .size(Size.ORIGINAL) // Or a specific size if you want to cap it
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        if (showDialog) {
            AddDiaryDialog(
                onDismiss = { showDialog = false },
                onAdd = { title, content, mood, tags, mediaItems ->
                    user?.uid?.let { diaryViewModel.addDiaryWithMedia(it, title, content, mood, tags, mediaItems) }
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun AddDiaryDialog(onDismiss: () -> Unit, onAdd: (String, String, String, List<String>, List<Pair<Uri, Boolean>>) -> Unit) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var mood by remember { mutableStateOf("Bình thường") }
    var tagsString by remember { mutableStateOf("") }

    // Danh sách lưu trữ các tệp media đã chọn: Pair(Uri, IsVideo)
    val chosenMediaList = remember { mutableStateListOf<Pair<Uri, Boolean>>() }
    val context = LocalContext.current

    // Hàm phụ kiểm tra xem một Uri có phải là Video hay không
    fun isVideoUri(uri: Uri): Boolean {
        return context.contentResolver.getType(uri)?.startsWith("video") == true
    }

    // 1. Launcher chụp ảnh từ Camera
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val uri = getImageUriFromBitmap(context, bitmap) // Hàm helper chuyển đổi bitmap có sẵn của bạn
            chosenMediaList.add(Pair(uri, false))
        }
    }

    // 2. Launcher chọn ảnh bằng PhotoPicker (Hỗ trợ chọn nhiều hoặc 1 ảnh)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris ->
        uris.forEach { uri ->
            chosenMediaList.add(Pair(uri, isVideoUri(uri)))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Viết nhật ký mới") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Tiêu đề") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Nội dung") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))

                Text("Cảm xúc hôm nay:")
                Row {
                    listOf("Vui", "Bình thường", "Buồn").forEach { m ->
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            RadioButton(selected = mood == m, onClick = { mood = m })
                            Text(m)
                        }
                    }
                }

                OutlinedTextField(
                    value = tagsString,
                    onValueChange = { tagsString = it },
                    label = { Text("Thẻ (phân cách bằng dấu phẩy)") },
                    placeholder = { Text("gia đình, học tập...") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Các nút điều khiển chọn phương tiện truyền thông
                Text("Đính kèm phương tiện:", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = { cameraLauncher.launch() }, modifier = Modifier.weight(1f)) {
                        Text("Chụp ảnh", style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Chọn tệp", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Hiển thị danh sách ảnh/video preview đã chọn dạng lưới nhỏ gọn
                if (chosenMediaList.isNotEmpty()) {
                    Text("Đã chọn (${chosenMediaList.size}):", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))

                    Box(modifier = Modifier.height(120.dp).fillMaxWidth()) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(chosenMediaList) { (uri, isVideo) ->
                                Box(modifier = Modifier.size(100.dp)) {
                                    if (isVideo) {
                                        // Preview đơn giản cho Video (Hiển thị icon hoặc text video)
                                        Surface(
                                            modifier = Modifier.fillMaxSize(),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                                                Text("📹 Video", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    } else {
                                        // Preview cho Ảnh bằng AsyncImage của Coil
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(uri)
                                                .crossfade(true)
                                                .size(200, 200) // Resize for small preview
                                                .memoryCachePolicy(CachePolicy.ENABLED)
                                                .build(),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    // Nút xóa nhanh phương tiện khỏi danh sách chọn
                                    FilledIconButton(
                                        onClick = { chosenMediaList.remove(Pair(uri, isVideo)) },
                                        modifier = Modifier.size(24.dp).align(androidx.compose.ui.Alignment.TopEnd),
                                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Xóa", modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val tags = tagsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                onAdd(title, content, mood, tags, chosenMediaList.toList())
            }) { Text("Thêm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

// Helper function to convert Bitmap to Uri
fun getImageUriFromBitmap(context: Context, bitmap: Bitmap): Uri {
    val bytes = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
    val path = MediaStore.Images.Media.insertImage(context.contentResolver, bitmap, "DiaryImage_${System.currentTimeMillis()}", null)
    return Uri.parse(path)
}
