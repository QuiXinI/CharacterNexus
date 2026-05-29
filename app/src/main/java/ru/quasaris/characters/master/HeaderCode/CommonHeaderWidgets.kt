package ru.quasaris.characters.master.HeaderCode

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.quasaris.characters.master.R

val SquirclePath = GenericShape { size, _ ->
    val r = size.width * 0.25f
    moveTo(r, 0f)
    lineTo(size.width - r, 0f)
    quadraticTo(size.width, 0f, size.width, r)
    lineTo(size.width, size.height - r)
    quadraticTo(size.width, size.height, size.width - r, size.height)
    lineTo(r, size.height)
    quadraticTo(0f, size.height, 0f, size.height - r)
    lineTo(0f, r)
    quadraticTo(0f, 0f, r, 0f)
    close()
}

@Composable
fun StatIconBox(value: String, iconRes: Int, onClick: () -> Unit = {}) {
    val colorScheme = MaterialTheme.colorScheme
    Box(modifier = Modifier.size(42.dp).clickable(remember { MutableInteractionSource() }, null, onClick = onClick), contentAlignment = Alignment.Center) {
        val tint = colorScheme.primary.copy(alpha = 0.5f)
        if (iconRes == R.drawable.ic_sword) {
            Box(Modifier.fillMaxSize()) {
                Image(painterResource(R.drawable.ic_sword), null, modifier = Modifier.size(42.dp), colorFilter = ColorFilter.tint(tint))
                Image(painterResource(R.drawable.ic_sword), null, modifier = Modifier.size(42.dp).graphicsLayer(scaleX = -1f), colorFilter = ColorFilter.tint(tint))
            }
        } else Image(painterResource(iconRes), null, modifier = Modifier.fillMaxSize(), colorFilter = ColorFilter.tint(tint))
        Text(value, fontSize = 15.sp, color = colorScheme.onSurface, fontWeight = FontWeight.Bold, style = TextStyle(shadow = Shadow(colorScheme.surface, Offset.Zero, 14f)))
    }
}
