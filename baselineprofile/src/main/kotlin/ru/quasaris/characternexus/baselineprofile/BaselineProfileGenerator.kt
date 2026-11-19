package ru.quasaris.characternexus.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generateFromManualClicks() {
        rule.collect(
            packageName = "ru.quasaris.characternexus",
            includeInStartupProfile = true
        ) {
            // 1. Автоматический холодный запуск приложения
            pressHome()
            startActivityAndWait()

            // 2. Ручная запись
            val captureDurationMs = 300_000L
            val pollIntervalMs = 1_000L
            var elapsed = 0L

            while (elapsed < captureDurationMs) {
                Thread.sleep(pollIntervalMs)
                elapsed += pollIntervalMs
            }

            // 3. Автоматическое сохранение бейзлайн-профиля
            pressHome()
        }
    }
}
