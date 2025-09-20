package com.example.shifttracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.math.BigDecimal
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun App() {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("ShiftTracker") })
        }
    ) { padding ->
        Box(Modifier.padding(padding)) { ShiftScreen() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftScreen() {
    var isFixed by remember { mutableStateOf(true) }
    var amountRaw by remember { mutableStateOf("") }  // сумма (точка/запятая)
    var minutes by remember { mutableStateOf(60) }
    var hourlyRaw by remember { mutableStateOf("") }

    val today = remember { System.currentTimeMillis() }
    var dateMillis by remember { mutableStateOf(today) }
    var showPicker by remember { mutableStateOf(false) }
    val dpState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)

    var shifts by remember { mutableStateOf(listOf<Shift>()) }

    if (showPicker) {
        FullWidthDatePickerDialog(
            state = dpState,
            onConfirm = {
                dateMillis = dpState.selectedDateMillis ?: dateMillis
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }

    Column(Modifier.padding(16.dp)) {
        OutlinedButton(onClick = { showPicker = true }) {
            Text("Дата: ${dateMillis.asDateString()}")
        }
        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Фикс")
            Switch(checked = isFixed, onCheckedChange = { isFixed = it })
            Spacer(Modifier.width(8.dp))
            Text(if (isFixed) "включён" else "выкл.")
        }

        Spacer(Modifier.height(8.dp))

        AmountField(label = "Сумма, ₽", value = amountRaw, onChange = { amountRaw = it })

        if (!isFixed) {
            Spacer(Modifier.height(8.dp))
            SliderField(
                label = "Длительность: ${(minutes / 60.0).format1()} ч",
                value = minutes / 60f,
                onValue = { minutes = (it * 60).roundToInt() }
            )
            Spacer(Modifier.height(8.dp))
            AmountField(label = "Ставка, ₽/ч", value = hourlyRaw, onChange = { hourlyRaw = it })
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                val amount = amountRaw.parseMoney()
                val hourly = hourlyRaw.parseMoney()
                val s = Shift(
                    dateMillis = dateMillis,
                    minutes = if (isFixed) 0 else minutes,
                    isFixed = isFixed,
                    fixedAmount = if (isFixed) amount else null,
                    hourlyRate = if (isFixed) null else hourly
                )
                shifts = listOf(s) + shifts
                if (isFixed) amountRaw = "" else { hourlyRaw = ""; amountRaw = "" }
            },
            enabled = (isFixed && amountRaw.isNotBlank()) ||
                      (!isFixed && hourlyRaw.isNotBlank())
        ) { Text("Сохранить смену") }

        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(shifts) { shift -> ShiftCard(shift) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullWidthDatePickerDialog(
    state: DatePickerState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false) // на всю ширину
    ) {
        Surface(Modifier.fillMaxWidth().padding(16.dp), shape = MaterialTheme.shapes.medium) {
            Column(Modifier.padding(8.dp)) {
                DatePicker(
                    state = state,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer(scaleX = 0.95f, scaleY = 0.95f) // немного «ужать» при крупном шрифте
                )
                Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Отмена") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onConfirm) { Text("Сохранить") }
                }
            }
        }
    }
}

@Composable
fun AmountField(label: String, value: String, onChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = { new ->
            val normalized = new.replace(',', '.')
            val filtered = buildString {
                var dot = false
                for (c in normalized) {
                    if (c.isDigit()) append(c)
                    else if (c == '.' && !dot) { append('.'); dot = true }
                }
            }
            onChange(filtered)
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun SliderField(label: String, value: Float, onValue: (Float) -> Unit) {
    Column {
        Text(label)
        Slider(
            value = value,
            onValueChange = onValue,
            valueRange = 0.5f..12f,
            steps = 23
        )
    }
}

@Composable
fun ShiftCard(shift: Shift) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(shift.dateMillis.asDateString(), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (shift.isFixed && shift.fixedAmount != null) {
                // ФИКС — без часов
                Text("Сумма: ${shift.fixedAmount.pretty()} ₽", style = MaterialTheme.typography.bodyLarge)
            } else {
                // Почасовая — часы и ставка
                val hours = (shift.minutes / 60.0).format1()
                val rate = (shift.hourlyRate ?: BigDecimal.ZERO).pretty()
                Text("$hours ч · $rate ₽/ч", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

/* утилиты */
private fun Double.format1(): String = String.format(Locale.US, "%.1f", this)
private fun String.parseMoney(): BigDecimal =
    this.replace(',', '.').toBigDecimalOrNull() ?: BigDecimal.ZERO