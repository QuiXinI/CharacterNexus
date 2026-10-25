package ru.quasaris.characternexus.backend.glossary

/**
 * Расчёт ячеек заклинаний по уровню класса для стандартной прогрессии PHB 5.5.
 * CUSTOM-заклинатели (например, Колдун) эту таблицу не используют — их ячейки
 * задаются вручную в ClassDefinition.spellcasting.customSlots.
 */
object SpellSlotCalculator {

    private fun row(vararg pairs: Pair<Int, Int>): List<SlotCount> =
        pairs.map { SlotCount(slotLevel = it.first, count = it.second) }

    // Индекс 0 = 1 уровень класса, индекс 19 = 20 уровень класса.
    private val FULL_TABLE: List<List<SlotCount>> = run {
        val l11 = row(1 to 4, 2 to 3, 3 to 3, 4 to 3, 5 to 2, 6 to 1)
        val l13 = row(1 to 4, 2 to 3, 3 to 3, 4 to 3, 5 to 2, 6 to 1, 7 to 1)
        val l15 = row(1 to 4, 2 to 3, 3 to 3, 4 to 3, 5 to 2, 6 to 1, 7 to 1, 8 to 1)
        val l17 = row(1 to 4, 2 to 3, 3 to 3, 4 to 3, 5 to 2, 6 to 1, 7 to 1, 8 to 1, 9 to 1)
        listOf(
            row(1 to 2),                                                                 // 1
            row(1 to 3),                                                                 // 2
            row(1 to 4, 2 to 2),                                                         // 3
            row(1 to 4, 2 to 3),                                                         // 4
            row(1 to 4, 2 to 3, 3 to 2),                                                 // 5
            row(1 to 4, 2 to 3, 3 to 3),                                                 // 6
            row(1 to 4, 2 to 3, 3 to 3, 4 to 1),                                         // 7
            row(1 to 4, 2 to 3, 3 to 3, 4 to 2),                                         // 8
            row(1 to 4, 2 to 3, 3 to 3, 4 to 3, 5 to 1),                                 // 9
            row(1 to 4, 2 to 3, 3 to 3, 4 to 3, 5 to 2),                                 // 10
            l11,                                                                         // 11
            l11,                                                                         // 12
            l13,                                                                         // 13
            l13,                                                                         // 14
            l15,                                                                         // 15
            l15,                                                                         // 16
            l17,                                                                         // 17
            row(1 to 4, 2 to 3, 3 to 3, 4 to 3, 5 to 3, 6 to 1, 7 to 1, 8 to 1, 9 to 1),  // 18
            row(1 to 4, 2 to 3, 3 to 3, 4 to 3, 5 to 3, 6 to 2, 7 to 1, 8 to 1, 9 to 1),  // 19
            row(1 to 4, 2 to 3, 3 to 3, 4 to 3, 5 to 3, 6 to 2, 7 to 2, 8 to 1, 9 to 1)   // 20
        )
    }

    /**
     * Ячейки заклинаний на заданном уровне класса.
     */
    fun slotsFor(config: SpellcastingConfig, classLevel: Int): List<SlotCount> {
        val level = classLevel.coerceIn(1, MAX_CLASS_LEVEL)
        return when (config.type) {
            SpellcastingType.NONE -> emptyList()
            SpellcastingType.FULL -> FULL_TABLE[level - 1]
            SpellcastingType.HALF -> {
                // Полукастер: 1→1, 2→1, 3→2, 4→2, 5→3 ... — округление вверх по парам уровней.
                val effective = ((level + 1) / 2).coerceIn(1, MAX_CLASS_LEVEL)
                FULL_TABLE[effective - 1]
            }
            SpellcastingType.CUSTOM -> config.customSlots
                .firstOrNull { it.level == level }
                ?.slots
                .orEmpty()
        }
    }

    /**
     * Суммарное количество ячеек по уровню заклинания на заданном уровне класса,
     * отсортированное по уровню заклинания. Используется для отображения
     * в сворачиваемом блоке "Количество ячеек".
     */
    fun totalsByLevel(config: SpellcastingConfig, classLevel: Int): Map<Int, Int> =
        slotsFor(config, classLevel)
            .groupBy { it.slotLevel }
            .mapValues { entry -> entry.value.sumOf { it.count } }
            .toList()
            .sortedBy { it.first }
            .toMap()

    /**
     * Суммарное число ячеек всех уровней на заданном уровне класса — то самое
     * число, которое выводится справа от таблицы прогрессии рядом с заголовком
     * раскрывающегося блока "Количество ячеек".
     */
    fun totalCount(config: SpellcastingConfig, classLevel: Int): Int =
        slotsFor(config, classLevel).sumOf { it.count }
}
