package com.example.nhatky.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.nhatky.viewmodel.AuthViewModel
import com.example.nhatky.viewmodel.DiaryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryListScreen(authViewModel: AuthViewModel, diaryViewModel: DiaryViewModel) {
    val user by authViewModel.currentUser.collectAsState()
    val diaries by diaryViewModel.diaries.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(user) {
        user?.uid?.let { diaryViewModel.loadDiaries(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nhật Ký của tôi") },
                actions = {
                    IconButton(onClick = { authViewModel.logout() }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
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
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(diary.content)
                    }
                }
            }
        }

        if (showDialog) {
            AddDiaryDialog(
                onDismiss = { showDialog = false },
                onAdd = { title, content ->
                    user?.uid?.let { diaryViewModel.addDiary(it, title, content) }
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun AddDiaryDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Viết nhật ký mới") },
        text = {
            Column {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Tiêu đề") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Nội dung") })
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(title, content) }) { Text("Thêm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}
