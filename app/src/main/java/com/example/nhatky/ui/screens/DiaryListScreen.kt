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
import com.example.nhatky.viewmodel.AuthViewModel
import com.example.nhatky.viewmodel.DiaryViewModel
import java.io.ByteArrayOutputStream

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
                                model = url,
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth().height(200.dp).padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        if (showDialog) {
            AddDiaryDialog(
                onDismiss = { showDialog = false },
                onAdd = { title, content, mood, tags, imageUri ->
                    user?.uid?.let { diaryViewModel.addDiary(it, title, content, mood, tags, imageUri) }
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun AddDiaryDialog(onDismiss: () -> Unit, onAdd: (String, String, String, List<String>, Uri?) -> Unit) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var mood by remember { mutableStateOf("Bình thường") }
    var tagsString by remember { mutableStateOf("") }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    val context = LocalContext.current
    
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            capturedImageUri = getImageUriFromBitmap(context, bitmap)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Viết nhật ký mới") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Tiêu đề") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Nội dung") })
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
                    placeholder = { Text("gia đình, học tập...") }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(onClick = { cameraLauncher.launch() }) {
                    Text("Chụp ảnh")
                }
                
                capturedBitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(100.dp).padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
                val tags = tagsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                onAdd(title, content, mood, tags, capturedImageUri) 
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
