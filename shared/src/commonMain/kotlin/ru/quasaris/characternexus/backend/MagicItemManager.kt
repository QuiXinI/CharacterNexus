package ru.quasaris.characternexus.backend

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.Path
import ru.quasaris.characternexus.getAppDataDir
import ru.quasaris.characternexus.platformFileSystem
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.util.*

class MagicItemManager(private val moduleManager: ModuleManager? = null) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val glossaryDir = getAppDataDir().div("glossary").div("magic_items")
    private var cachedItems: MutableList<GameMagicItem>? = null

    init {
        if (!platformFileSystem.exists(glossaryDir)) {
            platformFileSystem.createDirectories(glossaryDir)
        }
        initializeBuiltInModules()
    }

    private fun initializeBuiltInModules() {
        val srdModuleId = "srd_potions_5.2"
        val manifest = ModuleManifest(
            id = srdModuleId,
            name = "Зелья SRD 5.2",
            version = "1.0",
            description = "Встроенный модуль с 4 зельями лечения из SRD 5.2. Данный модуль включает материалы из System Reference Document 5.2 (SRD 5.2) от Wizards of the Coast LLC, используемые по лицензии Creative Commons Attribution 4.0 International (CC-BY-4.0)."
        )

        val description = "Вы восстанавливаете себе Хиты, когда выпиваете это зелье. Количетсво хитов указано в формуле."
        val defaultItems = listOf(
            GameMagicItem(
                id = "potion_of_healing_common",
                name = "Зелье лечения",
                rarity = PotionRarity.COMMON,
                description = description,
                formula = "2d4+2",
                damageTypes = listOf(DamageType.HEALING),
                iconIndex = 1,
                sourceModuleId = srdModuleId
            ),
            GameMagicItem(
                id = "potion_of_healing_greater",
                name = "Большое зелье лечения",
                rarity = PotionRarity.UNCOMMON,
                description = description,
                formula = "4d4+4",
                damageTypes = listOf(DamageType.HEALING),
                iconIndex = 2,
                sourceModuleId = srdModuleId
            ),
            GameMagicItem(
                id = "potion_of_healing_superior",
                name = "Отличное зелье лечения",
                rarity = PotionRarity.RARE,
                description = description,
                formula = "8d4+8",
                damageTypes = listOf(DamageType.HEALING),
                iconIndex = 3,
                sourceModuleId = srdModuleId
            ),
            GameMagicItem(
                id = "potion_of_healing_supreme",
                name = "Превосходное зелье лечения",
                rarity = PotionRarity.VERY_RARE,
                description = description,
                formula = "10d4+20",
                damageTypes = listOf(DamageType.HEALING),
                iconIndex = 4,
                sourceModuleId = srdModuleId
            )
        )

        val allItems = loadItems()
        var changed = false

        defaultItems.forEach { item ->
            val exists = allItems.any { 
                (it.id == item.id) || (it.name == item.name && it.sourceModuleId == item.sourceModuleId) 
            }
            if (!exists) {
                addOrUpdateItem(item)
                changed = true
            }
        }

        if (changed) {
            cachedItems = null
        }

        // Always ensure manifest is updated and all items are in it
        val currentSrdItems = loadItems().filter { it.sourceModuleId == srdModuleId }
        if (currentSrdItems.isNotEmpty()) {
            // Rebuild manifest contents to be sure
            val contents = currentSrdItems.map { 
                ModuleContent("magic_item", it.id ?: "", slugify(it.name ?: "") + ".json")
            }
            
            val finalManifest = manifest.copy(contents = contents)
            moduleManager?.addOrUpdateModule(finalManifest)
        }
    }

    fun slugify(name: String): String {
        return name.lowercase()
            .replace(" ", "_")
            .replace(Regex("[^a-zа-я0-9_'\\-.()]"), "")
            .ifBlank { "unnamed" }
    }

    private fun getFileForItem(item: GameMagicItem): Path {
        val identifier = slugify(item.name ?: item.id ?: "unnamed")
        return glossaryDir.div("$identifier.json")
    }

    fun loadItems(): List<GameMagicItem> {
        val currentCached = cachedItems
        if (currentCached != null) return currentCached
        
        val items = mutableListOf<GameMagicItem>()
        try {
            if (platformFileSystem.exists(glossaryDir)) {
                platformFileSystem.list(glossaryDir).forEach { file ->
                    if (file.name.endsWith(".json")) {
                        try {
                            val content = platformFileSystem.read(file) { readUtf8() }
                            val item = json.decodeFromString<GameMagicItem>(content)
                            items.add(item)
                        } catch (e: Exception) {
                            e.log()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.log()
        }
        
        cachedItems = items.sortedBy { it.name }.toMutableList()
        return cachedItems!!
    }

    fun addOrUpdateItem(item: GameMagicItem) {
        val allItems = loadItems()
        
        val existingMatch = allItems.find { it.id == item.id }
            ?: allItems.find { 
                it.name.equals(item.name, ignoreCase = true) && 
                it.sourceModuleId == item.sourceModuleId
            }
        
        val itemToSave = if (existingMatch != null) {
            item.copy(id = existingMatch.id ?: item.id)
        } else {
            item
        }

        if (existingMatch != null) {
            val oldFile = getFileForItem(existingMatch)
            val newFile = getFileForItem(itemToSave)
            if (oldFile != newFile) {
                platformFileSystem.delete(oldFile)
            }
        }

        try {
            val file = getFileForItem(itemToSave)
            val content = json.encodeToString(itemToSave)
            platformFileSystem.write(file) {
                writeUtf8(content)
            }
        } catch (e: Exception) {
            e.log()
        }
        
        cachedItems = null // Invalidate cache
    }

    fun deleteItem(itemId: String) {
        val items = loadItems()
        val itemToDelete = items.find { it.id == itemId }
        itemToDelete?.let {
            val file = getFileForItem(it)
            if (platformFileSystem.exists(file)) {
                platformFileSystem.delete(file)
            }
        }
        cachedItems = null // Invalidate cache
    }

    fun syncCustomPotions(potions: List<PotionState>) {
        val customModuleId = "custom_potions"
        val customPotions = potions.filter { it.sourceModuleId == customModuleId }
        
        if (customPotions.isNotEmpty()) {
            val manifest = ModuleManifest(
                id = customModuleId,
                name = "Пользовательские предметы",
                version = "1.0",
                description = "Ваши созданные вручную предметы и зелья. Это служебный модуль, не надо его удалять. Его удаление может привести к потере данных"
            )
            moduleManager?.addOrUpdateModule(manifest)
        }

        customPotions.forEach { potion ->
            val existingItems = loadItems()
            val match = existingItems.find { it.name.equals(potion.name, ignoreCase = true) }
            
            val newItem = GameMagicItem(
                id = potion.id, // Using potion ID as definition ID for custom ones
                name = potion.name,
                type = MagicItemType.POTION,
                rarity = potion.rarity,
                description = potion.description,
                formula = potion.formula,
                damageTypes = potion.damageTypes,
                iconIndex = potion.iconIndex,
                colorHex = potion.colorHex,
                sourceModuleId = customModuleId
            )

            if (match == null) {
                addOrUpdateItem(newItem)
                moduleManager?.addComponentToModule(customModuleId, "magic_item", newItem.id ?: "", slugify(newItem.name ?: "") + ".json")
            } else {
                // Check if data matches
                val dataMatches = match.formula == potion.formula &&
                        match.description == potion.description &&
                        match.iconIndex == potion.iconIndex &&
                        match.colorHex == potion.colorHex &&
                        match.rarity == potion.rarity
                
                if (!dataMatches) {
                    // Find unique name
                    var counter = 1
                    var uniqueName = "${potion.name} ($counter)"
                    while (existingItems.any { it.name.equals(uniqueName, ignoreCase = true) }) {
                        counter++
                        uniqueName = "${potion.name} ($counter)"
                    }
                    val uniqueItem = newItem.copy(id = generateUuid(), name = uniqueName)
                    addOrUpdateItem(uniqueItem)
                    moduleManager?.addComponentToModule(customModuleId, "magic_item", uniqueItem.id ?: "", slugify(uniqueItem.name ?: "") + ".json")
                } else {
                    // Ensure it's in module manifest
                    moduleManager?.addComponentToModule(customModuleId, "magic_item", match.id ?: "", slugify(match.name ?: "") + ".json")
                }
            }
        }
    }
}
