package com.example.shifttracker.ui

import java.math.BigDecimal
import java.util.Calendar

data class Shift(
    val dateMillis: Long,
    val minutes: Int,
    val isFixed: Boolean,
    val fixedAmount: BigDecimal?,   // если фикс
    val hourlyRate: BigDecimal?     // если почасовая
)

fun Long.asDateString(): String {
    val cal = Calendar.getInstance().apply { timeInMillis = this@asDateString }
    val d = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
    val m = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
    val y = cal.get(Calendar.YEAR)
    return "$d.$m.$y"
}