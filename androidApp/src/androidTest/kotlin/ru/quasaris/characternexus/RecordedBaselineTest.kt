package ru.quasaris.characternexus

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class RecordedBaselineTest {

    @get:Rule
    @JvmField
    val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun recordedBaselineTest() {
        // Запись Espresso действий от Android Studio произойдет сюда.
        // Вы также можете вручную вызывать методы конвертера после записи:
        // EspressoToBaselineConverter.recordClickByText("Заклинания")
        // EspressoToBaselineConverter.exportToFile()
    }
}
