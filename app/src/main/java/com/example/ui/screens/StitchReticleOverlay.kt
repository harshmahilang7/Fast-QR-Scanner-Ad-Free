package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GoogleBlue
import com.example.ui.theme.GoogleGreen
import com.example.ui.theme.GoogleRed
import com.example.ui.theme.GoogleYellow
import com.example.ui.theme.StitchLaserCyan
import kotlin.math.min

@Composable
fun StitchReticleOverlay(
    modifier: Modifier = Modifier,
    isScanningActive: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "LaserTransition")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserOffset"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Calculate viewfinder square size: 70% of min dimension, capped at 320dp
            val boxSize = min(canvasWidth * 0.72f, canvasHeight * 0.42f).coerceIn(220.dp.toPx(), 320.dp.toPx())
            val left = (canvasWidth - boxSize) / 2f
            val top = (canvasHeight - boxSize) / 2f - 40.dp.toPx()
            val right = left + boxSize
            val bottom = top + boxSize

            val cornerRadius = 24.dp.toPx()
            val bracketLength = 36.dp.toPx()
            val strokeWidth = 4.5.dp.toPx()

            // 1. Dark semi-transparent scrim with rounded cutout for the viewfinder
            val cutoutPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(left, top, right, bottom),
                        cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                    )
                )
            }

            clipPath(cutoutPath, clipOp = ClipOp.Difference) {
                drawRect(color = Color(0x990A0D14))
            }

            // 2. Subtle soft inner border
            drawRoundRect(
                color = Color.White.copy(alpha = 0.15f),
                topLeft = Offset(left, top),
                size = Size(boxSize, boxSize),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
            )

            // 3. Google Stitch 4-Corner Accent Brackets
            // Top-Left: Google Blue
            drawPath(
                path = Path().apply {
                    moveTo(left, top + bracketLength)
                    lineTo(left, top + cornerRadius)
                    quadraticTo(left, top, left + cornerRadius, top)
                    lineTo(left + bracketLength, top)
                },
                color = GoogleBlue,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )

            // Top-Right: Google Red
            drawPath(
                path = Path().apply {
                    moveTo(right - bracketLength, top)
                    lineTo(right - cornerRadius, top)
                    quadraticTo(right, top, right, top + cornerRadius)
                    lineTo(right, top + bracketLength)
                },
                color = GoogleRed,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )

            // Bottom-Left: Google Green
            drawPath(
                path = Path().apply {
                    moveTo(left, bottom - bracketLength)
                    lineTo(left, bottom - cornerRadius)
                    quadraticTo(left, bottom, left + cornerRadius, bottom)
                    lineTo(left + bracketLength, bottom)
                },
                color = GoogleGreen,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )

            // Bottom-Right: Google Yellow
            drawPath(
                path = Path().apply {
                    moveTo(right - bracketLength, bottom)
                    lineTo(right - cornerRadius, bottom)
                    quadraticTo(right, bottom, right, bottom - cornerRadius)
                    lineTo(right, bottom - bracketLength)
                },
                color = GoogleYellow,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )

            // 4. Animated Scanning Laser Bar
            if (isScanningActive) {
                val currentLaserY = top + (boxSize * laserProgress)
                val laserPadding = 16.dp.toPx()
                val laserStartX = left + laserPadding
                val laserEndX = right - laserPadding

                // Soft glow gradient around the laser
                val laserGlowBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        StitchLaserCyan.copy(alpha = 0.25f * pulseAlpha),
                        StitchLaserCyan.copy(alpha = 0.85f * pulseAlpha),
                        StitchLaserCyan.copy(alpha = 0.25f * pulseAlpha),
                        Color.Transparent
                    ),
                    startY = currentLaserY - 14.dp.toPx(),
                    endY = currentLaserY + 14.dp.toPx()
                )

                drawRect(
                    brush = laserGlowBrush,
                    topLeft = Offset(laserStartX, currentLaserY - 12.dp.toPx()),
                    size = Size(laserEndX - laserStartX, 24.dp.toPx())
                )

                // Crisp Center Laser Line
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            StitchLaserCyan.copy(alpha = pulseAlpha),
                            Color.White.copy(alpha = pulseAlpha),
                            StitchLaserCyan.copy(alpha = pulseAlpha),
                            Color.Transparent
                        ),
                        startX = laserStartX,
                        endX = laserEndX
                    ),
                    start = Offset(laserStartX, currentLaserY),
                    end = Offset(laserEndX, currentLaserY),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
