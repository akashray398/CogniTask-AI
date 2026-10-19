package com.example.knotes.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CogniTaskLogoCompose(
    modifier: Modifier = Modifier,
    isLarge: Boolean = false
) {
    // Breathing aura pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "CogniTaskLogoAnimation")
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val cardSize = if (isLarge) 140.dp else 56.dp
    val cogniFontSize = if (isLarge) 28.sp else 16.sp
    val taskFontSize = if (isLarge) 18.sp else 11.sp
    val aiFontSize = if (isLarge) 13.sp else 8.sp

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        // Pulsing Radial Aura Ring
        Box(
            modifier = Modifier
                .size(cardSize * 1.15f)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Glassmorphic Card Container
        Surface(
            modifier = Modifier
                .size(cardSize)
                .clip(RoundedCornerShape(if (isLarge) 32.dp else 16.dp)),
            shape = RoundedCornerShape(if (isLarge) 32.dp else 16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                )
            ),
            tonalElevation = 8.dp
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isLarge) 8.dp else 2.dp)
            ) {
                // Typographic Superscript / Subscript Layout: Cogni_Task^AI ✦
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Top Word: Cogni
                        Text(
                            text = "Cogni",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            fontSize = cogniFontSize,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            letterSpacing = (-0.5).sp,
                            lineHeight = cogniFontSize
                        )

                        // Subscript Word: Task
                        Text(
                            text = "Task",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = taskFontSize,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp,
                            lineHeight = taskFontSize
                        )
                    }

                    Spacer(modifier = Modifier.width(3.dp))

                    // Superscript Column: AI ✦
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.offset(y = if (isLarge) (-8).dp else (-4).dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFFD54F)
                        ) {
                            Text(
                                text = "AI",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF21005D),
                                fontSize = aiFontSize
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "✦",
                            fontSize = (aiFontSize.value + 3).sp,
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.scale(pulseScale)
                        )
                    }
                }
            }
        }
    }
}
