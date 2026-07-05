package com.example.nhatky.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.nhatky.viewmodel.AuthViewModel
import com.example.nhatky.viewmodel.DiaryViewModel
import java.io.File
import java.io.FileOutputStream

data class DrawPath(
    val path: Path,
    val color: Color,
    val strokeWidth: Float
)

data class TextOverlay(
    val id: Long = System.nanoTime(),
    var text: String,
    var position: Offset,
    var color: Color
)

data class StickerOverlay(
    val id: Long = System.nanoTime(),
    val emoji: String,
    var position: Offset
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditScreen(
    imageUri: Uri,
    authViewModel: AuthViewModel,
    diaryViewModel: DiaryViewModel,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val user by authViewModel.currentUser.collectAsState()
    
    var currentPath by remember { mutableStateOf<DrawPath?>(null) }
    val paths = remember { mutableStateListOf<DrawPath>() }
    val texts = remember { mutableStateListOf<TextOverlay>() }
    val stickers = remember { mutableStateListOf<StickerOverlay>() }
    
    var selectedColor by remember { mutableStateOf(Color.Red) }
    var strokeWidth by remember { mutableStateOf(5f) }
    var editMode by remember { mutableStateOf(EditMode.DRAW) }
    var isLoading by remember { mutableStateOf(false) }
    
    var showTextDialog by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }
    
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val colors = listOf(Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.Black, Color.White)
    val stickerList = listOf("❤️", "⭐", "🔥", "🌈", "🍀", "🌸", "🍦", "🎁", "🐶", "🐱")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chỉnh sửa", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            isLoading = true
                            user?.uid?.let { uid ->
                                // Flatten and save
                                val editedUri = flattenImage(
                                    context = context,
                                    originalUri = imageUri,
                                    paths = paths,
                                    texts = texts,
                                    stickers = stickers,
                                    canvasSize = canvasSize
                                )
                                
                                if (editedUri != null) {
                                    diaryViewModel.addOrUpdateDiary(
                                        userId = uid,
                                        title = "Kỷ niệm ảnh",
                                        content = "Kỷ niệm ảnh đã chỉnh sửa",
                                        mood = "Bình thường",
                                        tags = emptyList(),
                                        imageUris = listOf(editedUri),
                                        onComplete = { success ->
                                            isLoading = false
                                            if (success) onSave()
                                        }
                                    )
                                } else {
                                    isLoading = false
                                }
                            }
                        },
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Lưu", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        EditModeItem(
                            icon = Icons.Default.Edit,
                            isSelected = editMode == EditMode.DRAW,
                            onClick = { editMode = EditMode.DRAW }
                        )
                        EditModeItem(
                            icon = Icons.Default.Info, // Fallback for TextFields
                            isSelected = editMode == EditMode.TEXT,
                            onClick = { 
                                editMode = EditMode.TEXT
                                showTextDialog = true 
                            }
                        )
                        EditModeItem(
                            icon = Icons.Default.Face,
                            isSelected = editMode == EditMode.STICKER,
                            onClick = { editMode = EditMode.STICKER }
                        )
                        IconButton(onClick = { 
                            paths.clear() 
                            texts.clear()
                            stickers.clear()
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Clear all")
                        }
                    }
                    
                    if (editMode == EditMode.DRAW) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            colors.forEach { color ->
                                Surface(
                                    onClick = { selectedColor = color },
                                    shape = CircleShape,
                                    color = color,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .border(
                                            width = if (selectedColor == color) 2.dp else 0.dp,
                                            color = Color.Gray,
                                            shape = CircleShape
                                        )
                                ) {}
                            }
                        }
                    }
                    
                    if (editMode == EditMode.STICKER) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            stickerList.forEach { emoji ->
                                Text(
                                    text = emoji,
                                    fontSize = 24.sp,
                                    modifier = Modifier.clickable {
                                        stickers.add(StickerOverlay(emoji = emoji, position = Offset(canvasSize.width / 2f, canvasSize.height / 2f)))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .onGloballyPositioned { canvasSize = it.size }
                    .pointerInput(editMode) {
                        if (editMode == EditMode.DRAW) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentPath = DrawPath(Path().apply { moveTo(offset.x, offset.y) }, selectedColor, strokeWidth)
                                },
                                onDrag = { change, _ ->
                                    currentPath?.path?.lineTo(change.position.x, change.position.y)
                                    val temp = currentPath
                                    currentPath = null
                                    currentPath = temp
                                },
                                onDragEnd = {
                                    currentPath?.let { paths.add(it) }
                                    currentPath = null
                                }
                            )
                        }
                    }
            ) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                
                Canvas(modifier = Modifier.fillMaxSize()) {
                    paths.forEach { drawPath ->
                        drawPath(
                            path = drawPath.path,
                            color = drawPath.color,
                            style = Stroke(
                                width = drawPath.strokeWidth.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                    currentPath?.let { drawPath ->
                        drawPath(
                            path = drawPath.path,
                            color = drawPath.color,
                            style = Stroke(
                                width = drawPath.strokeWidth.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
                
                // Text Layers
                texts.forEach { textOverlay ->
                    Box(
                        modifier = Modifier
                            .offset(x = textOverlay.position.x.dp / 2f, y = textOverlay.position.y.dp / 2f) // Rough scaling
                            .pointerInput(textOverlay.id) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    textOverlay.position += dragAmount
                                }
                            }
                    ) {
                        Text(
                            text = textOverlay.text,
                            color = textOverlay.color,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Sticker Layers
                stickers.forEach { sticker ->
                    Box(
                        modifier = Modifier
                            .offset(x = sticker.position.x.dp / 2f, y = sticker.position.y.dp / 2f)
                            .pointerInput(sticker.id) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    sticker.position += dragAmount
                                }
                            }
                    ) {
                        Text(text = sticker.emoji, fontSize = 40.sp)
                    }
                }
            }
        }
    }
    
    if (showTextDialog) {
        AlertDialog(
            onDismissRequest = { showTextDialog = false },
            title = { Text("Thêm văn bản") },
            text = {
                TextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Nhập nội dung...") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (textInput.isNotEmpty()) {
                        texts.add(TextOverlay(text = textInput, position = Offset(100f, 100f), color = selectedColor))
                        textInput = ""
                        showTextDialog = false
                    }
                }) { Text("Xong") }
            }
        )
    }
}

