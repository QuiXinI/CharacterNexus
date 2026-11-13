package ru.quasaris.characternexus.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
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
            // 1. Запуск главного экрана и ожидание отрисовки (Cold Startup)
            pressHome()
            startActivityAndWait()
            device.waitForIdle()

            // 2. Скроллинг главного списка персонажей / меню
            val mainScrollable = device.findObject(By.scrollable(true))
            if (mainScrollable != null) {
                mainScrollable.fling(Direction.DOWN)
                device.waitForIdle()
                mainScrollable.fling(Direction.UP)
                device.waitForIdle()
            }

            // 3. Переход в карточку первого персонажа
            val characterCard = device.findObject(By.clickable(true))
            if (characterCard != null) {
                characterCard.click()
                device.waitForIdle()

                // 4. Прокрутка экрана персонажа для компиляции основных виджетов
                val detailScrollable = device.findObject(By.scrollable(true))
                if (detailScrollable != null) {
                    detailScrollable.scroll(Direction.DOWN, 0.8f)
                    device.waitForIdle()
                    detailScrollable.scroll(Direction.UP, 0.8f)
                    device.waitForIdle()
                }

                // 5. Переход по ключевым вкладкам (Атаки, Заклинания, Инвентарь)
                val tabsToVisit = listOf("Атаки", "Заклинания", "Инвентарь")
                for (tabName in tabsToVisit) {
                    val tabObject = device.findObject(By.text(tabName))
                    if (tabObject != null) {
                        tabObject.click()
                        device.waitForIdle()

                        // Прокрутка содержимого вкладки для прогрева LazyColumn/Grid компонентов
                        val tabScroll = device.findObject(By.scrollable(true))
                        if (tabScroll != null) {
                            tabScroll.scroll(Direction.DOWN, 0.5f)
                            device.waitForIdle()
                        }
                    }
                }
            }

            // 6. Возврат на домашний экран
            pressHome()
        }
    }
}
