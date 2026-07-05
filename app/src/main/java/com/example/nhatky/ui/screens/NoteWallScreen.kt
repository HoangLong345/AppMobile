package com.example.nhatky.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nhatky.data.model.DiaryEntry
import com.example.nhatky.ui.components.NoteDetailDialog
import com.example.nhatky.ui.components.StickyNote
import com.example.nhatky.ui.components.WallBackground
import com.example.nhatky.viewmodel.DiaryUiState
import com.example.nhatky.viewmodel.DiaryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteWallScreen(
    dateKey: String,
    diaryViewModel: DiaryViewModel,
    onEditNote: (String) -> Unit,
    onBack: () -> Unit
) {
    val uiState by diaryViewModel.uiState.collectAsState()
    var selectedNote by remember { mutableStateOf<DiaryEntry?>(null) }
    
    val displayDate = remember(dateKey) {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("EEEE, d MMMM, yyyy", Locale("vi", "VN"))
            val date = inputFormat.parse(dateKey)
            date?.let { outputFormat.format(it) } ?: dateKey
        } catch (_: Exception) { dateKey }
    }

    WallBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(displayDate, fontWeight = FontWeight.Bold) },
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
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                when (val state = uiState) {
                    is DiaryUiState.SuccessGrouped -> {
                        val notes = state.groupedDiaries[dateKey]?.sortedBy { it.timestamp } ?: emptyList()
                        
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 32.dp)
                        ) {
                            notes.forEachIndexed { index, note ->
                                val randomValues = remember(note.id) {
                                    val random = java.util.Random(note.id.hashCode().toLong())
                                    val x = random.nextInt(60) - 30
                                    val yShift = random.nextInt(40)
                                    Pair(x, yShift)
                                }
                                val offsetX = randomValues.first
                                val offsetY = index * 140 + randomValues.second
                                
                                Box(
                                    modifier = Modifier
                                        .offset(x = offsetX.dp, y = offsetY.dp)
                                        .align(if (index % 2 == 0) Alignment.TopStart else Alignment.TopEnd)
                                        .padding(horizontal = 20.dp)
                                ) {
                                    StickyNote(diary = note, onClick = { selectedNote = note })
                                }
                            }
                            Spacer(modifier = Modifier.height((notes.size * 140 + 200).dp))
                        }
                    }
                    else -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }

    selectedNote?.let { note ->
        NoteDetailDialog(
            diary = note,
            onDismiss = { selectedNote = null },
            onEdit = { onEditNote(note.id) },
            onDelete = { 
                diaryViewModel.deleteDiary(note.id)
                selectedNote = null
            }
        )
    }
}
