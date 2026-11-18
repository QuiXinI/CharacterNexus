package ru.quasaris.characternexus.backend.glossary

import ru.quasaris.characternexus.model.CharacterClass

/**
 * Реестр вшитых подклассов D&D 5.5 и сопутствующая логика объединения с модулями.
 */
object SubclassRegistry {

    /**
     * Вшитые подклассы 5.5, сгруппированные по названию класса на русском языке.
     */
    val BUILT_IN_SUBCLASSES: Map<String, List<String>> = mapOf(
        "Бард" to listOf(
            "Коллегия Гламура",
            "Коллегия Доблести",
            "Коллегия Духов",
            "Коллегия Знаний",
            "Коллегия Луны",
            "Коллегия Танцев"
        ),
        "Варвар" to listOf(
            "Путь Берсеркера",
            "Путь Дикого Сердца",
            "Путь Духа-Хранителя",
            "Путь Мирового Древа",
            "Путь Фанатика",
            "Путь Элементов",
            "Шаман"
        ),
        "Воин" to listOf(
            "Баннерет",
            "Магический Лучник",
            "Мастер Боевых Искусств",
            "Мистический Рыцарь",
            "Пси-Воин",
            "Чемпион"
        ),
        "Волшебник" to listOf(
            "Воплотитель",
            "Вызыватель",
            "Иллюзионист",
            "Некромант",
            "Оградитель",
            "Очарователь",
            "Певец Клинка",
            "Преобразователь",
            "Прорицатель"
        ),
        "Друид" to listOf(
            "Круг Звёзд",
            "Круг Земли",
            "Круг Луны",
            "Круг Моря"
        ),
        "Жрец" to listOf(
            "Домен Войны",
            "Домен Жизни",
            "Домен Знаний",
            "Домен Магии",
            "Домен Обмана",
            "Домен Послушания",
            "Домен Света",
            "Домен Тьмы",
            "Домен Упокоения"
        ),
        "Изобретатель" to listOf(
            "Алхимик",
            "Артиллерист",
            "Боевой Кузнец",
            "Бронник",
            "Картограф",
            "Реаниматор"
        ),
        "Колдун" to listOf(
            "Покровитель Архифея",
            "Покровитель Великий Древний",
            "Покровитель Исчадие",
            "Покровитель Небожитель",
            "Покровитель Нежить",
            "Покровитель Реликт"
        ),
        "Монах" to listOf(
            "Воитель Милосердия",
            "Воитель Мистических Искусств",
            "Воитель Мистического Варева",
            "Воитель Открытой Руки",
            "Воитель Элементов",
            "Воитель Тени"
        ),
        "Паладин" to listOf(
            "Клятва Благородных Гениев",
            "Клятва Древних",
            "Клятва Мести",
            "Клятва Преданности",
            "Клятва Света",
            "Клятва Славы",
            "Рыцарь Смерти",
            "Клятва Чёрного Клинка"
        ),
        "Плут" to listOf(
            "Ассасин",
            "Вор",
            "Клинок Души",
            "Магический Трикстер",
            "Наследник Троицы",
            "Сорвиголова",
            "Фантом"
        ),
        "Следопыт" to listOf(
            "Зимний Странник",
            "Охотник",
            "Повелитель Зверей",
            "Пустотный Смотритель",
            "Сумеречный Сталкер",
            "Фейский Странник",
            "Героический Охотник На Демонов",
            "Охотник На Демонов"
        ),
        "Сородич" to listOf(
            "Бруха",
            "Вентру",
            "Гангрел",
            "Ласомбра",
            "Носферату",
            "Тореадор"
        ),
        "Чародей" to listOf(
            "Аберрантное Чародейство",
            "Драконье Чародейство",
            "Заводное Чародейство",
            "Теневое Чародейство",
            "Чародейство Дикой Магии",
            "Чаропламенное Чародейство"
        )
    )

    /**
     * Определение родительского класса [CharacterClass] по имени подкласса.
     */
    fun getClassForSubclass(
        subclassName: String,
        customSubclasses: List<SubclassDefinition> = emptyList(),
        classDefinitions: List<ClassDefinition> = emptyList()
    ): CharacterClass? {
        val trimmed = subclassName.trim()
        if (trimmed.isBlank()) return null

        // 1. Поиск среди вшитых
        for ((clsDisplayName, list) in BUILT_IN_SUBCLASSES) {
            if (list.any { it.equals(trimmed, ignoreCase = true) }) {
                return CharacterClass.entries.find { it.displayName.equals(clsDisplayName, ignoreCase = true) }
            }
        }

        // 2. Поиск среди кастомных подклассов из модулей
        val customMatch = customSubclasses.find { it.name.trim().equals(trimmed, ignoreCase = true) }
        if (customMatch != null) {
            val clsDef = classDefinitions.find { it.id == customMatch.classId }
            if (clsDef != null) {
                CharacterClass.entries.find { it.displayName.equals(clsDef.name, ignoreCase = true) }?.let { return it }
            }
        }

        return null
    }

    /**
     * Возвращает полный словарь «Класс -> Список подклассов», объединяя вшитые 5.5 и
     * подклассы из загруженных модулей, которые не пересекаются с вшитыми по имени.
     */
    fun getSubclassesGrouped(
        customSubclasses: List<SubclassDefinition> = emptyList(),
        classDefinitions: List<ClassDefinition> = emptyList()
    ): Map<String, List<String>> {
        val result = LinkedHashMap<String, MutableList<String>>()

        // Вшитые подклассы 5.5
        BUILT_IN_SUBCLASSES.forEach { (clsName, subList) ->
            result[clsName] = subList.toMutableList()
        }

        // Кастомные подклассы из модулей
        val classIdToName = classDefinitions.associate { it.id to it.name }
        customSubclasses.forEach { sub ->
            val subName = sub.name.trim()
            if (subName.isBlank()) return@forEach

            val clsName = classIdToName[sub.classId] ?: sub.classId.ifBlank { "Другое" }
            val targetList = result.getOrPut(clsName) { mutableListOf() }
            if (targetList.none { it.equals(subName, ignoreCase = true) }) {
                targetList.add(subName)
            }
        }

        return result
    }

    /**
     * Плоский уникальный список всех названий подклассов.
     */
    fun getAllSubclassNames(
        customSubclasses: List<SubclassDefinition> = emptyList(),
        classDefinitions: List<ClassDefinition> = emptyList()
    ): List<String> {
        return getSubclassesGrouped(customSubclasses, classDefinitions).values.flatten().distinct()
    }
}
