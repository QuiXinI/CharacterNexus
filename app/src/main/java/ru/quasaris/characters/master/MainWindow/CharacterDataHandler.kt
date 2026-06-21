package ru.quasaris.characters.master.MainWindow

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.google.gson.Gson
import ru.quasaris.characters.master.ArmorClassEntry
import ru.quasaris.characters.master.Character
import ru.quasaris.characters.master.InitiativeEntry
import ru.quasaris.characters.master.SpeedEntry
import ru.quasaris.characters.master.ShieldEntry

import ru.quasaris.characters.master.ArchiveManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object CharacterDataHandler {
    private val gson = Gson()

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
        skilledExpertise: List<String> = emptyList()
    ): Character {
        return Character(
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
            skilledExpertise = skilledExpertise
        )
    }

    fun exportToLssKiller(context: Context, uri: Uri, character: Character, scope: CoroutineScope) {
        scope.launch {
            ArchiveManager.exportCharacter(context, character, uri)
        }
    }
}
