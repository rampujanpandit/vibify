package com.vibify.music.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun SpotifyCanvasVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "canvas_animation")

    // Animated phase for flowing sound waves
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    // Animated pulse radius for beat
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Secondary color shift phase
    val colorPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "color_phase"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F0F14))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f
            val centerY = height / 2f

            // Dynamic background gradient
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1DB954).copy(alpha = if (isPlaying) 0.45f else 0.15f),
                        Color(0xFF2E0854).copy(alpha = 0.6f),
                        Color(0xFF0A0A0E)
                    ),
                    center = Offset(centerX, centerY),
                    radius = (width.coerceAtLeast(height) / 1.4f) * (if (isPlaying) pulseScale else 1f)
                )
            )

            if (isPlaying) {
                // Expanding sonic ripple rings
                for (i in 1..3) {
                    val rippleRadius = (width * 0.22f * i * pulseScale) % (width * 0.65f)
                    val alpha = (1f - (rippleRadius / (width * 0.65f))).coerceIn(0f, 0.4f)
                    drawCircle(
                        color = Color(0xFF1DB954).copy(alpha = alpha),
                        radius = rippleRadius,
                        center = Offset(centerX, centerY)
                    )
                }

                // Flowing rhythmic wave path
                val path1 = Path()
                val path2 = Path()

                val waveHeight1 = height * 0.08f
                val waveHeight2 = height * 0.05f

                path1.moveTo(0f, centerY)
                path2.moveTo(0f, centerY + 20f)

                val points = 30
                for (step in 0..points) {
                    val x = (width / points) * step
                    val radians = Math.toRadians((step * 15.0 + wavePhase))
                    val y1 = centerY + (sin(radians).toFloat() * waveHeight1 * pulseScale)
                    val y2 = centerY + (sin(radians + 1.2).toFloat() * waveHeight2)

                    path1.lineTo(x, y1)
                    path2.lineTo(x, y2)
                }

                path1.lineTo(width, height)
                path1.lineTo(0f, height)
                path1.close()

                path2.lineTo(width, height)
                path2.lineTo(0f, height)
                path2.close()

                // Draw translucent glowing wave layers
                drawPath(
                    path = path2,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF9C27B0).copy(alpha = 0.4f),
                            Color.Transparent
                        ),
                        startY = centerY,
                        endY = height
                    )
                )

                drawPath(
                    path = path1,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1ED760).copy(alpha = 0.65f),
                            Color(0xFF121212).copy(alpha = 0.1f)
                        ),
                        startY = centerY - 30f,
                        endY = height
                    )
                )
            }
        }
    }
}
