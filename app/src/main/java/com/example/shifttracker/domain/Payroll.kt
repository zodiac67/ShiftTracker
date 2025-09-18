package com.example.shifttracker.domain

import com.example.shifttracker.data.ShiftWithProject

data class Summary(val count: Int, val totalPay: Double)

fun calcPay(item: ShiftWithProject): Double {
    val s = item.shift
    val p = item.project
    return when {
        s.customPay > 0 -> s.customPay
        p.hourlyRate > 0 -> s.hours * p.hourlyRate
        p.fixedPerShift > 0 -> p.fixedPerShift
        else -> 0.0
    }
}

fun summarize(items: List<ShiftWithProject>): Summary {
    val total = items.sumOf { calcPay(it) }
    return Summary(items.size, total)
}
