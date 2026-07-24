package ru.quasaris.characters.master.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

import ru.quasaris.characters.master.Character

@Composable
fun BioTab(character: Character) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Column {
            Text(
                text = "Вкладка Био пока в разработке",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (character.avatarUrl != null) {
                Text(
                    text = "Ссылка на аватарку LSS:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = character.avatarUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
