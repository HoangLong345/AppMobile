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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
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
    val strokeWidth: Float,
    val points: List<Offset> = emptyList(),
    val isEraser: Boolean = false
)

data class TextOverlay(
    val id: Long = System.nanoTime(),
    var text: String,
    var position: Offset,
    var color: Color,
    var fontSize: Float = 24f,
    var rotation: Float = 0f
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

    var selectedColor by remember { mutableStateOf(Color.Red) }
    var strokeWidth by remember { mutableStateOf(10f) }
    var editMode by remember { mutableStateOf(EditMode.DRAW) }
    var isLoading by remember { mutableStateOf(false) }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val actionHistory = remember { mutableStateListOf<ActionType>() }
    var editingTextId by remember { mutableStateOf<Long?>(null) }

    val colors = listOf(Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.Black, Color.White)

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
                            if (canvasSize.width == 0 || canvasSize.height == 0) return@TextButton

                            isLoading = true
                            user?.uid?.let { uid ->
                                val editedUri = flattenImage(
                                    context = context,
                                    originalUri = imageUri,
                                    paths = paths,
                                    texts = texts,
                                    canvasSize = canvasSize
                                )

                                if (editedUri != null) {
                                    diaryViewModel.addOrUpdateDiary(
                                        diaryId = null,
                                        userId = uid,
                                        title = "Kỷ niệm ảnh",
                                        content = "Kỷ niệm ảnh đã chỉnh sửa",
                                        mood = "Bình thường",
                                        tags = emptyList(),
                                        imageUris = listOf(editedUri),
                                        existingMediaUrls = emptyList(),
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
                            icon = Icons.Default.AutoFixNormal,
                            isSelected = editMode == EditMode.ERASE,
                            onClick = { editMode = EditMode.ERASE }
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
                AsyncImage(
                    model = imageUri,
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

                // Text Layers
                texts.forEach { textOverlay ->
                    key(textOverlay.id) {
                        val isEditing = editingTextId == textOverlay.id
                        val focusRequester = remember { FocusRequester() }

                        TransformableOverlay(
                            initialPosition = textOverlay.position,
                            onPositionChanged = { newPos ->
                                textOverlay.position = newPos
                            },
                            onZoom = { zoomFactor ->
                                val index = texts.indexOfFirst { it.id == textOverlay.id }
                                if (index != -1) {
                                    val currentSize = texts[index].fontSize
                                    val newSize = (currentSize * zoomFactor).coerceIn(10f, 200f)
                                    texts[index] = texts[index].copy(fontSize = newSize)
                                }
                            },
                            onRotate = { rotationDelta ->
                                val index = texts.indexOfFirst { it.id == textOverlay.id }
                                if (index != -1) {
                                    val currentRotation = texts[index].rotation
                                    texts[index] = texts[index].copy(rotation = currentRotation + rotationDelta)
                                }
                            }
                        ) {
                            Box(
                                modifier = Modifier.graphicsLayer(rotationZ = textOverlay.rotation)
                            ) {
                                Box(modifier = Modifier.padding(10.dp)) {
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

                                        Box(
                                            modifier = if (!isEditing) Modifier.pointerInput(textOverlay.id) {
                                                detectTapGestures { editingTextId = textOverlay.id }
                                            } else Modifier
                                        ) {
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
                                                            Text("T", color = textOverlay.color.copy(alpha = 0.5f), fontSize = textOverlay.fontSize.sp, fontWeight = FontWeight.Bold)
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
                                        focusRequester.requestFocus()
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
    onPositionChanged: (Offset) -> Unit,
    onZoom: (Float) -> Unit = {},
    onRotate: (Float) -> Unit = {},
    content: @Composable () -> Unit
) {
    var position by remember { mutableStateOf(initialPosition) }

    val currentOnZoom by rememberUpdatedState(onZoom)
    val currentOnRotate by rememberUpdatedState(onRotate)
    val currentOnPositionChanged by rememberUpdatedState(onPositionChanged)

    Box(
        modifier = Modifier
            .offset { IntOffset(position.x.roundToInt(), position.y.roundToInt()) }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, rotationDelta ->
                    if (zoom != 1f) {
                        currentOnZoom(zoom)
                    }
                    if (rotationDelta != 0f) {
                        currentOnRotate(rotationDelta)
                    }
                    if (pan != Offset.Zero) {
                        position += pan
                        currentOnPositionChanged(position)
                    }
                }
            }
    ) {
        content()
    }
}

enum class EditMode { DRAW, TEXT, ERASE }

enum class ActionType { DRAW, TEXT }

fun flattenImage(
    context: Context,
    originalUri: Uri,
    paths: List<DrawPath>,
    texts: List<TextOverlay>,
    canvasSize: IntSize
): Uri? {
    if (canvasSize.width == 0 || canvasSize.height == 0) return null

    return try {
        val inputStream = context.contentResolver.openInputStream(originalUri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)

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
            textPaint.textSize = textOverlay.fontSize * scaleX * 1.5f

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

            // Cập nhật vị trí lưu do đã thay đổi padding bên trong (10 + 2 = 12)
            val pxDrawX = (textOverlay.position.x + 12 * density) * scaleX
            val pxDrawY = (textOverlay.position.y + 12 * density) * scaleY

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