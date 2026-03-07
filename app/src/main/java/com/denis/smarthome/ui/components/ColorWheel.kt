package com.denis.smarthome.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.*

// Maps angle (0–360°, starting East clockwise) to a hue color
fun angleToHueColor(angle: Float): Color {
    val a = ((angle % 360f) + 360f) % 360f
    val segment = (a / 60f).toInt().coerceIn(0, 5)
    val fraction = (a % 60f) / 60f
    return when (segment) {
        0 -> lerp(Color.Red,     Color.Yellow,  fraction)
        1 -> lerp(Color.Yellow,  Color.Green,   fraction)
        2 -> lerp(Color.Green,   Color.Cyan,    fraction)
        3 -> lerp(Color.Cyan,    Color.Blue,    fraction)
        4 -> lerp(Color.Blue,    Color.Magenta, fraction)
        else -> lerp(Color.Magenta, Color.Red,  fraction)
    }
}

@Composable
fun ColorWheel(
    selectedAngle: Float,
    onColorChanged: (Color, Float) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 280.dp
) {
    val ringColors = listOf(
        Color.Red, Color.Yellow, Color.Green,
        Color.Cyan, Color.Blue, Color.Magenta, Color.Red
    )

    val innerBg = Color(0xFF1B2838)

    fun updateFromOffset(offset: Offset, canvasWidth: Float, canvasHeight: Float) {
        val cx = canvasWidth / 2f
        val cy = canvasHeight / 2f
        val dx = offset.x - cx
        val dy = offset.y - cy
        val distFromCenter = sqrt(dx * dx + dy * dy)
        val ringWidth = canvasWidth * 0.14f
        val outerR = canvasWidth / 2f
        val innerR = outerR - ringWidth
        // Only respond to taps on the ring area
        if (distFromCenter < innerR - 4f || distFromCenter > outerR + 4f) return

        val angleRad = atan2(dy, dx)
        val angleDeg = (Math.toDegrees(angleRad.toDouble()).toFloat() + 360f) % 360f
        onColorChanged(angleToHueColor(angleDeg), angleDeg)
    }

    var canvasSize by remember { mutableStateOf(Pair(0f, 0f)) }

    Canvas(
        modifier = modifier
            .size(size)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    updateFromOffset(offset, canvasSize.first, canvasSize.second)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    updateFromOffset(change.position, canvasSize.first, canvasSize.second)
                }
            }
    ) {
        canvasSize = Pair(this.size.width, this.size.height)
        val ringWidth = this.size.width * 0.14f
        val outerR = this.size.minDimension / 2f
        val midR = outerR - ringWidth / 2f
        val innerR = outerR - ringWidth

        // Draw color ring
        drawCircle(
            brush = Brush.sweepGradient(
                colors = ringColors,
                center = center
            ),
            radius = midR,
            style = Stroke(width = ringWidth)
        )

        // Draw inner filled circle (hide center)
        drawCircle(color = innerBg, radius = innerR - 1f)

        // Draw outer border
        drawCircle(
            color = Color(0xFF2A3F50),
            radius = outerR - 1f,
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Draw indicator at selected angle
        val rad = Math.toRadians(selectedAngle.toDouble())
        val ix = center.x + midR * cos(rad).toFloat()
        val iy = center.y + midR * sin(rad).toFloat()
        val indicatorR = 12.dp.toPx()

        drawCircle(
            color = Color.Black.copy(alpha = 0.4f),
            radius = indicatorR + 2f,
            center = Offset(ix, iy)
        )
        drawCircle(
            color = Color.White,
            radius = indicatorR,
            center = Offset(ix, iy)
        )
        drawCircle(
            color = angleToHueColor(selectedAngle),
            radius = indicatorR - 4.dp.toPx(),
            center = Offset(ix, iy)
        )
    }
}
