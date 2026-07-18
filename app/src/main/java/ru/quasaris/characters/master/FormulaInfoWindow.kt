package ru.quasaris.characters.master

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormulaInfoWindow(
    onNavigateBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("СПРАВОЧНИК ФОРМУЛ", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        containerColor = colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InfoCard(
                title = "ВАЖНЫЕ ПРЕДУПРЕЖДЕНИЯ",
                description = "Пожалуйста не выделяйте ничего стандартными инструментами android в окне настроек ресурса",
                items = listOf(
                    "Причины" to "Скорее всего ваше приложение просто крашнется",
                    "Если вы хотите помочь" to "Можете включить в настройках отображение информации для отладки. При вылете прилоежение автоматически копирует в буфер обмена причину вылета.\nПрежде чем отправлять причиину вылета нам, прочитайте её. Если в ней есть строка *layouts are not part of the same hierarchy*, не надо отправлять, об этой ошибке мы уже знаем"
                )
            )

            InfoCard(
                title = "ОСНОВНЫЕ ХАРАКТЕРИСТИКИ",
                description = "Используйте короткие названия в квадратных скобках. Поддерживаются как модификаторы, так и полные значения.",
                items = listOf(
                    "Модификаторы" to "[СИЛ], [ЛОВ], [ТЕЛ], [ИНТ], [МУД], [ХАР]\n[STR], [DEX], [CON], [INT], [WIS], [CHA]",
                    "Значения" to "[СИЛ ЗНАЧ], [ЛОВ ЗНАЧ]...\n[STR SCR], [DEX SCR]..."
                )
            )

            InfoCard(
                title = "БОНУСЫ МАСТЕРСТВА И УРОВЕНЬ",
                description = "Автоматически рассчитываются от уровня или берутся из настроек персонажа.",
                items = listOf(
                    "Бонус мастерства" to "[БМ], [PB], [PROF]",
                    "Чистый БМ (без бонусов)" to "[НАСТ БМ], [REAL PB]",
                    "Уровень персонажа" to "[УР], [LVL], [LEVEL]"
                )
            )

            InfoCard(
                title = "МАГИЧЕСКИЕ ПАРАМЕТРЫ",
                description = "Берутся из настроек заклинаний персонажа.",
                items = listOf(
                    "Бонус к атаке заклинанием" to "[МАГ АТК БОН], [MAG ATC BON]",
                    "Бонус к СЛ спаброска" to "[МАГ СПАС БОН], [MAG SAVE BON]",
                    "Модификатор заклинат. хар-ки" to "[МАГ МОД], [MAG MOD]"
                )
            )

            InfoCard(
                title = "ДИНАМИЧЕСКИЕ ПОКАЗАТЕЛИ",
                description = "Текущие значения состояния персонажа.",
                items = listOf(
                    "Хиты (текущие / максимальные / временные)" to "[ХП], [HP]\n[МХП], [MHP]\n[ВХП], [THP]",
                    "Класс Доспеха (текущий)" to "[КД], [AC]",
                    "Опыт" to "[ОП], [XP]",
                    "Истощение" to "[ИСТ], [EX]",
                    "Количество состояний" to "[СОСТ], [COND]"
                )
            )

            InfoCard(
                title = "МАТЕМАТИЧЕСКИЕ ФУНКЦИИ",
                description = "Поддерживаются вложенные вычисления и функции выбора.",
                items = listOf(
                    "Операции" to "+, -, *, / (целочисленное)",
                    "Максимум / Минимум" to "МАКС(a, b) / МИН(a, b)\nMAX(a, b) / MIN(a, b)",
                    "Округление" to "ВЕРХ(a) / НИЗ(a)\nCEIL(a) / FLOOR(a)"
                )
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun InfoCard(
    title: String,
    description: String,
    items: List<Pair<String, String>>
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
            
            items.forEachIndexed { index, item ->
                if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp, color = colorScheme.outlineVariant)
                
                Column {
                    Text(
                        text = item.first,
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.onSurface.copy(alpha = 0.7f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = item.second,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurface,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}
