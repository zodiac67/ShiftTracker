package com.example.shifttracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.shifttracker.data.ProjectEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private enum class PayMode { HOURLY, FIXED, HALF_FIXED }

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

    var payMode by remember { mutableStateOf(PayMode.HOURLY) }
    var hoursText by remember { mutableStateOf("") }       // ввод часов (для почасовой)
    var customPayText by remember { mutableStateOf("") }   // ручная сумма (необязательно)
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(), // даст максимум ширины
        // Убираем платформенное ограничение ширины, чтобы календарь не «обрезался» на узких экранах
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text("Новая смена") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()), // чтобы всё всегда помещалось
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Выберите дату", style = MaterialTheme.typography.titleMedium)
                // Сам календарь растягиваем на всю ширину
                DatePicker(
                    state = datePickerState,
                    modifier = Modifier.fillMaxWidth()
                )

                // Выбор проекта
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                ) {
                    OutlinedTextField(
                        value = selectedProject?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Проект") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
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

                // Режим оплаты под проектом: Часы / Фикс / ½ фикс
                Text("Оплата", style = MaterialTheme.typography.titleSmall)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    PayMode.values().forEachIndexed { index, mode ->
                        val first = index == 0
                        val last = index == PayMode.values().lastIndex
                        SegmentedButton(
                            selected = payMode == mode,
                            onClick = { payMode = mode },
                            shape = SegmentedButtonDefaults.itemShape(first = first, last = last),
                            label = {
                                Text(
                                    when (mode) {
                                        PayMode.HOURLY -> "Часы"
                                        PayMode.FIXED -> "Фикс"
                                        PayMode.HALF_FIXED -> "½ фикс"
                                    }
                                )
                            }
                        )
                    }
                }

                // Поле «Часы» показываем только для почасовой
                if (payMode == PayMode.HOURLY) {
                    OutlinedTextField(
                        value = hoursText,
                        onValueChange = { hoursText = it },
                        label = { Text("Часы") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Необязательное поле ручной суммы (перебивает выбранный режим)
                OutlinedTextField(
                    value = customPayText,
                    onValueChange = { customPayText = it },
                    label = { Text("Сумма вручную (опционально)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Заметка") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val millis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                val pid = selectedProject?.id ?: 0L

                // Парсим ввод
                val hours = hoursText.replace(",", ".").toDoubleOrNull() ?: 0.0
                val manual = customPayText.replace(",", ".").toDoubleOrNull()

                // Если пользователь ввёл ручную сумму — она главнее
                val customPay = when {
                    manual != null -> manual
                    else -> when (payMode) {
                        PayMode.HOURLY -> 0.0 // расчёт по часам произойдёт по логике проекта
                        PayMode.FIXED -> selectedProject?.fixed ?: 0.0
                        PayMode.HALF_FIXED -> (selectedProject?.fixed ?: 0.0) * 0.5
                    }
                }

                if (pid != 0L) onSave(date, pid, hours, customPay, note)
            }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProjectDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, hourly: Double, fixed: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var hourlyText by remember { mutableStateOf("") }
    var fixedText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый проект") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = hourlyText,
                    onValueChange = { hourlyText = it },
                    label = { Text("Ставка в час") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fixedText,
                    onValueChange = { fixedText = it },
                    label = { Text("Фикс за смену") },
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
            TextButton(onClick = {
                val hourly = hourlyText.replace(",", ".").toDoubleOrNull() ?: 0.0
                val fixed = fixedText.replace(",", ".").toDoubleOrNull() ?: 0.0
                if (name.isNotBlank()) onSave(name.trim(), hourly, fixed)
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}