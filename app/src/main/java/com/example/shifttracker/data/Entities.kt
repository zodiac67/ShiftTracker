package com.example.shifttracker.data

import androidx.room.*
import java.time.LocalDate

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val hourlyRate: Double = 0.0,
    val fixedPerShift: Double = 0.0
)

@Entity(tableName = "shifts",
    foreignKeys = [ForeignKey(
        entity = ProjectEntity::class,
        parentColumns = ["id"],
        childColumns = ["projectId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("projectId")]
)
data class ShiftEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val date: LocalDate,
    val hours: Double = 0.0,
    val customPay: Double = 0.0,
    val note: String = ""
)

data class ShiftWithProject(
    @Embedded val shift: ShiftEntity,
    @Relation(parentColumn = "projectId", entityColumn = "id")
    val project: ProjectEntity
)

class Converters {
    @TypeConverter
    fun fromEpoch(value: Long?): java.time.LocalDate? = value?.let { java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate() }
    @TypeConverter
    fun toEpoch(value: java.time.LocalDate?): Long? = value?.atStartOfDay(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
}