@Composable
fun EditModeItem(icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = if (isSelected) Modifier.background(MaterialTheme.colorScheme.primary.copy(0.1f), CircleShape) else Modifier
    ) {
        Icon(icon, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray)
    }
}

enum class EditMode { DRAW, TEXT, STICKER }

fun flattenImage(
    context: Context,
    originalUri: Uri,
    paths: List<DrawPath>,
    texts: List<TextOverlay>,
    stickers: List<StickerOverlay>,
    canvasSize: IntSize
): Uri? {
    return try {
        val inputStream = context.contentResolver.openInputStream(originalUri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        
        // Create a result bitmap with same dimensions
        val resultBitmap = Bitmap.createBitmap(originalBitmap.width, originalBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(resultBitmap)
        
        // Scale factor between screen canvas and actual bitmap
        val scaleX = originalBitmap.width.toFloat() / canvasSize.width
        val scaleY = originalBitmap.height.toFloat() / canvasSize.height
        
        // Draw original
        canvas.drawBitmap(originalBitmap, 0f, 0f, null)
        
        // Draw Paths
        val pathPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        
        paths.forEach { drawPath ->
            pathPaint.color = drawPath.color.toArgb()
            pathPaint.strokeWidth = drawPath.strokeWidth * scaleX 
            
            val androidPath = android.graphics.Path()
            // Here we would need to scale each point in the path. 
            // For brevity, we use asAndroidPath() assuming screen coordinates, 
            // but a proper implementation would iterate and scale points.
            val matrix = android.graphics.Matrix()
            matrix.postScale(scaleX, scaleY)
            val scaledPath = drawPath.path.asAndroidPath()
            scaledPath.transform(matrix)
            canvas.drawPath(scaledPath, pathPaint)
        }
        
        // Draw Texts
        val textPaint = Paint().apply {
            textSize = 24f * scaleX * 2f // Scale font size
            isAntiAlias = true
            isFakeBoldText = true
        }
        texts.forEach { textOverlay ->
            textPaint.color = textOverlay.color.toArgb()
            canvas.drawText(textOverlay.text, textOverlay.position.x * scaleX, textOverlay.position.y * scaleY, textPaint)
        }
        
        // Draw Stickers
        val stickerPaint = Paint().apply {
            textSize = 40f * scaleX * 2f
            isAntiAlias = true
        }
        stickers.forEach { sticker ->
            canvas.drawText(sticker.emoji, sticker.position.x * scaleX, sticker.position.y * scaleY, stickerPaint)
        }
        
        val outFile = File(context.cacheDir, "edited_${System.currentTimeMillis()}.jpg")
        val out = FileOutputStream(outFile)
        resultBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        out.flush()
        out.close()
        
        Uri.fromFile(outFile)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
