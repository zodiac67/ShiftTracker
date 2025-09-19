package com.example.shifttracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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

/** Пытаемся достать фикс-ставку у проекта, как бы она ни называлась. */
private fun ProjectEntity.fixedValueOrZero(): Double {
    val candidates = listOf(
        "fixed", "fixedPay", "fixed_rate", "fixedAmount", "fix", "fixedPrice"
    )
    for (name in candidates) {
        try {
            val f = this::class.java.getDeclaredField(name)
            f.isAccessible = true
            val v = f.get(this)
            if (v is Number) return v.toDouble()
        } catch (_: Throwable) {
            // пробуем следующее имя
        }
    }
    return 0.0
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddShiftDialog(
    projects: List<ProjectEntity>,
    onDismiss: () -> Unit,
    onSave: (date: LocalDate, projectId: Long, hours: Double, customPay: Double, note: String) -> Unit
) {
    val datePickerState =
        rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())

    var expanded by remember { mutableStateOf(false) }
    var selectedProject by remember { mutableStateOf(projects.firstOrNull()) }

    var payMode by remember { mutableStateOf(PayMode.HOURLY) }
    var hoursText by remember { mutableStateOf("") }        // ввод часов (для почасовой)
    var customPayText by remember { mutableStateOf("") }    // ручная сумма (необязательно)
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(),
        title = { Text("Новая смена") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Выберите дату", style = MaterialTheme.typography.titleMedium)

                DatePicker(
                    state = datePickerState,
                    modifier = Modifier.fillMaxWidth()
                )

                // Проект
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
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
                                onClick = { selectedProject = p; expanded = false }
                            )
                        }
                    }
                }

                // Режим оплаты под проектом
                Text("Оплата", style = MaterialTheme.typography.titleSmall)
                val modes = PayMode.values()
                val count = modes.size
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    modes.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = payMode == mode,
                            onClick = { payMode = mode },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = count),
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

                if (payMode == PayMode.HOURLY) {
                    OutlinedTextField(
                        value = hoursText,
                        onValueChange = { hoursText = it },
                        label = { Text("Часы") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

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
                val date = Instant.ofEpochMilli(millis)
                    .atZone(ZoneId.systemDefault()).toLocalDate()

                val pid = selectedProject?.id ?: 0L
                val hours = hoursText.replace(",", ".").toDoubleOrNull() ?: 0.0
                val manual = customPayText.replace(",", ".").toDoubleOrNull()

                val projectFixed = selectedProject?.fixedValueOrZero() ?: 0.0
                val customPay = when {
                    manual != null -> manual
                    else -> when (payMode) {
                        PayMode.HOURLY -> 0.0             // расчёт по часам сделает слой данных
                        PayMode.FIXED -> projectFixed
                        PayMode.HALF_FIXED -> projectFixed * 0.5
                    }
                }

                if (pid != 0L) onSave(date, pid, hours, customPay, note)
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
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