package net.vicp.biggee.android.myfitback.db.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import java.time.LocalDateTime

@Dao
interface SQLCommands {
    @Insert
    fun logHeartRate(vararg heartRates: HeartRate)

    @Delete
    fun deleteHeartRate(heartRate: HeartRate)

    @Query("SELECT * FROM HeartRate")
    fun readAllHeartRate(): List<HeartRate>

    @Query("SELECT * FROM HeartRate WHERE utc>=:utcFrom AND utc<:utcTo")
    fun readHeartRate(utcFrom: Long, utcTo: Long = Long.MAX_VALUE): List<HeartRate>

    @Query("SELECT * FROM HeartRate WHERE createTime>=:timeFrom AND createTime<:timeTo")
    fun readHeartRate(
        timeFrom: LocalDateTime,
        timeTo: LocalDateTime = LocalDateTime.MAX
    ): List<HeartRate>

}