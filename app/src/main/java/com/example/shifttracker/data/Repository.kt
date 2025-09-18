package com.example.shifttracker.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class Repo private constructor(context: Context) {
    private val db = Room.databaseBuilder(context, AppDb::class.java, "shifttracker.db").build()
    private val projects = db.projectDao()
    private val shifts = db.shiftDao()

    fun observeProjects(): Flow<List<ProjectEntity>> = projects.observeAll()
    fun observeShifts(from: LocalDate, to: LocalDate): Flow<List<ShiftWithProject>> = shifts.observeWithProjects(from, to)

    suspend fun addProject(name: String, hourly: Double, fixed: Double) {
        projects.insert(ProjectEntity(name = name, hourlyRate = hourly, fixedPerShift = fixed))
    }

    suspend fun addShift(date: LocalDate, projectId: Long, hours: Double, customPay: Double, note: String) {
        shifts.insert(ShiftEntity(projectId = projectId, date = date, hours = hours, customPay = customPay, note = note))
    }

    suspend fun deleteShift(s: ShiftEntity) = shifts.delete(s)

    companion object {
        @Volatile private var INSTANCE: Repo? = null
        fun get(context: Context): Repo = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Repo(context.applicationContext).also { INSTANCE = it }
        }
    }
}
