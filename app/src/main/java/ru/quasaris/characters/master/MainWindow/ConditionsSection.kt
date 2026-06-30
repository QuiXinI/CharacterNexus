package ru.quasaris.characters.master.MainWindow

import android.content.Context
import androidx.compose.runtime.*
import ru.quasaris.characters.master.backend.Condition
import ru.quasaris.characters.master.backend.parseConditions

@Composable
fun rememberAllConditions(context: Context): List<Condition> {
    var allConditions by remember { mutableStateOf(emptyList<Condition>()) }
    LaunchedEffect(Unit) {
        try {
            context.assets.open("Conditions.md").bufferedReader().use {
                allConditions = parseConditions(it.readText())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return allConditions
}

fun toggleCondition(selectedConditions: List<String>, conditionName: String): List<String> {
    return if (selectedConditions.contains(conditionName)) {
        selectedConditions - conditionName
    } else {
        selectedConditions + conditionName
    }
}
