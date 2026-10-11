package com.example.knotes.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.knotes.R

data class LinePath(
    val path: Path,
    val color: Color,
    val strokeWidth: Float
)

enum class DrawTool {
    PEN, HIGHLIGHTER, ERASER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SketchCanvasDialog(
    onSaveSketch: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    var paths by remember { mutableStateOf(listOf<LinePath>()) }
    var undonePaths by remember { mutableStateOf(listOf<LinePath>()) }
    var currentPath by remember { mutableStateOf<Path?>(null) }

    var selectedColor by remember { mutableStateOf(Color(0xFF21005D)) }
    var selectedTool by remember { mutableStateOf(DrawTool.PEN) }
    var strokeWidth by remember { mutableFloatStateOf(6f) }

    val colors = listOf(
        Color(0xFF1C1B1F),
        Color(0xFF6750A4),
        Color(0xFFB3261E),
        Color(0xFF2E7D32),
        Color(0xFF0288D1),
        Color(0xFFF57F17),
        Color(0xFFFBC02D)
    )

    var canvasWidth by remember { mutableIntStateOf(1080) }
    var canvasHeight by remember { mutableIntStateOf(1200) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_ai_assist),
                        contentDescription = "Canvas",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Stylus & Finger Sketch Pad 🎨",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = {
                            if (paths.isNotEmpty()) {
                                undonePaths = undonePaths + paths.last()
                                paths = paths.dropLast(1)
                            }
                        },
                        enabled = paths.isNotEmpty()
                    ) {
                        Text("↩", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(
                        onClick = {
                            if (undonePaths.isNotEmpty()) {
                                paths = paths + undonePaths.last()
                                undonePaths = undonePaths.dropLast(1)
                            }
                        },
                        enabled = undonePaths.isNotEmpty()
                    ) {
                        Text("↪", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(onClick = {
                        paths = emptyList()
                        undonePaths = emptyList()
                    }) {
                        Text("🗑️", fontSize = 16.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Drawing Canvas Area
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .clip(RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(selectedTool, selectedColor, strokeWidth) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        currentPath = Path().apply { moveTo(offset.x, offset.y) }
                                    },
                                    onDrag = { change, _ ->
                                        currentPath?.let { path ->
                                            path.lineTo(change.position.x, change.position.y)
                                            currentPath = Path().apply { addPath(path) }
                                        }
                                    },
                                    onDragEnd = {
                                        currentPath?.let { path ->
                                            val color = when (selectedTool) {
                                                DrawTool.ERASER -> Color.White
                                                DrawTool.HIGHLIGHTER -> selectedColor.copy(alpha = 0.35f)
                                                DrawTool.PEN -> selectedColor
                                            }
                                            val width = when (selectedTool) {
                                                DrawTool.ERASER -> strokeWidth * 3f
                                                DrawTool.HIGHLIGHTER -> strokeWidth * 2.5f
                                                DrawTool.PEN -> strokeWidth
                                            }
                                            paths = paths + LinePath(path, color, width)
                                            currentPath = null
                                            undonePaths = emptyList()
                                        }
                                    }
                                )
                            }
                    ) {
                        canvasWidth = size.width.toInt()
                        canvasHeight = size.height.toInt()

                        paths.forEach { linePath ->
                            drawPath(
                                path = linePath.path,
                                color = linePath.color,
                                style = Stroke(
                                    width = linePath.strokeWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }

                        currentPath?.let { path ->
                            val color = when (selectedTool) {
                                DrawTool.ERASER -> Color.White
                                DrawTool.HIGHLIGHTER -> selectedColor.copy(alpha = 0.35f)
                                DrawTool.PEN -> selectedColor
                            }
                            val width = when (selectedTool) {
                                DrawTool.ERASER -> strokeWidth * 3f
                                DrawTool.HIGHLIGHTER -> strokeWidth * 2.5f
                                DrawTool.PEN -> strokeWidth
                            }
                            drawPath(
                                path = path,
                                color = color,
                                style = Stroke(
                                    width = width,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tool Selection Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        DrawTool.PEN to "🖊️ Pen",
                        DrawTool.HIGHLIGHTER to "🖍️ Marker",
                        DrawTool.ERASER to "🧹 Eraser"
                    ).forEach { (tool, label) ->
                        val isSelected = selectedTool == tool
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedTool = tool },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(3f to "Fine", 7f to "Medium", 14f to "Bold").forEach { (width, label) ->
                        val isSelected = strokeWidth == width
                        Surface(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { strokeWidth = width },
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Color Selection Palette
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                colors.forEach { color ->
                    val isSelected = selectedColor == color && selectedTool != DrawTool.ERASER
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                shape = CircleShape
                            )
                            .clickable {
                                selectedColor = color
                                if (selectedTool == DrawTool.ERASER) selectedTool = DrawTool.PEN
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Save & Insert Button
            Button(
                onClick = {
                    if (paths.isNotEmpty() && canvasWidth > 0 && canvasHeight > 0) {
                        val bitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(bitmap)
                        canvas.drawColor(android.graphics.Color.WHITE)

                        val paint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            style = android.graphics.Paint.Style.STROKE
                            strokeCap = android.graphics.Paint.Cap.ROUND
                            strokeJoin = android.graphics.Paint.Join.ROUND
                        }

                        paths.forEach { linePath ->
                            paint.color = linePath.color.toArgb()
                            paint.strokeWidth = linePath.strokeWidth
                            canvas.drawPath(linePath.path.asAndroidPath(), paint)
                        }

                        onSaveSketch(bitmap)
                        onDismiss()
                    } else {
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = paths.isNotEmpty()
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_save),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Insert Sketch into Note 🎨",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
