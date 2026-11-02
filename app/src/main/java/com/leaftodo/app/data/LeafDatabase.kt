package com.leaftodo.app.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class TaskStatus { OPEN, DONE, DROPPED }

@Entity(tableName = "tasks", indices = [Index(value = ["date"])])
data class Task(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "note") val note: String = "",
    @ColumnInfo(name = "date") val date: LocalDate? = null,
    @ColumnInfo(name = "time") val time: LocalTime? = null,
    @ColumnInfo(name = "status") val status: TaskStatus = TaskStatus.OPEN,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant,
    @ColumnInfo(name = "closed_at") val closedAt: Instant? = null,
)

class TaskConverters {
    @TypeConverter fun dateToString(value: LocalDate?): String? = value?.toString()
    @TypeConverter fun stringToDate(value: String?): LocalDate? = value?.let(LocalDate::parse)
    @TypeConverter fun timeToString(value: LocalTime?): String? = value?.format(DateTimeFormatter.ofPattern("HH:mm"))
    @TypeConverter fun stringToTime(value: String?): LocalTime? = value?.let(LocalTime::parse)
    @TypeConverter fun instantToLong(value: Instant?): Long? = value?.toEpochMilli()
    @TypeConverter fun longToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)
    @TypeConverter fun statusToString(value: TaskStatus): String = value.name
    @TypeConverter fun stringToStatus(value: String): TaskStatus = TaskStatus.valueOf(value)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE status = 'OPEN' AND date IS NOT NULL AND time IS NOT NULL")
    suspend fun getReminderCandidates(): List<Task>
    @Query("SELECT * FROM tasks ORDER BY id") suspend fun getAll(): List<Task>
    @Query("SELECT COUNT(*) FROM tasks") suspend fun count(): Int
    @Query("DELETE FROM tasks") suspend fun deleteAll()
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertAll(tasks: List<Task>)
    @Query("SELECT * FROM tasks WHERE date IS NULL") fun observeListTasks(): Flow<List<Task>>
    @Query("SELECT * FROM tasks WHERE date = :date") fun observeTasksOn(date: LocalDate): Flow<List<Task>>
    @Query("SELECT * FROM tasks WHERE status = 'OPEN' AND date BETWEEN :from AND :to")
    fun observeOpenBetween(from: LocalDate, to: LocalDate): Flow<List<Task>>
    @Query("SELECT * FROM tasks WHERE date BETWEEN :from AND :to")
    fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<Task>>
    @Delete suspend fun delete(task: Task)
    @Insert suspend fun insert(task: Task): Long
    @Update suspend fun update(task: Task)
    @Query("SELECT * FROM tasks WHERE id = :id") suspend fun getById(id: Long): Task?
}

@Database(entities = [Task::class], version = 1, exportSchema = true)
@TypeConverters(TaskConverters::class)
abstract class LeafDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    companion object {
        @Volatile private var instance: LeafDatabase? = null
        fun get(context: Context): LeafDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, LeafDatabase::class.java, "leaf.db")
                .build().also { instance = it }
        }
    }
}

suspend fun LeafDatabase.replaceAllTasks(tasks: List<Task>) {
    withTransaction {
        taskDao().deleteAll()
        taskDao().insertAll(tasks)
    }
}
