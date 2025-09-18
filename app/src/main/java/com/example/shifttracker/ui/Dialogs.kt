package com.example.shifttracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.shifttracker.data.ProjectEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private enum class PayMode { FIXED, HALF_FIXED, HOURLY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddShiftDialog(
    projects: List<ProjectEntity>,
    onDismiss: () -> Unit,
    onSave: (date: LocalDate, projectId: Long, hours: Double, customPay: Double, note: String) -> Unit
) {
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    var expanded by remember { mutableStateOf(false) }
    var selectedProject by remember { mutableStateOf(projects.firstOrNull()) }

    // ввод
    var payMode by remember { mutableStateOf(PayMode.FIXED) }
    var hoursText by remember { mutableStateOf("") }
    var payText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var alsoNextDay by remember { mutableStateOf(false) }

    // когда выбираем проект или режим — автоподставляем суммы
    LaunchedEffect(selectedProject, payMode) {
        val fixed = selectedProject?.fixedPay ?: 0.0
        when (payMode) {
            PayMode.FIXED -> {
                if (fixed > 0) payText = fixed.clean()
                hoursText = ""
            }
            PayMode.HALF_FIXED -> {
                val half = if (fixed > 0) fixed / 2 else payText.toDoubleOrNull()?.div(2) ?: 0.0
                if (half > 0) payText = half.clean()
                hoursText = ""
            }
            PayMode.HOURLY -> {
                // почасовая — пользователь вводит часы; сумму можно оставить пустой (посчитается логикой репозитория, если есть ставка)
                payText = ""
            }
        }
    }

    // валидация
    val canSave by remember {
        derivedStateOf {
            selectedProject != null &&
            when (payMode) {
                PayMode.HOURLY -> hoursText.isNotBlank() // при почасовой нужны часы
                else -> payText.isNotBlank()            // при фикс/полуфикс нужна сумма
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая смена") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Календарь теперь внутри скролла — ничего не обрезается
                DatePicker(state = datePickerState)

                // Проект
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedProject?.name ?: "Выбери проект",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Проект") },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        projects.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.name) },
                                onClick = {
                                    selectedProject = p
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Режим оплаты (чипы)
                Text("Тип оплаты", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = payMode == PayMode.FIXED,
                        onClick = { payMode = PayMode.FIXED },
                        label = { Text("Фикс") }
                    )
                    FilterChip(
                        selected = payMode == PayMode.HALF_FIXED,
                        onClick = { payMode = PayMode.HALF_FIXED },
                        label = { Text("1/2 фикс") }
                    )
                    FilterChip(
                        selected = payMode == PayMode.HOURLY,
                        onClick = { payMode = PayMode.HOURLY },
                        label = { Text("Почасовая") }
                    )
                }

                // Часы (только для почасовой)
                OutlinedTextField(
                    value = hoursText,
                    onValueChange = { hoursText = it },
                    label = { Text("Часы") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = payMode == PayMode.HOURLY,
                    placeholder = {
                        val rate = selectedProject?.hourlyRate ?: 0.0
                        if (payMode == PayMode.HOURLY && rate > 0) Text("Ставка ${rate.clean()} ₽/час")
                    }
                )

                // Сумма вручную (для фикс/полуфикс — заполняется авто, но можно поправить)
                OutlinedTextField(
                    value = payText,
                    onValueChange = { payText = it },
                    label = { Text("Сумма (₽)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = payMode != PayMode.HOURLY
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Заметка") },
                    modifier = Modifier.fillMaxWidth()
                )

                Divider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Checkbox(
                        checked = alsoNextDay,
                        onCheckedChange = { alsoNextDay = it }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Добавить такую же смену на следующий день")
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    val millis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    val pid = selectedProject!!.id

                    // Подготовка значений
                    val hours = hoursText.replace(',', '.').toDoubleOrNull() ?: 0.0
                    val custom = payText.replace(',', '.').toDoubleOrNull() ?: run {
                        // если сумма пустая в почасовом — пусть будет 0, посчитается уровнем данных (если так задумано)
                        0.0
                    }

                    // Текущий день
                    onSave(date, pid, hours, custom, note)

                    // Опционально — следующий день с теми же параметрами
                    if (alsoNextDay) {
                        onSave(date.plusDays(1), pid, hours, custom, note)
                    }

                    onDismiss()
                }
            ) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
fun AddProjectDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, hourly: Double, fixed: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var hourlyText by remember { mutableStateOf("") }
    var fixedText by remember { mutableStateOf("") }

    val canSave by remember { derivedStateOf { name.isNotBlank() } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый проект") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = hourlyText,
                    onValueChange = { hourlyText = it },
                    label = { Text("Ставка в час (₽)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fixedText,
                    onValueChange = { fixedText = it },
                    label = { Text("Фикс за смену (₽)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Укажи ИЛИ почасовую ставку, ИЛИ фикс — достаточно одного поля.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    val hourly = hourlyText.replace(',', '.').toDoubleOrNull() ?: 0.0
                    val fixed = fixedText.replace(',', '.').toDoubleOrNull() ?: 0.0
                    onSave(name.trim(), hourly, fixed)
                    onDismiss()
                }
            ) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

/* ---------- helpers ---------- */

private fun Double.clean(): String {
    // без лишних .0
    val s = toString()
    return if (s.endsWith(".0")) s.dropLast(2) else s
}