package ru.quasaris.characters.master.backend

import org.junit.Assert.assertEquals
import org.junit.Test

class StatCalculationsTest {

    @Test
    fun testBasicMath() {
        assertEquals(6, evaluateFormula("2 + 2 * 2"))
        assertEquals(8, evaluateFormula("(2 + 2) * 2"))
        assertEquals(2, evaluateFormula("10 / 5"))
        assertEquals(0, evaluateFormula("10 / 0")) // Safe division
    }

    @Test
    fun testPower() {
        assertEquals(8, evaluateFormula("2 ^ 3"))
        assertEquals(16, evaluateFormula("2 ^ 2 ^ 2"))
    }

    @Test
    fun testUnaryMinus() {
        assertEquals(-2, evaluateFormula("-5 + 3"))
        assertEquals(8, evaluateFormula("5 - -3"))
        assertEquals(-8, evaluateFormula("-(5 + 3)"))
    }

    @Test
    fun testFunctions() {
        assertEquals(5, evaluateFormula("min(10, 5)"))
        assertEquals(3, evaluateFormula("МАКС(1; 2; 3)"))
        assertEquals(2, evaluateFormula("floor(2.9)"))
        assertEquals(3, evaluateFormula("ВЕРХ(2.1)"))
    }

    @Test
    fun testNestedFunctions() {
        assertEquals(2, evaluateFormula("min(5, max(1, 2))"))
        assertEquals(10, evaluateFormula("max(min(10, 20), 5)"))
    }

    @Test
    fun testSpacesAndCase() {
        assertEquals(20, evaluateFormula(" mIn ( 100 ; 20 ) "))
        assertEquals(5, evaluateFormula("МАКС( 2, 5 )"))
    }

    @Test
    fun testStatLookup() {
        val stats = mapOf(
            "strength" to "18",
            "dexterity" to "14",
            "base_strength" to "16"
        )
        // STR modifier: (18-10)/2 = 4. base STR modifier: (16-10)/2 = 3.
        assertEquals(4, evaluateFormula("[STR]", stats))
        assertEquals(18, evaluateFormula("[STR ЗНАЧ]", stats))
        assertEquals(3, evaluateFormula("[НАСТ STR]", stats))
        assertEquals(16, evaluateFormula("[НАСТ STR ЗНАЧ]", stats))
        
        // Russian tokens
        assertEquals(2, evaluateFormula("[ЛОВ]", stats))
    }

    @Test
    fun testDiceToggle() {
        DiceCalculationConfig.isDiceCalculationEnabled = true
        // evaluateFormula doesn't roll, but it should return 0 for dice part instead of failing or concatenating
        assertEquals(10, evaluateFormula("10 + 1d4"))
        
        DiceCalculationConfig.isDiceCalculationEnabled = false
        assertEquals(10, evaluateFormula("10 + 1d4"))
    }
    
    @Test
    fun testComplexFormula() {
        val stats = mapOf("strength" to "16") // Mod +3
        // 3 + max(10, 2 * 3) = 3 + 10 = 13
        assertEquals(13, evaluateFormula("[STR] + max(10, 2 * 3)", stats))
    }
}
