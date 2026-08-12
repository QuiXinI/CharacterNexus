package ru.quasaris.characters.master.MainWindow

import android.content.Context
import android.net.Uri
import ru.quasaris.characters.master.ArmorClassEntry
import ru.quasaris.characters.master.Character
import ru.quasaris.characters.master.InitiativeEntry
import ru.quasaris.characters.master.SpeedEntry
import ru.quasaris.characters.master.ShieldEntry
import ru.quasaris.characters.master.Wallet

import ru.quasaris.characters.master.backend.ArchiveManager
import ru.quasaris.characters.master.backend.GsonFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object CharacterDataHandler {
    private val gson = GsonFactory.create()

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
        hitDiceEntries: List<ru.quasaris.characters.master.HitDiceEntry> = emptyList(),
        hitDiceMap: Map<Int, Int> = emptyMap(),
        defaultHitDie: Int = 8,
        hpLevelData: List<ru.quasaris.characters.master.HPLevelEntry> = emptyList(),
        manualHPLevelData: List<ru.quasaris.characters.master.HPLevelEntry> = emptyList(),
        isMulticlassHP: Boolean = false,
        isManualHP: Boolean = false,
        manualMaxHp: Int = 0,
        manualMaxHitDice: Int = 0,
        hpBonusesAtLevel: List<ru.quasaris.characters.master.AttackBonus> = emptyList(),
        hpBonusesTotal: List<ru.quasaris.characters.master.AttackBonus> = emptyList(),
        attacks: List<ru.quasaris.characters.master.AttackEntry> = emptyList(),
        notes: List<ru.quasaris.characters.master.DynamicNoteState> = listOf(ru.quasaris.characters.master.DynamicNoteState()),
        skillsAndTraits: List<ru.quasaris.characters.master.DynamicNoteState>? = null,
        inventory: List<ru.quasaris.characters.master.DynamicNoteState>? = null,
        spells: List<ru.quasaris.characters.master.DynamicNoteState>? = null,
        spellSettings: ru.quasaris.characters.master.SpellSettings? = null,
        wallet: Wallet = Wallet(),
        bioShortFields: List<ru.quasaris.characters.master.BioShortField>? = null,
        bioLongSections: List<ru.quasaris.characters.master.DynamicNoteState>? = null
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
                ru.quasaris.characters.master.BioShortField(title = "Предыстория", widthRatio = 0.5f),
                ru.quasaris.characters.master.BioShortField(title = "Мировоззрение", widthRatio = 0.5f),
                ru.quasaris.characters.master.BioShortField(title = "Рост", widthRatio = 0.33f),
                ru.quasaris.characters.master.BioShortField(title = "Вес", widthRatio = 0.33f),
                ru.quasaris.characters.master.BioShortField(title = "Возраст", widthRatio = 0.33f),
                ru.quasaris.characters.master.BioShortField(title = "Кожа", widthRatio = 0.33f),
                ru.quasaris.characters.master.BioShortField(title = "Глаза", widthRatio = 0.33f),
                ru.quasaris.characters.master.BioShortField(title = "Волосы", widthRatio = 0.33f)
            ),
            bioLongSections = bioLongSections ?: listOf(
                ru.quasaris.characters.master.DynamicNoteState(title = "Предыстория персонажа"),
                ru.quasaris.characters.master.DynamicNoteState(title = "Союзники и организации"),
                ru.quasaris.characters.master.DynamicNoteState(title = "Враги и организации"),
                ru.quasaris.characters.master.DynamicNoteState(title = "Черты характера"),
                ru.quasaris.characters.master.DynamicNoteState(title = "Идеалы"),
                ru.quasaris.characters.master.DynamicNoteState(title = "Привязанности"),
                ru.quasaris.characters.master.DynamicNoteState(title = "Слабости")
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

    fun exportToLssKiller(context: Context, uri: Uri, character: Character, scope: CoroutineScope) {
        scope.launch {
            ArchiveManager.exportCharacter(context, character, uri)
        }
    }
}
