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

    @Query("SELECT * FROM Course WHERE startTime=:startTime AND duration=:duration")
    fun readCourse(startTime: LocalDateTime, duration: Long = -1L): List<Course>

    @Query("SELECT * FROM Course WHERE createTime>:createTime ORDER BY createTime DESC LIMIT 1")
    fun readCourse(createTime: LocalDateTime): Course

    @Query("SELECT * FROM Course GROUP BY startTime,duration ORDER BY createTime DESC")
    fun readCourseTable(): List<Course>

    @Query("SELECT * FROM Course GROUP BY teacherId ORDER BY createTime DESC")
    fun readCourseTeachers(): List<Course>

    @Query("SELECT * FROM Course GROUP BY userId ORDER BY createTime DESC")
    fun readCourseUsers(): List<Course>

    @Query("SELECT * FROM Course WHERE userId=:memberId OR teacherId=:memberId ORDER BY createTime DESC")
    fun readCourse(memberId: String): List<Course>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addMember(vararg member: Member)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateMember(member: Member): Int

    @Delete
    fun deleteMember(member: Member): Int

    @Query("SELECT * FROM Member")
    fun readAllMember(): List<Member>

    @Query("SELECT * FROM Member WHERE uid=:uid ORDER BY createTime DESC LIMIT 1")
    fun readMember(uid: String): Member

    @Query("SELECT * FROM Member WHERE name=:name")
    fun readMemberByName(name: String): List<Member>

    @Query("SELECT * FROM Member WHERE role=:role")
    fun readMemberByRole(role: String): List<Member>
}