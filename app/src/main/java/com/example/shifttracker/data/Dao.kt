package com.example.shifttracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY name")
    fun observeAll(): Flow<List<ProjectEntity>>
    @Insert suspend fun insert(p: ProjectEntity): Long
}

@Dao
interface ShiftDao {
    @Query("SELECT * FROM shifts WHERE date BETWEEN :from AND :to ORDER BY date DESC")
    fun observeByRange(from: LocalDate, to: LocalDate): Flow<List<ShiftEntity>>
    @Transaction
    @Query("SELECT * FROM shifts WHERE date BETWEEN :from AND :to ORDER BY date DESC")
    fun observeWithProjects(from: LocalDate, to: LocalDate): Flow<List<ShiftWithProject>>
    @Insert suspend fun insert(s: ShiftEntity): Long
    @Delete suspend fun delete(s: ShiftEntity)
}

@Database(entities = [ProjectEntity::class, ShiftEntity::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDb : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun shiftDao(): ShiftDao
}
