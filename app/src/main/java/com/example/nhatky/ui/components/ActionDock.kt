package com.example.nhatky.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ActionDock(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onPenClick: () -> Unit,
    onPaletteClick: () -> Unit,
    onMainAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        // Main Add Button (Center)
        FloatingActionButton(
            onClick = onMainAddClick,
            modifier = Modifier
                .size(64.dp)
                .align(Alignment.BottomCenter)
                .offset(y = (-8).dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(32.dp))
        }

        // Secondary Buttons cluster around
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // Camera (using PlayArrow as placeholder)
            CircleToolButton(icon = Icons.Default.PlayArrow, onClick = onCameraClick)
            
            // Pen
            CircleToolButton(icon = Icons.Default.Edit, onClick = onPenClick)
            
            Spacer(modifier = Modifier.width(64.dp)) // Space for center FAB
            
            // Photos (using Face as placeholder)
            CircleToolButton(icon = Icons.Default.Face, onClick = onGalleryClick)
            
            // Palette (using Build as placeholder)
            CircleToolButton(icon = Icons.Default.Build, onClick = onPaletteClick)
        }
    }
}

@Composable
fun CircleToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .shadow(elevation = 4.dp, shape = CircleShape),
        color = Color.White,
        shape = CircleShape
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
