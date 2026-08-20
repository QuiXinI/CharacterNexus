package ru.quasaris.characternexus.ui.util

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import characternexus.shared.generated.resources.Res
import characternexus.shared.generated.resources.ic_premium_dragon

@Composable
fun PayWall(
    isLocked: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(12.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isLocked) Modifier.blur(2.dp) else Modifier)
        ) {
            content()
        }

        if (isLocked) {
            val isDark = isSystemInDarkTheme()
            val overlayColor = if (isDark) Color.Black.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.1f)
            
            val diamondGradient = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF00E5FF), // Cyan
                    Color(0xFF00B0FF), // Light Blue
                    Color(0xFF2979FF)  // Blue
                )
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(overlayColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Gradient Icon
                val painter = painterResource(Res.drawable.ic_premium_dragon)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                        .drawWithCache {
                            onDrawWithContent {
                                drawContent()
                                drawRect(diamondGradient, blendMode = BlendMode.SrcIn)
                            }
                        }
                ) {
                    Icon(
                        painter = painter,
                        contentDescription = "Premium Only",
                        modifier = Modifier.fillMaxSize(),
                        tint = Color.White // Base for SrcIn
                    )
                }
            }
        }
    }
}
