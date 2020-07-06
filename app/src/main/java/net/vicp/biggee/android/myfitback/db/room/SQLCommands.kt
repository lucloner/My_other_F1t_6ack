package net.vicp.biggee.android.myfitback.db.room

import androidx.room.*
import java.time.LocalDateTime

@Dao
interface SQLCommands {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun logHeartRate(vararg heartRates: HeartRate)

    @Delete
    fun deleteHeartRate(heartRate: HeartRate): Int

    @Query("SELECT * FROM HeartRate")
    fun readAllHeartRates(): List<HeartRate>

    @Query("SELECT * FROM HeartRate WHERE utc>=:utcFrom AND utc<:utcTo")
    fun readHeartRate(utcFrom: Long, utcTo: Long = Long.MAX_VALUE): List<HeartRate>

    @Query("SELECT * FROM HeartRate WHERE createTime>=:timeFrom AND createTime<:timeTo")
    fun readHeartRate(
        timeFrom: LocalDateTime,
        timeTo: LocalDateTime = LocalDateTime.MAX
    ): List<HeartRate>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addCourse(vararg courses: Course)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateCourse(course: Course): Int

    @Delete
    fun deleteCourse(course: Course): Int

    @Query("SELECT * FROM Course")
    fun readAllCourses(): List<Course>

    @Query("SELECT * FROM Course WHERE timeRange=:timeRange")
    fun readCourse(timeRange: ClosedRange<LocalDateTime>): List<Course>

    @Query("SELECT * FROM Course WHERE timeRange=:timeRange AND userId=:id OR teacherId=:id ORDER BY createTime DESC")
    fun readCourse(timeRange: ClosedRange<LocalDateTime>, id: String): List<Course>

    @Query("SELECT timeRange FROM Course GROUP BY timeRange ORDER BY createTime DESC")
    fun readCourseTable(): List<String>

    @Query("SELECT teacherId FROM Course GROUP BY teacherId ORDER BY createTime DESC")
    fun readCourseTeachers(): List<String>

    @Query("SELECT userId FROM Course GROUP BY userId ORDER BY createTime DESC")
    fun readCourseUsers(): List<String>

    @Query("SELECT * FROM Course WHERE timeRange+userId+teacherId like '%'+:string+'%'")
    fun readCourse(string: String): List<Course>
}