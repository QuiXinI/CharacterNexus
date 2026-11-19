package ru.quasaris.characternexus

import android.content.Context
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

/**
 * Вспомогательный класс для сохранения записанных действий кликов/скроллов
 * в текстовый файл с готовым кодом для BaselineProfileGenerator.kt.
 */
object EspressoToBaselineConverter {

    private val loggedActions = mutableListOf<String>()

    fun recordClickByText(text: String) {
        val snippet = """
            // Клик по тексту "$text"
            val textObj_$text = device.wait(Until.findObject(By.text("$text")), 3000)
            textObj_$text?.click()
            device.waitForIdle()
        """.trimIndent()
        loggedActions.add(snippet)
        exportToFile()
    }

    fun recordClickByRes(testTagOrResId: String) {
        val snippet = """
            // Клик по testTag / resourceId "$testTagOrResId"
            val resObj_$testTagOrResId = device.wait(Until.findObject(By.res("$testTagOrResId")), 3000)
            resObj_$testTagOrResId?.click()
            device.waitForIdle()
        """.trimIndent()
        loggedActions.add(snippet)
        exportToFile()
    }

    fun recordScrollDown() {
        val snippet = """
            // Прокрутка вниз
            val scrollable = device.findObject(By.scrollable(true))
            scrollable?.scroll(Direction.DOWN, 0.5f)
            device.waitForIdle()
        """.trimIndent()
        loggedActions.add(snippet)
        exportToFile()
    }

    fun exportToFile() {
        try {
            val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
            val targetFile = File(context.getExternalFilesDir(null), "recorded_baseline_profile.txt")

            val fileContent = buildString {
                appendLine("// ===========================================================")
                appendLine("// АВТОМАТИЧЕСКИ СГЕНЕРИРОВАННЫЙ СЦЕНАРИЙ ДЛЯ BaselineProfileGenerator.kt")
                appendLine("// Сгенерировано: ${java.util.Date()}")
                appendLine("// ===========================================================")
                appendLine()
                loggedActions.forEach { action ->
                    appendLine(action)
                    appendLine()
                }
            }

            targetFile.writeText(fileContent)
            Log.d("BaselineConverter", "✅ Сценарий сохранен в файл: ${targetFile.absolutePath}")
            Log.d("BaselineConverter", "Содержимое файла:\n$fileContent")
        } catch (e: Exception) {
            Log.e("BaselineConverter", "Ошибка при сохранении сценария в файл", e)
        }
    }
}
