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

    BackHandler(onBack = onCancel)

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
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
                        containerColor = if (useHaze) Color.Transparent else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.run {
                        if (useHaze && hazeState != null) hazeEffect(state = hazeState) { } else this
                    }
                )
            },
            bottomBar = {
                BottomAppBar(
                    containerColor = if (useHaze) Color.Transparent else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    modifier = Modifier.run {
                        if (useHaze && hazeState != null) hazeEffect(state = hazeState) { } else this
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalIconButton(onClick = { state.rotateCCW() }) {
                            Icon(Icons.AutoMirrored.Filled.RotateLeft, contentDescription = "Влево")
                        }
                        FilledTonalIconButton(onClick = { state.rotateCW() }) {
                            Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Вправо")
                        }
                        
                        // Swapped horizontal/vertical as per user feedback
                        FilledTonalIconButton(onClick = { state.flipVertical() }) {
                            Icon(Icons.Default.SwapVert, contentDescription = "Отразить Г")
                        }
                        FilledTonalIconButton(onClick = { state.flipHorizontal() }) {
                            Icon(Icons.Default.Flip, contentDescription = "Отразить В")
                        }
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
