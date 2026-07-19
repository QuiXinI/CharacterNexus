package ru.quasaris.characters.master.backend

import androidx.compose.ui.graphics.Color
import ru.quasaris.characters.master.Wallet
import java.util.Stack
import kotlin.math.floor
import kotlin.math.roundToInt

enum class Currency(
    val id: String,
    val displayName: String,
    val color: Color,
    val rateToCopper: Int,
    val order: Int
) {
    PLATINUM("platinum", "Платина", Color(0xFFADD8E6), 1000, 0),
    GOLD("gold", "Золото", Color(0xFFFFD700), 100, 1),
    ELECTRUM("electrum", "Электрум", Color(0xFFBF00FF), 50, 2),
    SILVER("silver", "Серебро", Color(0xFFC0C0C0), 10, 3),
    COPPER("copper", "Бронза", Color(0xFFCD7F32), 1, 4)
}

object CurrencyUtils {

    fun formatCurrency(value: Double, isGold: Boolean = false): String {
        val suffixes = listOf("", "k", "m", "b", "t")
        var num = value
        var suffixIndex = 0

        while (num >= 1000 && suffixIndex < suffixes.size - 1) {
            num /= 1000.0
            suffixIndex++
        }

        val suffix = suffixes[suffixIndex]
        val maxDigits = if (isGold) 7 else 3
        
        // If it's a whole number, don't show decimal
        return if (num % 1 == 0.0) {
            val s = num.toLong().toString()
            if (s.length > maxDigits) s.take(maxDigits) + suffix else s + suffix
        } else {
            // For 3.1 type limit (1 decimal)
            val formatted = "%.1f".format(num).replace(",", ".")
            val parts = formatted.split(".")
            val integerPart = parts[0]
            val decimalPart = parts[1]
            
            if (integerPart.length >= maxDigits) {
                integerPart.take(maxDigits) + suffix
            } else {
                formatted + suffix
            }
        }
    }

    fun evaluateFormula(formula: String): Double {
        var processed = formula.replace(",", ".").replace(" ", "")
        if (processed.isEmpty()) return 0.0
        
        try {
            val tokens = mutableListOf<String>()
            var i = 0
            while (i < processed.length) {
                val c = processed[i]
                if (c.isDigit() || c == '.') {
                    val start = i
                    while (i < processed.length && (processed[i].isDigit() || processed[i] == '.')) i++
                    tokens.add(processed.substring(start, i))
                } else if ("+*/()".contains(c)) {
                    tokens.add(c.toString())
                    i++
                } else if (c == '-') {
                    // Check if it's unary minus
                    if (i == 0 || "+*/(".contains(processed[i - 1])) {
                        val start = i
                        i++
                        while (i < processed.length && (processed[i].isDigit() || processed[i] == '.')) i++
                        tokens.add(processed.substring(start, i))
                    } else {
                        tokens.add("-")
                        i++
                    }
                } else i++
            }

            val values = Stack<Double>()
            val ops = Stack<String>()

            fun precedence(op: String): Int = when (op) {
                "+", "-" -> 1
                "*", "/" -> 2
                else -> 0
            }

            fun applyOp(b: Double, a: Double, op: String): Double = when (op) {
                "+" -> a + b
                "-" -> a - b
                "*" -> a * b
                "/" -> if (b != 0.0) a / b else 0.0
                else -> 0.0
            }

            for (token in tokens) {
                when {
                    token == "(" -> ops.push(token)
                    token == ")" -> {
                        while (ops.peek() != "(") values.push(applyOp(values.pop(), values.pop(), ops.pop()))
                        ops.pop()
                    }
                    "+-*/".contains(token) -> {
                        while (ops.isNotEmpty() && precedence(ops.peek()) >= precedence(token)) {
                            values.push(applyOp(values.pop(), values.pop(), ops.pop()))
                        }
                        ops.push(token)
                    }
                    else -> values.push(token.toDouble())
                }
            }

            while (ops.isNotEmpty()) values.push(applyOp(values.pop(), values.pop(), ops.pop()))
            return values.pop()
        } catch (e: Exception) {
            return 0.0
        }
    }

    data class ConversionResult(
        val sourceCurrency: Currency,
        val sourceAmount: Double,
        val targetCurrency: Currency,
        val targetAmount: Double
    )

    fun calculateConversion(wallet: Wallet, targetCurrency: Currency, requiredAmount: Double): ConversionResult? {
        val currentAmount = getWalletValue(wallet, targetCurrency)
        if (currentAmount >= requiredAmount) return null

        val deficit = requiredAmount - currentAmount
        val deficitInCopper = (deficit * targetCurrency.rateToCopper).roundToInt()

        // Find higher currencies to convert
        val higherCurrencies = Currency.entries
            .filter { it.rateToCopper > targetCurrency.rateToCopper }
            .sortedBy { it.rateToCopper } // Closest higher first

        for (source in higherCurrencies) {
            val sourceBalance = getWalletValue(wallet, source)
            if (sourceBalance <= 0) continue

            // How many units of source do we need?
            val sourceNeeded = Math.ceil(deficitInCopper.toDouble() / source.rateToCopper).toInt()
            
            if (sourceBalance >= sourceNeeded) {
                return ConversionResult(
                    sourceCurrency = source,
                    sourceAmount = sourceNeeded.toDouble(),
                    targetCurrency = targetCurrency,
                    targetAmount = (sourceNeeded * source.rateToCopper).toDouble() / targetCurrency.rateToCopper
                )
            }
        }
        
        return null // Not enough funds even after conversion
    }

    fun getWalletValue(wallet: Wallet, currency: Currency): Double = when (currency) {
        Currency.PLATINUM -> wallet.platinum
        Currency.GOLD -> wallet.gold
        Currency.ELECTRUM -> wallet.electrum
        Currency.SILVER -> wallet.silver
        Currency.COPPER -> wallet.copper
    }

    fun updateWallet(wallet: Wallet, currency: Currency, newAmount: Double): Wallet = when (currency) {
        Currency.PLATINUM -> wallet.copy(platinum = newAmount)
        Currency.GOLD -> wallet.copy(gold = newAmount)
        Currency.ELECTRUM -> wallet.copy(electrum = newAmount)
        Currency.SILVER -> wallet.copy(silver = newAmount)
        Currency.COPPER -> wallet.copy(copper = newAmount)
    }
}
