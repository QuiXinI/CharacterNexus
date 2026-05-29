package ru.quasaris.characters.master.MainWindow

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.google.gson.Gson
import ru.quasaris.characters.master.ArmorClassEntry
import ru.quasaris.characters.master.Character
import ru.quasaris.characters.master.InitiativeEntry
import ru.quasaris.characters.master.SpeedEntry

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
        imageData: String? = null,
        context: Context? = null,
        selectedImageUri: Uri? = null
    ): Character {
        val img = selectedImageUri?.let { u ->
            try {
                context?.contentResolver?.openInputStream(u)?.use { 
                    Base64.encodeToString(it.readBytes(), Base64.DEFAULT) 
                }
            } catch (e: Exception) { null }
        } ?: imageData

        return Character(
            id = id,
            name = name,
            characterClass = "", // Can be extended
            order = "Человек",   // Can be extended
            imageData = img,
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
            selectedConditions = selectedConditions
        )
    }

    fun exportToJson(context: Context, uri: Uri, character: Character) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { 
                it.write(gson.toJson(character).toByteArray()) 
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
