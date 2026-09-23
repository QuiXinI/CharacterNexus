package ru.quasaris.characternexus.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        rule.collect(
            packageName = "ru.quasaris.characternexus",
            includeInStartupProfile = true
        ) {
            // Start the default activity
            pressHome()
            startActivityAndWait()

            // Wait for UI to load
            device.waitForIdle()

            // Perform scroll interaction if scrollable content exists
            val scrollable = device.findObject(By.scrollable(true))
            if (scrollable != null) {
                scrollable.fling(Direction.DOWN)
                device.waitForIdle()
                scrollable.fling(Direction.UP)
                device.waitForIdle()
            }
        }
    }
}
