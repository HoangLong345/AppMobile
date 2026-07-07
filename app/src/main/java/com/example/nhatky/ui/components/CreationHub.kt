package com.example.nhatky.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreationHub(
    onWriteText: () -> Unit,
    onTakePhoto: () -> Unit,
    onImportPhoto: () -> Unit,
    onDraw: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp, start = 24.dp, end = 24.dp, top = 8.dp)
        ) {
            Text(
                text = "Tạo kỷ niệm mới",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ),
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CreationItem(
                    icon = Icons.Default.Edit,
                    label = "Viết chữ",
                    color = MaterialTheme.colorScheme.primary,
                    onClick = {
                        onDismiss()
                        onWriteText()
                    }
                )
                CreationItem(
                    icon = Icons.Default.Add,
                    label = "Chụp ảnh",
                    color = Color(0xFFF59E0B),
                    onClick = {
                        onDismiss()
                        onTakePhoto()
                    }
                )
                CreationItem(
                    icon = Icons.Default.PlayArrow,
                    label = "Thư viện",
                    color = Color(0xFF10B981),
                    onClick = {
                        onDismiss()
                        onImportPhoto()
                    }
                )
                CreationItem(
                    icon = Icons.Default.Face,
                    label = "Vẽ tay",
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = {
                        onDismiss()
                        onDraw()
                    }
                )
            }
        }
    }
}

@Composable
fun CreationItem(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = color.copy(alpha = 0.1f),
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
