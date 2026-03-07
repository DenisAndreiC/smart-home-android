package com.denis.smarthome.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.denis.smarthome.ui.theme.OnBackground
import com.denis.smarthome.ui.theme.OnSurface
import com.denis.smarthome.ui.theme.Primary

@Composable
fun CircularTemperatureDisplay(
    temperature: Int,
    isOn: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp
) {
    val arcColor1 = if (isOn) Primary else Color(0xFF37474F)
    val arcColor2 = if (isOn) Color(0xFF00E5FF) else Color(0xFF546E7A)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 12.dp.toPx()
            val padding = strokeWidth / 2
            val arcSize = Size(this.size.width - padding * 2, this.size.height - padding * 2)
            val topLeft = Offset(padding, padding)

            // Background track
            drawArc(
                color = Color(0xFF1E3040),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Progress arc
            val sweepAngle = if (isOn) {
                val progress = (temperature - 16f) / (30f - 16f)
                progress * 270f
            } else 0f

            if (sweepAngle > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(arcColor1, arcColor2, arcColor1),
                        center = Offset(this.size.width / 2, this.size.height / 2)
                    ),
                    startAngle = 135f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        // Temperature text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isOn) "$temperature°" else "--°",
                color = if (isOn) OnBackground else OnSurface,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 52.sp
            )
            Text(
                text = if (isOn) "Celsius" else "Standby",
                color = OnSurface,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
