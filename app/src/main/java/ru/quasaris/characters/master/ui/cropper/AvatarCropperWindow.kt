package ru.quasaris.characters.master.ui.cropper

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.shape.RoundedCornerShape
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarCropperWindow(
    imageToCrop: Bitmap,
    hazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    onCropSuccess: (Bitmap) -> Unit,
    onCancel: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val imageCropper = rememberImageCropper()
    val imageBitmap = remember(imageToCrop) { imageToCrop.asImageBitmap() }
    
    val state = imageCropper.cropState ?: return

    val useHaze = (hazeState != null) && forceBlurEnabled

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BackHandler(onBack = onCancel)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (useHaze) Color.Black.copy(alpha = 0.1f) else Color.Black)
                .run {
                    if (useHaze && hazeState != null) {
                        this.hazeEffect(state = hazeState) {
                            style = dev.chrisbanes.haze.HazeStyle(
                                blurRadius = 24.dp,
                                tints = listOf(dev.chrisbanes.haze.HazeTint(Color.Black.copy(alpha = 0.1f)))
                            )
                            inputScale = dev.chrisbanes.haze.HazeInputScale.Fixed(0.6f)
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
                containerColor = Color.Transparent,
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text("Кадрирование", style = MaterialTheme.typography.titleMedium) },
                        navigationIcon = {
                            IconButton(onClick = onCancel) {
                                Icon(Icons.Default.Close, contentDescription = "Отмена")
                            }
                        },
                        actions = {
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        val result = imageCropper.cropImage(imageBitmap)
                                        if (result is ImageCropResult.Success) {
                                            onCropSuccess(result.bitmap.asAndroidBitmap())
                                        }
                                    }
                                }
                            ) {
                                Text("Готово", style = MaterialTheme.typography.labelLarge)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                },
                bottomBar = {
                    BottomAppBar(
                        containerColor = Color.Transparent,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(80.dp) // Coin-like size height
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Larger buttons like coins
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
                            
                            // User wants flip vertical (swap top/bottom) rotated 90 deg
                            CropperButton(
                                onClick = { state.flipVertical() },
                                icon = Icons.Default.Flip,
                                contentDescription = "Отразить Г",
                                rotation = 90f
                            )
                            // User wants flip horizontal (swap left/right)
                            CropperButton(
                                onClick = { state.flipHorizontal() },
                                icon = Icons.Default.Flip,
                                contentDescription = "Отразить В"
                            )
                        }
                    }
                }
            ) { padding ->
                // Interaction area is the content of the scaffold
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = contentDescription,
                modifier = Modifier
                    .size(32.dp)
                    .run { if (rotation != 0f) this.rotate(rotation) else this },
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
