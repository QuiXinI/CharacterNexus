package ru.quasaris.characternexus.MainWindow

import ru.quasaris.characternexus.*
import ru.quasaris.characternexus.backend.ArchiveManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object CharacterDataHandler {

    fun createCharacter(
        id: Int,
        name: String,
        level: String,
        experience: String,
        strength: String,
        dexterity: String,
        constitution: String,
        intelligence: String,
        wisdom: String,
        charisma: String,
        strProf: Boolean,
        dexProf: Boolean,
        conProf: Boolean,
        intProf: Boolean,
        wisProf: Boolean,
        chaProf: Boolean,
        maxHp: String,
        currentHp: String,
        tempHp: String,
        armorClassEntries: List<ArmorClassEntry>,
        activeArmorClassId: String?,
        initiativeEntries: List<InitiativeEntry>,
        activeInitiativeId: String?,
        speedEntries: List<SpeedEntry>,
        activeSpeedId: String?,
        selectedConditions: List<String>,
        exhaustion: Int = 0,
        isShieldActive: Boolean = false,
        shieldEntries: List<ShieldEntry> = emptyList(),
        activeShieldId: String? = null,
        imageData: String? = null,
        skilledProficiencies: List<String> = emptyList(),
        skilledExpertise: List<String> = emptyList(),
        themeSeedColorArgb: Int? = null,
        hitDiceEntries: List<HitDiceEntry> = emptyList(),
        hitDiceMap: Map<Int, Int> = emptyMap(),
        defaultHitDie: Int = 8,
        hpLevelData: List<HPLevelEntry> = emptyList(),
        manualHPLevelData: List<HPLevelEntry> = emptyList(),
        isMulticlassHP: Boolean = false,
        isManualHP: Boolean = false,
        manualMaxHp: Int = 0,
        manualMaxHitDice: Int = 0,
        hpBonusesAtLevel: List<AttackBonus> = emptyList(),
        hpBonusesTotal: List<AttackBonus> = emptyList(),
        attacks: List<AttackEntry> = emptyList(),
        notes: List<DynamicNoteState> = listOf(DynamicNoteState()),
        skillsAndTraits: List<DynamicNoteState>? = null,
        inventory: List<DynamicNoteState>? = null,
        spells: List<DynamicNoteState>? = null,
        spellSettings: SpellSettings? = null,
        wallet: Wallet = Wallet(),
        bioShortFields: List<BioShortField>? = null,
        bioLongSections: List<DynamicNoteState>? = null
    ): Character {
        val baseChar = Character(
            id = id,
            name = name,
            characterClass = "", // Can be extended
            order = "Человек",   // Can be extended
            imageData = imageData,
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
            activeArmorClassId = activeArmorClassId,
            initiativeEntries = initiativeEntries,
            activeInitiativeId = activeInitiativeId,
            speedEntries = speedEntries,
            activeSpeedId = activeSpeedId,
            maxHp = maxHp,
            currentHp = currentHp,
            tempHp = tempHp,
            selectedConditions = selectedConditions,
            exhaustion = exhaustion,
            isShieldActive = isShieldActive,
            shieldEntries = shieldEntries,
            activeShieldId = activeShieldId,
            skilledProficiencies = skilledProficiencies,
            skilledExpertise = skilledExpertise,
            themeSeedColorArgb = themeSeedColorArgb,
            attacks = attacks,
            notes = notes,
            wallet = wallet,
            bioShortFields = bioShortFields ?: listOf(
                BioShortField(title = "Предыстория", widthRatio = 0.5f),
                BioShortField(title = "Мировоззрение", widthRatio = 0.5f),
                BioShortField(title = "Рост", widthRatio = 0.33f),
                BioShortField(title = "Вес", widthRatio = 0.33f),
                BioShortField(title = "Возраст", widthRatio = 0.33f),
                BioShortField(title = "Кожа", widthRatio = 0.33f),
                BioShortField(title = "Глаза", widthRatio = 0.33f),
                BioShortField(title = "Волосы", widthRatio = 0.33f)
            ),
            bioLongSections = bioLongSections ?: listOf(
                DynamicNoteState(title = "Предыстория персонажа"),
                DynamicNoteState(title = "Союзники и организации"),
                DynamicNoteState(title = "Враги и организации"),
                DynamicNoteState(title = "Черты характера"),
                DynamicNoteState(title = "Идеалы"),
                DynamicNoteState(title = "Привязанности"),
                DynamicNoteState(title = "Слабости")
            ),
            hitDiceEntries = hitDiceEntries,
            hitDiceMap = hitDiceMap,
            defaultHitDie = defaultHitDie,
            hpLevelData = hpLevelData,
            manualHPLevelData = manualHPLevelData,
            isMulticlassHP = isMulticlassHP,
            isManualHP = isManualHP,
            manualMaxHp = manualMaxHp,
            manualMaxHitDice = manualMaxHitDice,
            hpBonusesAtLevel = hpBonusesAtLevel,
            hpBonusesTotal = hpBonusesTotal
        )
        return baseChar.copy(
            skillsAndTraits = skillsAndTraits ?: baseChar.skillsAndTraits,
            inventory = inventory ?: baseChar.inventory,
            spells = spells ?: baseChar.spells,
            spellSettings = spellSettings ?: baseChar.spellSettings,
            bioShortFields = bioShortFields ?: baseChar.bioShortFields,
            bioLongSections = bioLongSections ?: baseChar.bioLongSections
        )
    }

    fun exportToLssKiller(path: String, character: Character, scope: CoroutineScope) {
        scope.launch {
            ArchiveManager.exportCharacter(character, path)
        }
    }
}
