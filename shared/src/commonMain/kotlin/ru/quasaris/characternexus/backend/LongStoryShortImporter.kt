package ru.quasaris.characternexus.backend

import kotlinx.serialization.json.*
import ru.quasaris.characternexus.model.*

object LongStoryShortImporter {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun isLongStoryShort(jsonElement: JsonElement): Boolean {
        return try {
            if (jsonElement !is JsonObject) return false
            jsonElement["jsonType"]?.jsonPrimitive?.content == "character" && jsonElement.containsKey("data")
        } catch (e: Exception) {
            false
        }
    }

    private fun JsonElement?.safeString(default: String = ""): String {
        return when {
            this == null || this is JsonNull -> default
            this is JsonPrimitive -> content
            else -> default
        }
    }

    private fun mapMode(mode: String?): BonusOperation {
        return when (mode?.lowercase()) {
            "add" -> BonusOperation.ADD
            "subtract" -> BonusOperation.SUBTRACT
            "set" -> BonusOperation.OVERRIDE
            else -> BonusOperation.ADD
        }
    }

    fun parse(jsonElement: JsonElement): Character? {
        return try {
            val root = jsonElement.jsonObject
            val dataElement = root["data"] ?: return null
            val data = if (dataElement is JsonObject) {
                dataElement
            } else {
                json.parseToJsonElement(dataElement.jsonPrimitive.content).jsonObject
            }

            val name = data["name"]?.jsonObject?.get("value").safeString("Новый персонаж")
            val info = data["info"]?.jsonObject ?: buildJsonObject {}
            val charClass = info["charClass"]?.jsonObject?.get("value").safeString()
            val race = info["race"]?.jsonObject?.get("value").safeString()
            val level = info["level"]?.jsonObject?.get("value").safeString("1")
            val experience = info["experience"]?.jsonObject?.get("value").safeString("0")

            val stats = data["stats"]?.jsonObject ?: buildJsonObject {}
            val strength = stats["str"]?.jsonObject?.get("score").safeString("10")
            val dexterity = stats["dex"]?.jsonObject?.get("score").safeString("10")
            val constitution = stats["con"]?.jsonObject?.get("score").safeString("10")
            val intelligence = stats["int"]?.jsonObject?.get("score").safeString("10")
            val wisdom = stats["wis"]?.jsonObject?.get("score").safeString("10")
            val charisma = stats["cha"]?.jsonObject?.get("score").safeString("10")

            val avatarObj = data["avatar"]?.jsonObject
            val avatarUrl = avatarObj?.get("jpeg")?.jsonPrimitive?.content ?: avatarObj?.get("webp")?.jsonPrimitive?.content

            val saves = data["saves"]?.jsonObject ?: buildJsonObject {}
            val strProf = saves["str"]?.jsonObject?.get("isProf")?.jsonPrimitive?.boolean ?: false
            val dexProf = saves["dex"]?.jsonObject?.get("isProf")?.jsonPrimitive?.boolean ?: false
            val conProf = saves["con"]?.jsonObject?.get("isProf")?.jsonPrimitive?.boolean ?: false
            val intProf = saves["int"]?.jsonObject?.get("isProf")?.jsonPrimitive?.boolean ?: false
            val wisProf = saves["wis"]?.jsonObject?.get("isProf")?.jsonPrimitive?.boolean ?: false
            val chaProf = saves["cha"]?.jsonObject?.get("isProf")?.jsonPrimitive?.boolean ?: false

            val vitality = data["vitality"]?.jsonObject ?: buildJsonObject {}
            val maxHp = vitality["hp-max"]?.getFieldValue().safeString("0")
            val currentHp = vitality["hp-current"]?.getFieldValue().safeString("0")
            val tempHp = vitality["hp-temp"]?.getFieldValue().safeString("0")
            val isShieldActive = vitality["shield"]?.getFieldValue()?.jsonPrimitive?.booleanOrNull ?: false

            val acFormula = vitality["ac"]?.getFieldValue().safeString("10 + [DEX]")
            val initFormula = vitality["initiative"]?.getFieldValue().safeString("[DEX]")

            val armorClassEntries = mutableListOf(ArmorClassEntry(name = "Базовый КД", formula = acFormula))
            val initiativeEntries = mutableListOf(InitiativeEntry(name = "Базовая Инициатива", formula = initFormula))
            val speedEntries = mutableListOf(SpeedEntry(name = "Базовая Скорость", formula = vitality["speed"]?.getFieldValue().safeString("30")))

            val coins = data["coins"]?.jsonObject ?: buildJsonObject {}
            val wallet = Wallet(
                gold = coins["gp"]?.jsonObject?.get("value")?.jsonPrimitive?.doubleOrNull ?: 0.0,
                silver = coins["sp"]?.jsonObject?.get("value")?.jsonPrimitive?.doubleOrNull ?: 0.0,
                copper = coins["cp"]?.jsonObject?.get("value")?.jsonPrimitive?.doubleOrNull ?: 0.0,
                platinum = coins["pp"]?.jsonObject?.get("value")?.jsonPrimitive?.doubleOrNull ?: 0.0,
                electrum = coins["ep"]?.jsonObject?.get("value")?.jsonPrimitive?.doubleOrNull ?: 0.0
            )

            // Skills
            val skills = data["skills"]?.jsonObject ?: buildJsonObject {}
            val skilledProficiencies = mutableListOf<String>()
            val skilledExpertise = mutableListOf<String>()

            val skillMap = mapOf(
                "acrobatics" to "Акробатика",
                "animal handling" to "Уход за животными",
                "arcana" to "Магия",
                "athletics" to "Атлетика",
                "deception" to "Обман",
                "history" to "История",
                "insight" to "Проницательность",
                "intimidation" to "Запугивание",
                "investigation" to "Анализ",
                "medicine" to "Медицина",
                "nature" to "Природа",
                "perception" to "Внимательность",
                "performance" to "Выступление",
                "persuasion" to "Убеждение",
                "religion" to "Религия",
                "sleight of hand" to "Ловкость рук",
                "stealth" to "Скрытность",
                "survival" to "Выживание"
            )

            for ((lssName, mpName) in skillMap) {
                val skillObj = skills[lssName]?.jsonObject
                if (skillObj != null) {
                    val isProf = skillObj["isProf"]?.let {
                        when {
                            it is JsonPrimitive && it.isString -> it.content.toIntOrNull() ?: 0
                            it is JsonPrimitive && it.booleanOrNull != null -> if (it.boolean) 1 else 0
                            it is JsonPrimitive -> it.intOrNull ?: 0
                            else -> 0
                        }
                    } ?: 0
                    if (isProf >= 1) skilledProficiencies.add(mpName)
                    if (isProf == 2) {
                        skilledExpertise.add(mpName)
                    }
                }
            }

            // Bonuses
            val allBonuses = data["bonuses"]?.jsonArray ?: buildJsonArray {}
            val statBonuses = mutableListOf<StatBonus>()
            val skillBonuses = mutableListOf<SkillBonus>()
            
            val acBonusExprs = mutableListOf<String>()
            val initBonusExprs = mutableListOf<String>()
            val shieldBonusExprs = mutableListOf<String>()
            var shieldLabel = "Базовый Щит"
            
            val speedBonusExprs = mutableListOf<String>()

            for (b in allBonuses) {
                if (b !is JsonObject) continue
                val target = b["target"].safeString()
                val expr = b["expr"].safeString()
                val disabled = b["disabled"]?.jsonPrimitive?.booleanOrNull ?: false
                val mode = b["mode"]?.jsonPrimitive?.contentOrNull
                
                val label = b["label"].safeString().takeIf { it.isNotBlank() } ?: when {
                    target == "ac" -> "Бонус КД"
                    target == "initiative" -> "Бонус Инициативы"
                    target.startsWith("speed") -> "Бонус Скорости"
                    target.startsWith("skill.") -> {
                        val skillKey = target.removePrefix("skill.")
                        skillMap[skillKey] ?: "Бонус Навыка"
                    }
                    target.startsWith("stat.") -> {
                        val parts = target.split(".")
                        val statName = when(parts.getOrNull(1)) {
                            "str" -> "СИЛ"; "dex" -> "ЛОВ"; "con" -> "ТЕЛ"
                            "int" -> "ИНТ"; "wis" -> "МУД"; "cha" -> "ХАР"
                            else -> ""
                        }
                        val typeName = when(parts.getOrNull(2)) {
                            "save" -> "Спасбросок"
                            "score" -> "Значение"
                            else -> "Проверка"
                        }
                        "$typeName ($statName)".trim()
                    }
                    else -> "Бонус"
                }

                when {
                    target == "ac" -> {
                        if (isActiveBonus(b)) {
                            if (label.contains("щит", ignoreCase = true) || label.contains("shield", ignoreCase = true)) {
                                shieldBonusExprs.add(expr)
                                shieldLabel = label
                            } else {
                                acBonusExprs.add(expr)
                            }
                        }
                    }
                    target == "initiative" -> {
                        if (isActiveBonus(b)) initBonusExprs.add(expr)
                    }
                    target.startsWith("speed") -> {
                        if (isActiveBonus(b)) speedBonusExprs.add(expr)
                    }
                    target == "skill-all" -> {
                        skillMap.values.forEach { mpSkillName ->
                            skillBonuses.add(SkillBonus(name = label, formula = expr, skillName = mpSkillName, operation = mapMode(mode), isActive = !disabled))
                        }
                    }
                    target.startsWith("skill.") -> {
                        val skillKey = target.removePrefix("skill.")
                        val mpSkillName = skillMap[skillKey] ?: skillKey
                        skillBonuses.add(SkillBonus(name = label, formula = expr, skillName = mpSkillName, operation = mapMode(mode), isActive = !disabled))
                    }
                    target == "stat-all.save" -> {
                        Attribute.entries.filter { it != Attribute.NONE }.forEach { attr ->
                            statBonuses.add(StatBonus(name = label, formula = expr, attribute = attr, type = StatBonusType.SAVING_THROW, operation = mapMode(mode), isActive = !disabled))
                        }
                    }
                    target == "stat-all.check" -> {
                        Attribute.entries.filter { it != Attribute.NONE }.forEach { attr ->
                            statBonuses.add(StatBonus(name = label, formula = expr, attribute = attr, type = StatBonusType.ABILITY_CHECK, operation = mapMode(mode), isActive = !disabled))
                        }
                    }
                    target.startsWith("stat.") -> {
                        val parts = target.split(".")
                        if (parts.size >= 3) {
                            val statKey = parts[1]
                            val typeKey = parts[2]
                            val attribute = when (statKey) {
                                "str" -> Attribute.STRENGTH
                                "dex" -> Attribute.DEXTERITY
                                "con" -> Attribute.CONSTITUTION
                                "int" -> Attribute.INTELLIGENCE
                                "wis" -> Attribute.WISDOM
                                "cha" -> Attribute.CHARISMA
                                else -> Attribute.NONE
                            }
                            val bonusType = when(typeKey) {
                                "save" -> StatBonusType.SAVING_THROW
                                "score" -> StatBonusType.CHARACTERISTIC_VALUE
                                else -> StatBonusType.ABILITY_CHECK
                            }
                            statBonuses.add(StatBonus(name = label, formula = expr, attribute = attribute, type = bonusType, operation = mapMode(mode), isActive = !disabled))
                        }
                    }
                }
            }

            // Consolidate AC, Initiative and Speed
            if (acBonusExprs.isNotEmpty()) {
                val baseAc = armorClassEntries[0]
                val fullAcFormula = (listOf(baseAc.formula) + acBonusExprs).joinToString(" + ") { "($it)" }
                armorClassEntries[0] = baseAc.copy(formula = fullAcFormula)
            }
            if (initBonusExprs.isNotEmpty()) {
                val baseInit = initiativeEntries[0]
                val fullInitFormula = (listOf(baseInit.formula) + initBonusExprs).joinToString(" + ") { "($it)" }
                initiativeEntries[0] = baseInit.copy(formula = fullInitFormula)
            }
            if (speedBonusExprs.isNotEmpty()) {
                val baseSpeed = speedEntries[0]
                val fullSpeedFormula = (listOf(baseSpeed.formula) + speedBonusExprs).joinToString(" + ") { "($it)" }
                speedEntries[0] = baseSpeed.copy(formula = fullSpeedFormula)
            }
            
            val shieldEntries = mutableListOf(ShieldEntry(name = "Базовый Щит", formula = "2"))
            if (shieldBonusExprs.isNotEmpty()) {
                val fullShieldFormula = shieldBonusExprs.joinToString(" + ") { "($it)" }
                shieldEntries[0] = ShieldEntry(name = shieldLabel, formula = fullShieldFormula)
            }

            // Attacks
            val weaponsList = data["weaponsList"]?.jsonArray ?: buildJsonArray {}
            val mpAttacks = mutableListOf<AttackEntry>()
            
            for (item in weaponsList) {
                if (item !is JsonObject) continue
                val weaponId = item["id"].safeString()
                
                val abilityStr = item["ability"].safeString("str").lowercase()
                val attribute = when (abilityStr) {
                    "str" -> Attribute.STRENGTH
                    "dex" -> Attribute.DEXTERITY
                    "con" -> Attribute.CONSTITUTION
                    "int" -> Attribute.INTELLIGENCE
                    "wis" -> Attribute.WISDOM
                    "cha" -> Attribute.CHARISMA
                    "none" -> Attribute.NONE
                    else -> Attribute.NONE
                }

                val attackBonuses = mutableListOf<AttackBonus>()
                val damageBonuses = mutableListOf<DamageBonus>()

                for (b in allBonuses) {
                    if (b !is JsonObject) continue
                    val target = b["target"].safeString()
                    val expr = b["expr"].safeString()
                    val disabled = b["disabled"]?.jsonPrimitive?.booleanOrNull ?: false
                    val mode = b["mode"]?.jsonPrimitive?.contentOrNull
                    
                    val label = b["label"].safeString().takeIf { it.isNotBlank() } ?: when {
                        target.contains(".attack") -> "Бонус к попаданию"
                        target.contains(".damage") -> "Бонус к урону"
                        else -> "Бонус"
                    }
                    
                    if (target == "weapon.$weaponId.attack" || target == "weapon-all.attack") {
                        attackBonuses.add(AttackBonus(name = label, formula = expr, operation = mapMode(mode), isActive = !disabled))
                    }
                    if (target == "weapon.$weaponId.damage" || target == "weapon-all.damage") {
                        damageBonuses.add(DamageBonus(name = label, formula = expr, operation = mapMode(mode), isActive = !disabled))
                    }
                }

                mpAttacks.add(
                    AttackEntry(
                        name = item["name"]?.jsonObject?.get("value").safeString("Оружие"),
                        damageFormula = item["dmg"]?.jsonObject?.get("value").safeString(),
                        damageType = item["dmgType"]?.jsonObject?.get("value").safeString(),
                        isProficient = item["isProf"]?.jsonPrimitive?.booleanOrNull ?: true,
                        attribute = attribute,
                        attackBonuses = attackBonuses,
                        damageBonuses = damageBonuses
                    )
                )
            }

            // Notes and Text fields
            val text = data["text"]?.jsonObject ?: buildJsonObject {}
            val lssResources = data["resources"]?.jsonObject ?: buildJsonObject {}
            
            fun extractText(key: String): String {
                val field = text[key]?.jsonObject ?: return ""
                val value = field["value"] ?: return ""
                val contentData = if (value is JsonObject) {
                    value["data"]?.takeIf { it is JsonObject }?.jsonObject
                } else null
                
                return contentData?.let { convertTipTapToMarkdown(it, lssResources) } ?: ""
            }

            val attacksText = extractText("attacks")
            val traitsText = extractText("traits")
            val additionalText = extractText("additional")
            val featsText = extractText("feats")
            val profText = extractText("prof")
            
            val itemsText = extractText("items")
            val equipmentText = extractText("equipment")

            val subInfo = data["subInfo"]?.jsonObject ?: buildJsonObject {}
            val bioShortFields = listOf(
                BioShortField(title = "Предыстория", value = info["background"]?.jsonObject?.get("value").safeString(), widthRatio = 0.5f),
                BioShortField(title = "Мировоззрение", value = info["alignment"]?.jsonObject?.get("value").safeString(), widthRatio = 0.5f),
                BioShortField(title = "Рост", value = subInfo["height"]?.jsonObject?.get("value").safeString(), widthRatio = 0.33f),
                BioShortField(title = "Вес", value = subInfo["weight"]?.jsonObject?.get("value").safeString(), widthRatio = 0.33f),
                BioShortField(title = "Возраст", value = subInfo["age"]?.jsonObject?.get("value").safeString(), widthRatio = 0.33f),
                BioShortField(title = "Кожа", value = subInfo["skin"]?.jsonObject?.get("value").safeString(), widthRatio = 0.33f),
                BioShortField(title = "Глаза", value = subInfo["eyes"]?.jsonObject?.get("value").safeString(), widthRatio = 0.33f),
                BioShortField(title = "Волосы", value = subInfo["hair"]?.jsonObject?.get("value").safeString(), widthRatio = 0.33f)
            )

            val bioLongSections = listOf(
                DynamicNoteState(title = "Предыстория персонажа", content = extractText("background")),
                DynamicNoteState(title = "Союзники и организации", content = extractText("allies")),
                DynamicNoteState(title = "Враги и организации", content = ""), // Not explicitly in LSS template
                DynamicNoteState(title = "Черты характера", content = extractText("personality")),
                DynamicNoteState(title = "Идеалы", content = extractText("ideals")),
                DynamicNoteState(title = "Привязанности", content = extractText("bonds")),
                DynamicNoteState(title = "Слабости", content = extractText("flaws"))
            )

            val skillsAndTraits = listOf(
                DynamicNoteState(title = "Атаки и заклинания", content = attacksText),
                DynamicNoteState(title = "Умения и особенности", content = traitsText),
                DynamicNoteState(title = "Дополнительные способности и умения", content = additionalText),
                DynamicNoteState(title = "Черты", content = featsText),
                DynamicNoteState(title = "Владения", content = profText)
            )

            val inventory = listOf(
                DynamicNoteState(title = "Снаряжение", content = equipmentText),
                DynamicNoteState(title = "Предметы", content = itemsText)
            )

            // Spells
            val mpSpells = mutableListOf<DynamicNoteState>()
            for (levelIdx in 0..9) {
                val spellKey = "spells-level-$levelIdx"
                val spellContent = extractText(spellKey)
                val title = if (levelIdx == 0) "Заговоры" else "$levelIdx уровень"
                mpSpells.add(DynamicNoteState(title = title, content = spellContent))
            }

            // Additional notes
            val notes = mutableListOf<DynamicNoteState>()
            for (i in 1..4) {
                val noteKey = "notes-$i"
                val noteObj = text[noteKey]?.jsonObject
                if (noteObj != null) {
                    val label = noteObj["customLabel"]?.jsonPrimitive?.content ?: "Заметка $i"
                    val content = extractText(noteKey)
                    if (content.isNotBlank()) {
                        notes.add(DynamicNoteState(title = label, content = content))
                    }
                }
            }
            if (notes.isEmpty()) notes.add(DynamicNoteState())

            Character(
                id = (0..Int.MAX_VALUE).random(),
                name = name,
                characterClass = charClass,
                order = race,
                level = level,
                experience = experience,
                strength = strength,
                dexterity = dexterity,
                constitution = constitution,
                intelligence = intelligence,
                wisdom = wisdom,
                charisma = charisma,
                strengthProficient = strProf,
                dexterityProficient = dexProf,
                constitutionProficient = conProf,
                intelligenceProficient = intProf,
                wisdomProficient = wisProf,
                charismaProficient = chaProf,
                armorClassEntries = armorClassEntries,
                activeArmorClassId = armorClassEntries.firstOrNull()?.id,
                initiativeEntries = initiativeEntries,
                activeInitiativeId = initiativeEntries.firstOrNull()?.id,
                speedEntries = speedEntries,
                activeSpeedId = speedEntries.firstOrNull()?.id,
                maxHp = maxHp,
                currentHp = currentHp,
                tempHp = tempHp,
                isShieldActive = isShieldActive,
                shieldEntries = shieldEntries,
                activeShieldId = shieldEntries.firstOrNull()?.id,
                wallet = wallet,
                skilledProficiencies = skilledProficiencies,
                skilledExpertise = skilledExpertise,
                statBonuses = statBonuses,
                skillBonuses = skillBonuses,
                attacks = mpAttacks,
                skillsAndTraits = skillsAndTraits,
                inventory = inventory,
                spells = mpSpells,
                notes = notes,
                bioShortFields = bioShortFields,
                bioLongSections = bioLongSections,
                avatarUrl = avatarUrl
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun isActiveBonus(bonus: JsonObject): Boolean {
        return bonus["disabled"]?.jsonPrimitive?.booleanOrNull != true
    }

    private fun JsonElement.getFieldValue(): JsonElement? {
        return if (this is JsonObject) this["value"] else this
    }

    private fun convertTipTapToMarkdown(node: JsonObject, lssResources: JsonObject, depth: Int = 0): String {
        val sb = StringBuilder()
        val type = node["type"]?.jsonPrimitive?.contentOrNull

        var prefix = ""
        var suffix = ""

        val marks = node["marks"]?.jsonArray
        if (marks != null) {
            // Sort marks so that "link" is processed first to be innermost: **[text](url)**
            val sortedMarks = marks.map { it.jsonObject }.sortedByDescending {
                if (it["type"]?.jsonPrimitive?.content == "link") 1 else 0
            }
            for (mark in sortedMarks) {
                when (mark["type"]?.jsonPrimitive?.content) {
                    "bold" -> { prefix = "**$prefix"; suffix = "${suffix}**" }
                    "italic" -> { prefix = "_$prefix"; suffix = "${suffix}_" }
                    "strike" -> { prefix = "~~$prefix"; suffix = "${suffix}~~" }
                    "link" -> {
                        val href = mark["attrs"]?.jsonObject?.get("href").safeString()
                        prefix = "[$prefix"
                        suffix = "${suffix}]($href)"
                    }
                }
            }
        }

        if (type == "text") {
            sb.append(prefix).append(node["text"]?.jsonPrimitive?.content ?: "").append(suffix)
        } else if (type == "resource") {
            val resId = node["attrs"]?.jsonObject?.get("id").safeString()
            val lssRes = lssResources[resId]?.jsonObject
            if (lssRes != null) {
                val name = lssRes["name"].safeString("Ресурс")
                val cur = lssRes["current"].safeString("0")
                val max = lssRes["maxExpr"].safeString(lssRes["resolvedMax"].safeString("0"))
                val notes = lssRes["notes"].safeString()
                val isLongRest = lssRes["isLongRest"]?.jsonPrimitive?.booleanOrNull ?: false
                
                val lrParam = if (isLongRest) " | lr=all" else ""
                val notesParam = if (notes.isNotEmpty()) " | notes=$notes" else ""
                
                sb.append("{Ресурс: $name | cur=$cur | max=$max$lrParam$notesParam}")
            }
        } else if (type == "horizontalRule") {
            sb.append("\n---\n")
        }

        val content = node["content"]?.jsonArray
        if (content != null) {
            for (item in content) {
                if (item is JsonObject) {
                    val nextDepth = if (type == "bulletList" || type == "orderedList") depth + 1 else depth
                    sb.append(convertTipTapToMarkdown(item, lssResources, nextDepth))
                }
            }
        }

        return when (type) {
            "paragraph" -> sb.toString() + "\n"
            "listItem" -> {
                val indent = "  ".repeat((depth - 1).coerceAtLeast(0))
                "\n$indent• ${sb.toString().trim()}"
            }
            "bulletList", "orderedList" -> sb.toString() + "\n"
            else -> sb.toString()
        }
    }
}
