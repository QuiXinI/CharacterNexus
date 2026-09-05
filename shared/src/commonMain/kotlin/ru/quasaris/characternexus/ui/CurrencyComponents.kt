package ru.quasaris.characternexus.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import ru.quasaris.characternexus.ui.theme.rememberEffectiveBlurRadius
import ru.quasaris.characternexus.ui.theme.hazePopover
import ru.quasaris.characternexus.backend.SettingsViewModel
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.backend.Currency
import ru.quasaris.characternexus.backend.CurrencyUtils
import kotlin.math.roundToInt

@Composable
fun CurrencyIcon(currency: Currency, modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Default.Toll,
        contentDescription = currency.displayName,
        modifier = modifier.rotate(45f),
        tint = currency.color
    )
}

@Composable
fun CurrencyDisplayRow(
    wallet: Wallet,
    onCurrencyClick: (Currency) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val visibleIds = wallet.visibleCurrencies
        val displayCurrencies = Currency.entries.filter { it.id in visibleIds }

        displayCurrencies.forEach { currency ->
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onCurrencyClick(currency) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CurrencyIcon(currency, Modifier.size(24.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = CurrencyUtils.formatCurrency(
                        CurrencyUtils.getWalletValue(wallet, currency),
                        currency == Currency.GOLD
                    ),
                    color = currency.color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyEditDialog(
    wallet: Wallet,
    initialCurrency: Currency,
    onWalletChange: (Wallet) -> Unit,
    onDismiss: () -> Unit,
    hazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    isDesktop: Boolean = false,
    settingsViewModel: SettingsViewModel? = null
) {
    var selectedCurrency by remember { mutableStateOf(initialCurrency) }
    var amountString by remember { mutableStateOf("") }
    var conversionToConfirm by remember { mutableStateOf<CurrencyUtils.ConversionResult?>(null) }
    var manualConversionToConfirm by remember { mutableStateOf<CurrencyUtils.ConversionResult?>(null) }

    if (isDesktop) {
        CurrencyEditContent(
            wallet = wallet,
            selectedCurrency = selectedCurrency,
            onSelectedCurrencyChange = { selectedCurrency = it },
            amountString = amountString,
            onAmountStringChange = { amountString = it },
            conversionToConfirm = conversionToConfirm,
            onConversionToConfirmChange = { conversionToConfirm = it },
            manualConversionToConfirm = manualConversionToConfirm,
            onManualConversionToConfirmChange = { manualConversionToConfirm = it },
            onWalletChange = onWalletChange,
            onDismiss = onDismiss,
            hazeState = hazeState,
            forceBlurEnabled = forceBlurEnabled,
            settingsViewModel = settingsViewModel
        )
    } else {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            DialogDimStyle(0f)
            CurrencyEditContent(
                wallet = wallet,
                selectedCurrency = selectedCurrency,
                onSelectedCurrencyChange = { selectedCurrency = it },
                amountString = amountString,
                onAmountStringChange = { amountString = it },
                conversionToConfirm = conversionToConfirm,
                onConversionToConfirmChange = { conversionToConfirm = it },
                manualConversionToConfirm = manualConversionToConfirm,
                onManualConversionToConfirmChange = { manualConversionToConfirm = it },
                onWalletChange = onWalletChange,
                onDismiss = onDismiss,
                hazeState = hazeState,
                forceBlurEnabled = forceBlurEnabled,
                settingsViewModel = settingsViewModel
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CurrencyEditContent(
    wallet: Wallet,
    selectedCurrency: Currency,
    onSelectedCurrencyChange: (Currency) -> Unit,
    amountString: String,
    onAmountStringChange: (String) -> Unit,
    conversionToConfirm: CurrencyUtils.ConversionResult?,
    onConversionToConfirmChange: (CurrencyUtils.ConversionResult?) -> Unit,
    manualConversionToConfirm: CurrencyUtils.ConversionResult?,
    onManualConversionToConfirmChange: (CurrencyUtils.ConversionResult?) -> Unit,
    onWalletChange: (Wallet) -> Unit,
    onDismiss: () -> Unit,
    hazeState: HazeState?,
    forceBlurEnabled: Boolean,
    settingsViewModel: SettingsViewModel? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val isOled = colorScheme.background == Color.Black
    val blurRadius = rememberEffectiveBlurRadius(settingsViewModel)
    val verticalScrollState = rememberScrollState()

    BackHandler(onBack = onDismiss)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Кошелек") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Закрыть")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = if (forceBlurEnabled && !isOled && hazeState != null) Color.Transparent else colorScheme.surface
                )
            )
        },
        containerColor = if (forceBlurEnabled && !isOled && hazeState != null) Color.Transparent else colorScheme.background,
        modifier = Modifier
            .fillMaxSize()
            .hazePopover(
                state = hazeState,
                blurRadius = blurRadius,
                forceBlurEnabled = forceBlurEnabled,
                isOled = isOled
            )
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(verticalScrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Full List
            Surface(
                color = if (hazeState != null && !isOled) Color.Black.copy(alpha = 0.3f) else colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Currency.entries.forEach { currency ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .combinedClickable(
                                    onClick = { onSelectedCurrencyChange(currency) },
                                    onLongClick = {
                                        if (amountString.isNotBlank() && currency != selectedCurrency) {
                                            val amount = CurrencyUtils.evaluateFormula(amountString)
                                            if (amount > 0) {
                                                onManualConversionToConfirmChange(
                                                    CurrencyUtils.calculateManualConversion(
                                                        selectedCurrency,
                                                        currency,
                                                        amount
                                                    )
                                                )
                                            }
                                        }
                                    }
                                )
                                .padding(vertical = 8.dp)
                        ) {
                            CurrencyIcon(currency, Modifier.size(20.dp))
                            Text(
                                text = CurrencyUtils.formatCurrency(CurrencyUtils.getWalletValue(wallet, currency), true),
                                color = currency.color,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Currency Selector
            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedCard(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, selectedCurrency.color.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CurrencyIcon(selectedCurrency, Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            selectedCurrency.displayName, 
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    Currency.entries.forEach { currency ->
                        DropdownMenuItem(
                            text = { Text(currency.displayName) },
                            leadingIcon = { CurrencyIcon(currency, Modifier.size(20.dp)) },
                            onClick = {
                                onSelectedCurrencyChange(currency)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Amount Field
            OutlinedTextField(
                value = amountString,
                onValueChange = onAmountStringChange,
                label = { Text("Сумма или формула") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(32.dp))

            // Action Buttons
            Surface(
                color = if (hazeState != null && !isOled) Color.Black.copy(alpha = 0.3f) else Color.Transparent,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val value = CurrencyUtils.evaluateFormula(amountString)
                            val current = CurrencyUtils.getWalletValue(wallet, selectedCurrency)
                            onWalletChange(CurrencyUtils.updateWallet(wallet, selectedCurrency, current + value))
                            onAmountStringChange("")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = selectedCurrency.color),
                        border = BorderStroke(2.dp, selectedCurrency.color),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("ВЗЯТЬ", fontWeight = FontWeight.Black, fontSize = 20.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val value = CurrencyUtils.evaluateFormula(amountString)
                            val current = CurrencyUtils.getWalletValue(wallet, selectedCurrency)
                            
                            if (current < value) {
                                val conversion = CurrencyUtils.calculateConversion(wallet, selectedCurrency, value)
                                if (conversion != null) {
                                    onConversionToConfirmChange(conversion)
                                }
                            } else {
                                onWalletChange(CurrencyUtils.updateWallet(wallet, selectedCurrency, current - value))
                                onAmountStringChange("")
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE57373)),
                        border = BorderStroke(2.dp, Color(0xFFE57373)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("ДАТЬ", fontWeight = FontWeight.Black, fontSize = 20.sp)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Detailed Balance List
            Text(
                "Точный баланс",
                style = MaterialTheme.typography.labelLarge,
                color = colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(Modifier.height(8.dp))
            
            Surface(
                color = if (hazeState != null && !isOled) Color.Black.copy(alpha = 0.3f) else colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Currency.entries.forEach { currency ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CurrencyIcon(currency, Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = currency.displayName,
                                color = currency.color,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(Modifier.width(16.dp))
                            
                            val horizontalScrollState = rememberScrollState()
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                    .drawWithContent {
                                        drawContent()
                                        val fadeWidth = 4.dp.toPx()
                                        if (horizontalScrollState.value > 0) {
                                            drawRect(
                                                brush = Brush.horizontalGradient(
                                                    0f to Color.Transparent,
                                                    fadeWidth to Color.Black,
                                                    startX = 0f,
                                                    endX = fadeWidth
                                                ),
                                                blendMode = BlendMode.DstIn
                                            )
                                        }
                                        if (horizontalScrollState.value < horizontalScrollState.maxValue) {
                                            drawRect(
                                                brush = Brush.horizontalGradient(
                                                    (size.width - fadeWidth) to Color.Black,
                                                    size.width to Color.Transparent,
                                                    startX = size.width - fadeWidth,
                                                    endX = size.width
                                                ),
                                                blendMode = BlendMode.DstIn
                                            )
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .horizontalScroll(horizontalScrollState)
                                        .align(Alignment.CenterEnd),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = CurrencyUtils.getWalletValue(wallet, currency).toLong().toString(),
                                        color = currency.color,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1
                                    )
                                    Spacer(Modifier.width(4.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Visibility Selector
            Text(
                "Отображение в инвентаре",
                style = MaterialTheme.typography.labelLarge,
                color = colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(Modifier.height(8.dp))
            
            Surface(
                color = if (hazeState != null && !isOled) Color.Black.copy(alpha = 0.3f) else colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Currency.entries.forEach { currency ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val currentVisible = wallet.visibleCurrencies.toMutableList()
                                    if (currentVisible.contains(currency.id)) {
                                        currentVisible.remove(currency.id)
                                    } else {
                                        currentVisible.add(currency.id)
                                    }
                                    onWalletChange(wallet.copy(visibleCurrencies = currentVisible))
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CurrencyIcon(currency, Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = currency.displayName,
                                color = currency.color,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Checkbox(
                                checked = wallet.visibleCurrencies.contains(currency.id),
                                onCheckedChange = null // Handled by row click
                            )
                        }
                    }
                }
            }
        }
    }

    conversionToConfirm?.let { conversion ->
        AutoConversionDialog(
            result = conversion,
            onConfirm = {
                val valueToDeduct = CurrencyUtils.evaluateFormula(amountString)
                var tempWallet = wallet
                
                // Deduct source
                val sourceBalance = CurrencyUtils.getWalletValue(tempWallet, conversion.sourceCurrency)
                tempWallet = CurrencyUtils.updateWallet(tempWallet, conversion.sourceCurrency, sourceBalance - conversion.sourceAmount)
                
                // Add converted amount to target
                val targetBalance = CurrencyUtils.getWalletValue(tempWallet, conversion.targetCurrency)
                tempWallet = CurrencyUtils.updateWallet(tempWallet, conversion.targetCurrency, targetBalance + conversion.targetAmount)
                
                // Deduct original required amount
                val finalTargetBalance = CurrencyUtils.getWalletValue(tempWallet, conversion.targetCurrency)
                tempWallet = CurrencyUtils.updateWallet(tempWallet, conversion.targetCurrency, finalTargetBalance - valueToDeduct)
                
                onWalletChange(tempWallet)
                onAmountStringChange("")
                onConversionToConfirmChange(null)
            },
            onDismiss = { onConversionToConfirmChange(null) }
        )
    }

    manualConversionToConfirm?.let { conversion ->
        ManualConversionDialog(
            result = conversion,
            onConfirm = {
                var tempWallet = wallet
                val sourceBalance = CurrencyUtils.getWalletValue(tempWallet, conversion.sourceCurrency)
                
                // We allow conversion even if balance is low, but character nexus usually checks
                // Let's check balance for safety
                if (sourceBalance >= conversion.sourceAmount) {
                    tempWallet = CurrencyUtils.updateWallet(tempWallet, conversion.sourceCurrency, sourceBalance - conversion.sourceAmount)
                    val targetBalance = CurrencyUtils.getWalletValue(tempWallet, conversion.targetCurrency)
                    tempWallet = CurrencyUtils.updateWallet(tempWallet, conversion.targetCurrency, targetBalance + conversion.targetAmount)
                    onWalletChange(tempWallet)
                    onAmountStringChange("")
                }
                onManualConversionToConfirmChange(null)
            },
            onDismiss = { onManualConversionToConfirmChange(null) },
            canConfirm = CurrencyUtils.getWalletValue(wallet, conversion.sourceCurrency) >= conversion.sourceAmount
        )
    }
}

@Composable
fun ManualConversionDialog(
    result: CurrencyUtils.ConversionResult,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    canConfirm: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Конвертация валюты") },
        text = {
            Column {
                Text("Вы хотите конвертировать:")
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CurrencyIcon(result.sourceCurrency, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("${result.sourceAmount.toLong()} ${result.sourceCurrency.displayName}", fontWeight = FontWeight.Bold)
                }
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CurrencyIcon(result.targetCurrency, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("${result.targetAmount.toLong()} ${result.targetCurrency.displayName}", fontWeight = FontWeight.Bold)
                }
                
                if (!canConfirm) {
                    Spacer(Modifier.height(16.dp))
                    Text("Недостаточно средств на балансе!", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = canConfirm) { Text("Да") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Нет") }
        }
    )
}

@Composable
fun AutoConversionDialog(
    result: CurrencyUtils.ConversionResult,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Недостаточно средств") },
        text = {
            Column {
                Text("У вас недостаточно выбранной валюты.")
                Spacer(Modifier.height(8.dp))
                Text("Конвертировать ${result.sourceAmount.toInt()} ${result.sourceCurrency.displayName} в ${result.targetAmount.toInt()} ${result.targetCurrency.displayName} и списать остаток?")
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Конвертировать") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
