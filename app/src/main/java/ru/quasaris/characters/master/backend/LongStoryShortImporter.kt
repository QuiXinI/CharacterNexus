package ru.quasaris.characters.master.backend

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import ru.quasaris.characters.master.*

object LongStoryShortImporter {

    fun isLongStoryShort(jsonElement: JsonElement): Boolean {
        return try {
            if (!jsonElement.isJsonObject) return false
            val obj = jsonElement.asJsonObject
            obj.has("jsonType") && obj.get("jsonType")?.asString == "character" && obj.has("data")
        } catch (e: Exception) {
            false
        }
    }

    private fun JsonElement?.safeString(default: String = ""): String {
        return when {
            this == null || isJsonNull -> default
            isJsonPrimitive -> asString
            else -> default
        }
    }

    fun parse(jsonElement: JsonElement): Character? {
        return try {
            val root = jsonElement.asJsonObject
            val dataElement = root.get("data") ?: return null
            val data = if (dataElement.isJsonObject) {
                dataElement.asJsonObject
            } else {
                JsonParser.parseString(dataElement.asString).asJsonObject
            }

            val name = data.getAsJsonObject("name")?.get("value").safeString("Новый персонаж")
            val info = data.getAsJsonObject("info") ?: JsonObject()
            val charClass = info.getAsJsonObject("charClass")?.get("value").safeString()
            val race = info.getAsJsonObject("race")?.get("value").safeString()
            val level = info.getAsJsonObject("level")?.get("value").safeString("1")
            val experience = info.getAsJsonObject("experience")?.get("value").safeString("0")

            val stats = data.getAsJsonObject("stats") ?: JsonObject()
            val strength = stats.getAsJsonObject("str")?.get("score").safeString("10")
            val dexterity = stats.getAsJsonObject("dex")?.get("score").safeString("10")
            val constitution = stats.getAsJsonObject("con")?.get("score").safeString("10")
            val intelligence = stats.getAsJsonObject("int")?.get("score").safeString("10")
            val wisdom = stats.getAsJsonObject("wis")?.get("score").safeString("10")
            val charisma = stats.getAsJsonObject("cha")?.get("score").safeString("10")

            val avatarObj = data.getAsJsonObject("avatar")
            val avatarUrl = avatarObj?.get("jpeg")?.asString ?: avatarObj?.get("webp")?.asString

            val saves = data.getAsJsonObject("saves") ?: JsonObject()
            val strProf = saves.getAsJsonObject("str")?.get("isProf")?.asBoolean ?: false
            val dexProf = saves.getAsJsonObject("dex")?.get("isProf")?.asBoolean ?: false
            val conProf = saves.getAsJsonObject("con")?.get("isProf")?.asBoolean ?: false
            val intProf = saves.getAsJsonObject("int")?.get("isProf")?.asBoolean ?: false
            val wisProf = saves.getAsJsonObject("wis")?.get("isProf")?.asBoolean ?: false
            val chaProf = saves.getAsJsonObject("cha")?.get("isProf")?.asBoolean ?: false

            val vitality = data.getAsJsonObject("vitality") ?: JsonObject()
            val maxHp = vitality.get("hp-max")?.getFieldValue().safeString("0")
            val currentHp = vitality.get("hp-current")?.getFieldValue().safeString("0")
            val tempHp = vitality.get("hp-temp")?.getFieldValue().safeString("0")
            val isShieldActive = vitality.get("shield")?.getFieldValue()?.asBoolean ?: false

            val acFormula = vitality.get("ac")?.getFieldValue().safeString("10 + [DEX]")
            val initFormula = vitality.get("initiative")?.getFieldValue().safeString("[DEX]")

            val armorClassEntries = mutableListOf(ArmorClassEntry(name = "Базовый КД", formula = acFormula))
            val initiativeEntries = mutableListOf(InitiativeEntry(name = "Базовая Инициатива", formula = initFormula))
            val speedEntries = mutableListOf(SpeedEntry(name = "Базовая Скорость", formula = vitality.get("speed")?.getFieldValue().safeString("30")))

            val coins = data.getAsJsonObject("coins") ?: JsonObject()
            val wallet = Wallet(
                gold = coins.getAsJsonObject("gp")?.get("value")?.asDouble ?: 0.0,
                silver = coins.getAsJsonObject("sp")?.get("value")?.asDouble ?: 0.0,
                copper = coins.getAsJsonObject("cp")?.get("value")?.asDouble ?: 0.0,
                platinum = coins.getAsJsonObject("pp")?.get("value")?.asDouble ?: 0.0,
                electrum = coins.getAsJsonObject("ep")?.get("value")?.asDouble ?: 0.0
            )

            // Skills
            val skills = data.getAsJsonObject("skills") ?: JsonObject()
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
                val skillObj = skills.getAsJsonObject(lssName)
                if (skillObj != null) {
                    val isProf = skillObj.get("isProf")?.let {
                        if (it.isJsonPrimitive) {
                            if (it.asJsonPrimitive.isBoolean) if (it.asBoolean) 1 else 0
                            else it.asInt
                        } else 0
                    } ?: 0
                    if (isProf >= 1) skilledProficiencies.add(mpName)
                    if (isProf == 2) {
                        skilledExpertise.add(mpName)
                    }
                }
            }

            // Bonuses
            val allBonuses = data.getAsJsonArray("bonuses") ?: JsonArray()
            val statBonuses = mutableListOf<StatBonus>()
            val skillBonuses = mutableListOf<SkillBonus>()
            
            val acBonusExprs = mutableListOf<String>()
            val initBonusExprs = mutableListOf<String>()
            val shieldBonusExprs = mutableListOf<String>()
            var shieldLabel = "Базовый Щит"
            
            val speedBonusExprs = mutableListOf<String>()

            for (b in allBonuses) {
                if (!b.isJsonObject) continue
                val bonus = b.asJsonObject
                val target = bonus.get("target").safeString()
                val expr = bonus.get("expr").safeString()
                
                val label = bonus.get("label").safeString().takeIf { it.isNotBlank() } ?: when {
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
                        val typeName = if (parts.getOrNull(2) == "save") "Спасбросок" else "Проверка"
                        "$typeName ($statName)".trim()
                    }
                    else -> "Бонус"
                }

                when {
                    target == "ac" -> {
                        if (label.contains("щит", ignoreCase = true) || label.contains("shield", ignoreCase = true)) {
                            shieldBonusExprs.add(expr)
                            shieldLabel = label
                        } else {
                            acBonusExprs.add(expr)
                        }
                    }
                    target == "initiative" -> initBonusExprs.add(expr)
                    target.startsWith("speed") -> speedBonusExprs.add(expr)
                    target == "skill-all" -> {
                        skillMap.values.forEach { mpSkillName ->
                            skillBonuses.add(SkillBonus(name = label, formula = expr, skillName = mpSkillName))
                        }
                    }
                    target.startsWith("skill.") -> {
                        val skillKey = target.removePrefix("skill.")
                        val mpSkillName = skillMap[skillKey] ?: skillKey
                        skillBonuses.add(SkillBonus(name = label, formula = expr, skillName = mpSkillName))
                    }
                    target == "stat-all.save" -> {
                        Attribute.values().filter { it != Attribute.NONE }.forEach { attr ->
                            statBonuses.add(StatBonus(name = label, formula = expr, attribute = attr, type = StatBonusType.SAVING_THROW))
                        }
                    }
                    target == "stat-all.check" -> {
                        Attribute.values().filter { it != Attribute.NONE }.forEach { attr ->
                            statBonuses.add(StatBonus(name = label, formula = expr, attribute = attr, type = StatBonusType.ABILITY_CHECK))
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
                            val bonusType = if (typeKey == "save") StatBonusType.SAVING_THROW else StatBonusType.ABILITY_CHECK
                            statBonuses.add(StatBonus(name = label, formula = expr, attribute = attribute, type = bonusType))
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
            val weaponsList = data.getAsJsonArray("weaponsList") ?: JsonArray()
            val mpAttacks = mutableListOf<AttackEntry>()
            
            for (item in weaponsList) {
                if (!item.isJsonObject) continue
                val weapon = item.asJsonObject
                val weaponId = weapon.get("id").safeString()
                
                val abilityStr = weapon.get("ability").safeString("str").lowercase()
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
                    val bonus = b.asJsonObject
                    val target = bonus.get("target").safeString()
                    val expr = bonus.get("expr").safeString()
                    
                    val label = bonus.get("label").safeString().takeIf { it.isNotBlank() } ?: when {
                        target.contains(".attack") -> "Бонус к попаданию"
                        target.contains(".damage") -> "Бонус к урону"
                        else -> "Бонус"
                    }
                    
                    if (target == "weapon.$weaponId.attack" || target == "weapon-all.attack") {
                        attackBonuses.add(AttackBonus(name = label, formula = expr))
                    }
                    if (target == "weapon.$weaponId.damage" || target == "weapon-all.damage") {
                        damageBonuses.add(DamageBonus(name = label, formula = expr))
                    }
                }

                mpAttacks.add(
                    AttackEntry(
                        name = weapon.getAsJsonObject("name")?.get("value").safeString("Оружие"),
                        damageFormula = weapon.getAsJsonObject("dmg")?.get("value").safeString(),
                        damageType = weapon.getAsJsonObject("dmgType")?.get("value").safeString(),
                        isProficient = weapon.get("isProf")?.asBoolean ?: true,
                        attribute = attribute,
                        attackBonuses = attackBonuses,
                        damageBonuses = damageBonuses
                    )
                )
            }

            // Notes and Text fields
            val text = data.getAsJsonObject("text") ?: JsonObject()
            val lssResources = data.getAsJsonObject("resources") ?: JsonObject()
            
            fun extractText(key: String): String {
                val field = text.getAsJsonObject(key) ?: return ""
                val value = field.get("value") ?: return ""
                val contentData = if (value.isJsonObject) {
                    value.asJsonObject.get("data")?.takeIf { it.isJsonObject }?.asJsonObject
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

            val subInfo = data.getAsJsonObject("subInfo") ?: JsonObject()
            val bioShortFields = listOf(
                BioShortField(title = "Предыстория", value = info.getAsJsonObject("background")?.get("value").safeString(), widthRatio = 0.5f),
                BioShortField(title = "Мировоззрение", value = info.getAsJsonObject("alignment")?.get("value").safeString(), widthRatio = 0.5f),
                BioShortField(title = "Рост", value = subInfo.getAsJsonObject("height")?.get("value").safeString(), widthRatio = 0.33f),
                BioShortField(title = "Вес", value = subInfo.getAsJsonObject("weight")?.get("value").safeString(), widthRatio = 0.33f),
                BioShortField(title = "Возраст", value = subInfo.getAsJsonObject("age")?.get("value").safeString(), widthRatio = 0.33f),
                BioShortField(title = "Кожа", value = subInfo.getAsJsonObject("skin")?.get("value").safeString(), widthRatio = 0.33f),
                BioShortField(title = "Глаза", value = subInfo.getAsJsonObject("eyes")?.get("value").safeString(), widthRatio = 0.33f),
                BioShortField(title = "Волосы", value = subInfo.getAsJsonObject("hair")?.get("value").safeString(), widthRatio = 0.33f)
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
                val noteObj = text.getAsJsonObject(noteKey)
                if (noteObj != null) {
                    val label = noteObj.get("customLabel").safeString("Заметка $i")
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

    private fun JsonElement.getFieldValue(): JsonElement? {
        return if (this.isJsonObject) this.asJsonObject.get("value") else this
    }

    private fun convertTipTapToMarkdown(node: JsonObject, lssResources: JsonObject, depth: Int = 0): String {
        val sb = StringBuilder()
        val type = node.get("type")?.asString

        var prefix = ""
        var suffix = ""

        val marks = node.getAsJsonArray("marks")
        if (marks != null) {
            // Sort marks so that "link" is processed first to be innermost: **[text](url)**
            val sortedMarks = marks.map { it.asJsonObject }.sortedByDescending {
                if (it.get("type")?.asString == "link") 1 else 0
            }
            for (mark in sortedMarks) {
                when (mark.get("type")?.asString) {
                    "bold" -> { prefix = "**$prefix"; suffix = "${suffix}**" }
                    "italic" -> { prefix = "_$prefix"; suffix = "${suffix}_" }
                    "strike" -> { prefix = "~~$prefix"; suffix = "${suffix}~~" }
                    "link" -> {
                        val href = mark.getAsJsonObject("attrs")?.get("href").safeString()
                        prefix = "[$prefix"
                        suffix = "${suffix}]($href)"
                    }
                }
            }
        }

        if (type == "text") {
            sb.append(prefix).append(node.get("text")?.asString ?: "").append(suffix)
        } else if (type == "resource") {
            val resId = node.getAsJsonObject("attrs")?.get("id").safeString()
            val lssRes = lssResources.getAsJsonObject(resId)
            if (lssRes != null) {
                val name = lssRes.get("name").safeString("Ресурс")
                val cur = lssRes.get("current").safeString("0")
                val max = lssRes.get("maxExpr").safeString(lssRes.get("resolvedMax").safeString("0"))
                val notes = lssRes.get("notes").safeString()
                val isLongRest = lssRes.get("isLongRest")?.asBoolean ?: false
                
                val lrParam = if (isLongRest) " | lr=all" else ""
                val notesParam = if (notes.isNotEmpty()) " | notes=$notes" else ""
                
                sb.append("{Ресурс: $name | cur=$cur | max=$max$lrParam$notesParam}")
            }
        } else if (type == "horizontalRule") {
            sb.append("\n---\n")
        }

        val content = node.getAsJsonArray("content")
        if (content != null) {
            for (item in content) {
                if (item.isJsonObject) {
                    val nextDepth = if (type == "bulletList" || type == "orderedList") depth + 1 else depth
                    sb.append(convertTipTapToMarkdown(item.asJsonObject, lssResources, nextDepth))
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
