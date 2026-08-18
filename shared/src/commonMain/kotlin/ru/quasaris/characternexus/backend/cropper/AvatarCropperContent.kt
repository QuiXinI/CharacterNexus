package ru.quasaris.characternexus.backend.cropper

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.vector.ImageVector
import dev.chrisbanes.haze.HazeInputScale
import ru.quasaris.characternexus.backend.AppScaleProvider
import ru.quasaris.characternexus.backend.LocalAppScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import ru.quasaris.characternexus.ui.BackHandler
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarCropperContent(
    imageBitmap: ImageBitmap,
    onCrop: (ImageBitmap) -> Unit,
    onDismiss: () -> Unit,
) {
    val hazeState: HazeState? = null
    val forceBlurEnabled = false
    val scope = rememberCoroutineScope()
    val imageCropper = rememberImageCropper()
    
    val state = imageCropper.cropState ?: return
    val useHaze = (hazeState != null) && forceBlurEnabled

    AppScaleProvider(LocalAppScale.current) {
        BackHandler(onBack = onDismiss)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (useHaze) Color.Black.copy(alpha = 0.0f) else Color.Black)
                .run {
                    if (useHaze && hazeState != null) {
                        this.hazeEffect(state = hazeState) {
                            style = HazeStyle(
                                blurRadius = 24.dp,
                                tints = listOf(HazeTint(Color.Black.copy(alpha = 0.0f)))
                            )
                            inputScale = HazeInputScale.Fixed(0.6f)
                        }
                    } else this
                }
        ) {
            // Bottom Layer: Image and Overlay
            ImageCropperPreview(
                image = imageBitmap,
                state = state,
            )

            // Top Layer: UI Controls
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent.copy(alpha = 0.0f),
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text("Кадрирование", style = MaterialTheme.typography.titleMedium, color = Color.White) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Отмена", tint = Color.White)
                            }
                        },
                        actions = {
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        val result = imageCropper.cropImage(imageBitmap)
                                        if (result is ImageCropResult.Success) {
                                            onCrop(result.bitmap)
                                        }
                                    }
                                }
                            ) {
                                Text("Готово", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent.copy(alpha = 0.0f),
                            titleContentColor = Color.White,
                            navigationIconContentColor = Color.White,
                            actionIconContentColor = Color.White
                        )
                    )
                },
                bottomBar = {
                    BottomAppBar(
                        containerColor = Color.Transparent.copy(alpha = 0.0f),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(80.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CropperButton(
                                onClick = { state.rotateCCW() },
                                icon = Icons.AutoMirrored.Filled.RotateLeft,
                                contentDescription = "Влево"
                            )
                            CropperButton(
                                onClick = { state.rotateCW() },
                                icon = Icons.AutoMirrored.Filled.RotateRight,
                                contentDescription = "Вправо"
                            )
                            
                            CropperButton(
                                onClick = { state.flipVertical() },
                                icon = Icons.Default.Flip,
                                contentDescription = "Отразить Г",
                                rotation = 90f
                            )
                            CropperButton(
                                onClick = { state.flipHorizontal() },
                                icon = Icons.Default.Flip,
                                contentDescription = "Отразить В"
                            )
                        }
                    }
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .cropperInteractions(state)
                )
            }
        }
    }
}

@Composable
fun RowScope.CropperButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    rotation: Float = 0f
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .padding(4.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.2f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = contentDescription,
                modifier = Modifier
                    .size(32.dp)
                    .run { if (rotation != 0f) this.rotate(rotation) else this },
                tint = Color.White
            )
        }
    }
}
