package ru.quasaris.characternexus.ui.util

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import characternexus.shared.generated.resources.Res
import characternexus.shared.generated.resources.ic_premium_dragon
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle

@Composable
fun PayWall(
    isLocked: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var showPremiumText by remember { mutableStateOf(false) }

    val diamondGradient = remember {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF00E5FF), // Cyan
                Color(0xFF00B0FF), // Light Blue
                Color(0xFF2979FF)  // Blue
            )
        )
    }

    LaunchedEffect(showPremiumText) {
        if (showPremiumText) {
            delay(5.seconds)
            showPremiumText = false
        }
    }

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
            
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(overlayColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showPremiumText = true }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Gradient Icon
                    val painter = painterResource(Res.drawable.ic_premium_dragon)
                    Box(
                        modifier = Modifier
                            .size(28.dp)
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

                    AnimatedVisibility(
                        visible = showPremiumText,
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = "Это ",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            val premiumTextStyle = LocalTextStyle.current.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Премиум",
                                    style = premiumTextStyle.merge(
                                        TextStyle(
                                            drawStyle = Stroke(
                                                miter = 30f,
                                                width = 10f,
                                                join = StrokeJoin.Round
                                            )
                                        )
                                    ),
                                    color = MaterialTheme.colorScheme.background
                                )

                                Box(
                                    modifier = Modifier
                                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                                        .drawWithCache {
                                            onDrawWithContent {
                                                drawContent()
                                                drawRect(diamondGradient, blendMode = BlendMode.SrcIn)
                                            }
                                        }
                                ) {
                                    Text(
                                        text = "Премиум",
                                        style = premiumTextStyle,
                                        color = Color.White
                                    )
                                }
                            }

                            Text(
                                text = " функция",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
