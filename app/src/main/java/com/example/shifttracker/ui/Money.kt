package com.example.shifttracker.ui

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val RU = DecimalFormatSymbols(Locale("ru")).apply {
    decimalSeparator = '.'
    groupingSeparator = ' '
}
private val MONEY = DecimalFormat("#,##0.00", RU)

fun BigDecimal.pretty(): String = MONEY.format(this)