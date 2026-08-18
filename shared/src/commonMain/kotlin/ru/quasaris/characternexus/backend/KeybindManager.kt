package ru.quasaris.characternexus.backend

import androidx.compose.ui.input.key.Key
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.quasaris.characternexus.getAppDataDir
import ru.quasaris.characternexus.platformFileSystem
import ru.quasaris.characternexus.util.log

enum class KeybindAction(val displayName: String) {
    PREV_TAB("Предыдущая вкладка"),
    NEXT_TAB("Следующая вкладка"),
    TOGGLE_AC("КД (Класс Доспеха)"),
    TOGGLE_INIT("Инициатива"),
    TOGGLE_HEALTH("Хиты (Здоровье)"),
    TOGGLE_COND("Состояния"),
    TOGGLE_SPEED("Скорость"),
    TOGGLE_LEVEL("Опыт и Уровень"),
    TOGGLE_REST("Меню отдыха"),
    ADD_ITEM("Добавить элемент"),
    TOGGLE_EDIT_MODE("Режим редактирования / Расширенный режим"),
    TOGGLE_EXPANSION("Свернуть / Развернуть всё"),
    OPEN_DRAWER("Открыть боковое меню"),
    BACK("Назад / Закрыть окно")
}

@Serializable
data class KeybindConfig(
    val actionToKey: Map<KeybindAction, Long>
)

class KeybindManager {
    private val keybindsFile = getAppDataDir().div("keybinds.json")
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val defaultMappings = mapOf(
        KeybindAction.PREV_TAB to Key.DirectionLeft.keyCode,
        KeybindAction.NEXT_TAB to Key.DirectionRight.keyCode,
        KeybindAction.TOGGLE_AC to Key.A.keyCode,
        KeybindAction.TOGGLE_INIT to Key.I.keyCode,
        KeybindAction.TOGGLE_HEALTH to Key.H.keyCode,
        KeybindAction.TOGGLE_COND to Key.C.keyCode,
        KeybindAction.TOGGLE_SPEED to Key.S.keyCode,
        KeybindAction.TOGGLE_LEVEL to Key.E.keyCode,
        KeybindAction.TOGGLE_REST to Key.R.keyCode,
        KeybindAction.ADD_ITEM to Key.N.keyCode,
        KeybindAction.TOGGLE_EDIT_MODE to Key.Z.keyCode,
        KeybindAction.TOGGLE_EXPANSION to Key.X.keyCode,
        KeybindAction.OPEN_DRAWER to Key.Tab.keyCode,
        KeybindAction.BACK to Key.Escape.keyCode
    )

    var mappings: Map<KeybindAction, Key> = loadMappings()
        private set

    private fun loadMappings(): Map<KeybindAction, Key> {
        return try {
            if (platformFileSystem.exists(keybindsFile)) {
                val config = platformFileSystem.read(keybindsFile) {
                    json.decodeFromString<KeybindConfig>(readUtf8())
                }
                config.actionToKey.mapValues { Key(it.value) }
            } else {
                defaultMappings.mapValues { Key(it.value) }
            }
        } catch (e: Exception) {
            e.log()
            defaultMappings.mapValues { Key(it.value) }
        }
    }

    fun updateMapping(action: KeybindAction, key: Key) {
        val newMappings = mappings.toMutableMap()
        newMappings[action] = key
        mappings = newMappings
        save()
    }

    fun resetAction(action: KeybindAction) {
        val newMappings = mappings.toMutableMap()
        newMappings[action] = Key(defaultMappings[action] ?: return)
        mappings = newMappings
        save()
    }

    fun resetAll() {
        mappings = defaultMappings.mapValues { Key(it.value) }
        save()
    }

    private fun save() {
        try {
            val config = KeybindConfig(mappings.mapValues { it.value.keyCode })
            platformFileSystem.write(keybindsFile) {
                writeUtf8(json.encodeToString(config))
            }
        } catch (e: Exception) {
            e.log()
        }
    }

    fun getActionForKey(key: Key): KeybindAction? {
        return mappings.entries.find { it.value == key }?.key
    }
    
    fun getDefaultKey(action: KeybindAction): Key {
        return Key(defaultMappings[action] ?: 0L)
    }
}
