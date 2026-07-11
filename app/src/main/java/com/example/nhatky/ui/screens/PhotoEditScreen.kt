package com.example.nhatky.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.roundToInt
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.example.nhatky.ui.utils.checkAndRequestDrivePermission
import com.example.nhatky.ui.utils.rememberDrivePermissionLauncher
import com.example.nhatky.viewmodel.AuthViewModel
import com.example.nhatky.viewmodel.DiaryViewModel
import android.widget.Toast
import com.example.nhatky.data.model.DiaryEntry
import com.example.nhatky.ui.components.VideoPlayerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class DrawPath(
    val path: Path,
    val color: Color,
    val strokeWidth: Float,
    val points: List<Offset> = emptyList(),
    val isEraser: Boolean = false
)

data class TextOverlay(
    val id: Long = System.nanoTime(),
    var text: String,
    var position: Offset,
    var color: Color,
    var scale: Float = 1f,
    var fontSize: Float = 24f,
    var rotation: Float = 0f
)

// HÀM HỖ TRỢ: Chuyển đổi googledrive:// thành Link HTTP thật
private fun getRealDriveUrl(url: String): String {
    if (url.startsWith("googledrive://")) {
        val id = url.substringAfter("googledrive://").substringBeforeLast(".")
        return "https://www.googleapis.com/drive/v3/files/$id?alt=media"
    }
    return url
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditScreen(
    imageUri: Uri,
    diaryId: String? = null,
    authViewModel: AuthViewModel,
    diaryViewModel: DiaryViewModel,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val user by authViewModel.currentUser.collectAsState()

    // 1. Chuyển đổi ảnh ra link thực tế để hệ thống đọc được
    val realImageUriStr = remember(imageUri) { getRealDriveUrl(imageUri.toString()) }
    val realImageUri = Uri.parse(realImageUriStr)

    val drivePermissionLauncher = rememberDrivePermissionLauncher { success ->
        if (success) {
            Toast.makeText(context, "Đã cấp quyền Google Drive. Vui lòng nhấn Lưu lại.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Cần quyền Google Drive để lưu ảnh/video.", Toast.LENGTH_LONG).show()
        }
    }

    // Kiểm tra định dạng Video
    val isVideo = remember(imageUri) {
        val urlStr = imageUri.toString().lowercase()
        urlStr.endsWith(".mp4") || context.contentResolver.getType(imageUri)?.startsWith("video") == true
    }

    var currentPath by remember { mutableStateOf<DrawPath?>(null) }
    val paths = remember { mutableStateListOf<DrawPath>() }
    val texts = remember { mutableStateListOf<TextOverlay>() }

    var selectedColor by remember { mutableStateOf(Color.Red) }
    var strokeWidth by remember { mutableStateOf(10f) }
    var editMode by remember { mutableStateOf(EditMode.DRAW) }
    var isLoading by remember { mutableStateOf(false) }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val actionHistory = remember { mutableStateListOf<ActionType>() }
    var editingTextId by remember { mutableStateOf<Long?>(null) }

    val colors = listOf(Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.Black, Color.White)

    var existingDiary by remember { mutableStateOf<DiaryEntry?>(null) }

    LaunchedEffect(diaryId) {
        if (diaryId != null) {
            existingDiary = diaryViewModel.getDiaryById(diaryId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isVideo) "Xem trước Video" else "Chỉnh sửa Ảnh", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (!isVideo && (canvasSize.width == 0 || canvasSize.height == 0)) return@TextButton

                            checkAndRequestDrivePermission(context, drivePermissionLauncher) {
                                isLoading = true
                                user?.uid?.let { uid ->
                                    if (isVideo) {
                                        diaryViewModel.addOrUpdateDiary(
                                            diaryId = diaryId,
                                            userId = uid,
                                            title = existingDiary?.title ?: "Kỷ niệm Video",
                                            content = existingDiary?.content ?: "Video kỷ niệm",
                                            mood = existingDiary?.mood ?: "Bình thường",
                                            tags = existingDiary?.tags ?: emptyList(),
                                            imageUris = listOf(realImageUri), // Truyền link đã convert
                                            existingMediaUrls = emptyList(),
                                            onComplete = { success ->
                                                isLoading = false
                                                if (success) {
                                                    onSave()
                                                } else {
                                                    Toast.makeText(context, "Lỗi upload Video! Hãy kiểm tra kết nối mạng.", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        )
                                    } else {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            val editedUri = flattenImage(
                                                context = context,
                                                originalUri = realImageUri, // Đưa link thật vào để tải
                                                paths = paths,
                                                texts = texts,
                                                canvasSize = canvasSize
                                            )

                                            withContext(Dispatchers.Main) {
                                                if (editedUri != null) {
                                                    diaryViewModel.addOrUpdateDiary(
                                                        diaryId = diaryId,
                                                        userId = uid,
                                                        title = existingDiary?.title ?: "Kỷ niệm ảnh",
                                                        content = existingDiary?.content ?: "Kỷ niệm ảnh đã chỉnh sửa",
                                                        mood = existingDiary?.mood ?: "Bình thường",
                                                        tags = existingDiary?.tags ?: emptyList(),
                                                        imageUris = listOf(editedUri),
                                                        existingMediaUrls = emptyList(),
                                                        onComplete = { success ->
                                                            isLoading = false
                                                            if (success) {
                                                                onSave()
                                                            } else {
                                                                Toast.makeText(context, "Lỗi upload Ảnh lên Drive! (Chưa bật API Drive hoặc sai mã SHA-1)", Toast.LENGTH_LONG).show()
                                                            }
                                                        }
                                                    )
                                                } else {
                                                    isLoading = false
                                                    Toast.makeText(context, "Lỗi khi lưu ảnh, vui lòng thử lại", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
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
            if (!isVideo) {
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
                                onClick = {
                                    editMode = EditMode.DRAW
                                    editingTextId = null
                                }
                            )
                            EditModeItem(
                                icon = Icons.Default.AutoFixNormal,
                                isSelected = editMode == EditMode.ERASE,
                                onClick = {
                                    editMode = EditMode.ERASE
                                    editingTextId = null
                                }
                            )
                            EditModeItem(
                                icon = Icons.Default.Title,
                                isSelected = editMode == EditMode.TEXT,
                                onClick = { editMode = EditMode.TEXT }
                            )
                            IconButton(onClick = {
                                if (actionHistory.isNotEmpty()) {
                                    val lastAction = actionHistory.removeAt(actionHistory.size - 1)
                                    when (lastAction) {
                                        ActionType.DRAW -> if (paths.isNotEmpty()) paths.removeAt(paths.size - 1)
                                        ActionType.TEXT -> if (texts.isNotEmpty()) texts.removeAt(texts.size - 1)
                                    }
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo last")
                            }
                        }

                        if (editMode == EditMode.DRAW || editMode == EditMode.ERASE || editMode == EditMode.TEXT) {
                            if (editMode == EditMode.DRAW || editMode == EditMode.ERASE) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LineWeight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                                    Slider(
                                        value = strokeWidth,
                                        onValueChange = { strokeWidth = it },
                                        valueRange = 5f..50f,
                                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                                    )
                                    Text("${strokeWidth.roundToInt()}", fontSize = 12.sp, color = Color.Gray)
                                }
                            }

                            if (editMode == EditMode.DRAW || editMode == EditMode.TEXT) {
                                Spacer(modifier = Modifier.height(if (editMode == EditMode.TEXT) 16.dp else 8.dp))
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
            if (isVideo) {
                VideoPlayerView(
                    videoUrl = realImageUri.toString(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
                        .onGloballyPositioned { canvasSize = it.size }
                        .pointerInput(editMode) {
                            if (editMode == EditMode.DRAW) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        currentPath = DrawPath(
                                            path = Path().apply { moveTo(offset.x, offset.y) },
                                            color = selectedColor,
                                            strokeWidth = strokeWidth,
                                            points = listOf(offset)
                                        )
                                    },
                                    onDrag = { change, _ ->
                                        val point = change.position
                                        currentPath?.path?.lineTo(point.x, point.y)
                                        currentPath = currentPath?.copy(points = currentPath!!.points + point)
                                    },
                                    onDragEnd = {
                                        currentPath?.let {
                                            paths.add(it)
                                            actionHistory.add(ActionType.DRAW)
                                        }
                                        currentPath = null
                                    }
                                )
                            } else if (editMode == EditMode.TEXT) {
                                detectTapGestures { offset ->
                                    val newText = TextOverlay(
                                        text = "",
                                        position = offset,
                                        color = selectedColor
                                    )
                                    texts.add(newText)
                                    actionHistory.add(ActionType.TEXT)
                                    editingTextId = newText.id
                                }
                            } else if (editMode == EditMode.ERASE) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        currentPath = DrawPath(
                                            path = Path().apply { moveTo(offset.x, offset.y) },
                                            color = Color.Transparent,
                                            strokeWidth = strokeWidth,
                                            points = listOf(offset),
                                            isEraser = true
                                        )
                                    },
                                    onDrag = { change, _ ->
                                        val point = change.position
                                        currentPath?.path?.lineTo(point.x, point.y)
                                        currentPath = currentPath?.copy(points = currentPath!!.points + point)
                                    },
                                    onDragEnd = {
                                        currentPath?.let {
                                            paths.add(it)
                                            actionHistory.add(ActionType.DRAW)
                                        }
                                        currentPath = null
                                    }
                                )
                            }
                        }
                ) {
                    // Dùng realImageUri để hiển thị ảnh.
                    AsyncImage(
                        model = realImageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    ) {
                        paths.forEach { drawPath ->
                            drawPath(
                                path = drawPath.path,
                                color = drawPath.color,
                                style = Stroke(
                                    width = drawPath.strokeWidth.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                ),
                                blendMode = if (drawPath.isEraser) BlendMode.Clear else BlendMode.SrcOver
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
                                ),
                                blendMode = if (drawPath.isEraser) BlendMode.Clear else BlendMode.SrcOver
                            )
                        }
                    }

                    if (editingTextId != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    editingTextId = null
                                }
                        )
                    }

                    val sortedTexts = texts.sortedBy { if (it.id == editingTextId) 1 else 0 }

                    sortedTexts.forEach { textOverlay ->
                        key(textOverlay.id) {
                            val isEditing = editingTextId == textOverlay.id
                            val focusRequester = remember { FocusRequester() }

                            TransformableOverlay(
                                initialPosition = textOverlay.position,
                                initialScale = textOverlay.scale,
                                initialRotation = textOverlay.rotation,
                                isSelected = isEditing,
                                isInteractable = editMode == EditMode.TEXT,
                                onTransform = { newPos, newScale, newRot ->
                                    val index = texts.indexOfFirst { it.id == textOverlay.id }
                                    if (index != -1) {
                                        texts[index] = texts[index].copy(
                                            position = newPos,
                                            scale = newScale,
                                            rotation = newRot
                                        )
                                    }
                                },
                                onSelect = {
                                    editingTextId = textOverlay.id
                                }
                            ) {
                                Box(modifier = Modifier.padding(20.dp)) {
                                    Box {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 12.dp, end = 12.dp)
                                                .padding(10.dp)
                                        ) {
                                            if (isEditing) {
                                                Canvas(modifier = Modifier.matchParentSize()) {
                                                    drawRect(
                                                        color = Color.White,
                                                        style = Stroke(
                                                            width = 1.dp.toPx(),
                                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                                        )
                                                    )
                                                }
                                            }

                                            Box(
                                                modifier = Modifier.padding(2.dp)
                                            ) {
                                                var textFieldValue by remember(textOverlay.id) {
                                                    mutableStateOf(TextFieldValue(textOverlay.text, TextRange(textOverlay.text.length)))
                                                }

                                                Box {
                                                    BasicTextField(
                                                        value = textFieldValue,
                                                        onValueChange = { newValue ->
                                                            textFieldValue = newValue
                                                            textOverlay.text = newValue.text
                                                        },
                                                        enabled = isEditing,
                                                        textStyle = TextStyle(
                                                            color = textOverlay.color,
                                                            fontSize = textOverlay.fontSize.sp,
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                        cursorBrush = SolidColor(textOverlay.color),
                                                        modifier = (if (isEditing) Modifier.focusRequester(focusRequester) else Modifier)
                                                            .width(IntrinsicSize.Min),
                                                        decorationBox = { innerTextField ->
                                                            Box(contentAlignment = Alignment.CenterStart) {
                                                                if (textFieldValue.text.isEmpty()) {
                                                                    Text(
                                                                        "T",
                                                                        color = textOverlay.color.copy(alpha = 0.5f),
                                                                        fontSize = textOverlay.fontSize.sp,
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                }
                                                                innerTextField()
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(24.dp)
                                                .alpha(if (isEditing) 1f else 0f)
                                                .background(if (isEditing) Color.Red else Color.Transparent, CircleShape)
                                                .then(
                                                    if (isEditing) Modifier.clickable {
                                                        texts.removeAll { it.id == textOverlay.id }
                                                        editingTextId = null
                                                    } else Modifier
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isEditing) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Delete",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        LaunchedEffect(textOverlay.id, isEditing) {
                                            if (isEditing) {
                                                try {
                                                    kotlinx.coroutines.delay(50)
                                                    focusRequester.requestFocus()
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
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

@Composable
fun TransformableOverlay(
    initialPosition: Offset,
    initialScale: Float = 1f,
    initialRotation: Float = 0f,
    isSelected: Boolean,
    isInteractable: Boolean,
    onTransform: (Offset, Float, Float) -> Unit,
    onSelect: () -> Unit,
    content: @Composable () -> Unit
) {
    var position by remember { mutableStateOf(initialPosition) }
    var scale by remember { mutableStateOf(initialScale) }
    var rotation by remember { mutableStateOf(initialRotation) }

    Box(
        modifier = Modifier
            .graphicsLayer {
                translationX = position.x
                translationY = position.y
                scaleX = scale
                scaleY = scale
                rotationZ = rotation
            }
            .then(
                if (isInteractable) {
                    Modifier.pointerInput(isSelected) {
                        if (isSelected) {
                            detectTransformGestures { _, pan, zoom, rotationDelta ->
                                position += pan
                                scale = (scale * zoom).coerceIn(0.5f, 5f)
                                rotation += rotationDelta
                                onTransform(position, scale, rotation)
                            }
                        } else {
                            detectTapGestures {
                                onSelect()
                            }
                        }
                    }
                } else Modifier
            )
    ) {
        content()
    }
}

enum class EditMode { DRAW, TEXT, ERASE }

enum class ActionType { DRAW, TEXT }

// NÂNG CẤP HÀM FLATTEN THÀNH SUSPEND VÀ SỬ DỤNG COIL IMAGELOADER
suspend fun flattenImage(
    context: Context,
    originalUri: Uri,
    paths: List<DrawPath>,
    texts: List<TextOverlay>,
    canvasSize: IntSize
): Uri? {
    if (canvasSize.width == 0 || canvasSize.height == 0) return null

    return try {
        // Sử dụng thư viện Coil để tải ảnh qua mạng an toàn (đã bao gồm Auth qua Interceptor)
        val originalBitmap: Bitmap? = if (originalUri.toString().startsWith("http")) {
            val request = ImageRequest.Builder(context)
                .data(originalUri.toString())
                .allowHardware(false) // Bắt buộc false để Canvas có thể vẽ đè lên
                .build()
            val result = context.imageLoader.execute(request)
            (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
        } else {
            // Đọc ảnh nội bộ trong máy nếu là ảnh mới chụp từ Camera
            val inputStream = context.contentResolver.openInputStream(originalUri)
            if (inputStream != null) BitmapFactory.decodeStream(inputStream) else null
        }

        if (originalBitmap == null) return null

        val resultBitmap = Bitmap.createBitmap(originalBitmap.width, originalBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(resultBitmap)

        val scaleX = originalBitmap.width.toFloat() / canvasSize.width
        val scaleY = originalBitmap.height.toFloat() / canvasSize.height

        canvas.drawBitmap(originalBitmap, 0f, 0f, null)

        val pathPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        paths.forEach { drawPath ->
            pathPaint.color = drawPath.color.toArgb()
            pathPaint.strokeWidth = drawPath.strokeWidth * scaleX

            if (drawPath.isEraser) {
                pathPaint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
            } else {
                pathPaint.xfermode = null
            }

            val matrix = android.graphics.Matrix()
            matrix.postScale(scaleX, scaleY)
            val scaledPath = drawPath.path.asAndroidPath()
            scaledPath.transform(matrix)
            canvas.drawPath(scaledPath, pathPaint)
        }

        val textPaint = TextPaint().apply {
            isAntiAlias = true
            isFakeBoldText = true
        }

        val density = context.resources.displayMetrics.density

        texts.forEach { textOverlay ->
            textPaint.color = textOverlay.color.toArgb()
            textPaint.textSize = textOverlay.fontSize * textOverlay.scale * scaleX * 1.5f

            val staticLayout = StaticLayout.Builder.obtain(
                textOverlay.text.ifEmpty { "T" },
                0,
                textOverlay.text.ifEmpty { "T" }.length,
                textPaint,
                canvasSize.width * 2
            )
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()

            canvas.save()

            val pxDrawX = (textOverlay.position.x + 32 * density) * scaleX
            val pxDrawY = (textOverlay.position.y + 44 * density) * scaleY

            val pivotX = pxDrawX + (staticLayout.width / 2f)
            val pivotY = pxDrawY + (staticLayout.height / 2f)

            canvas.rotate(textOverlay.rotation, pivotX, pivotY)
            canvas.translate(pxDrawX, pxDrawY)

            staticLayout.draw(canvas)
            canvas.restore()
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