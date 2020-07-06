package net.vicp.biggee.android.myfitback.db.room

import androidx.room.TypeConverter
import com.google.gson.Gson
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.stream.Collectors

open class Converter {
    @TypeConverter
    fun localDateTimeToDB(time: LocalDateTime) = time.toInstant(ZoneOffset.UTC).toEpochMilli()

    @TypeConverter
    fun dbToLocalDateTime(epocMilli: Long) =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(epocMilli), ZoneId.systemDefault())

    @TypeConverter
    fun timeRangeToDB(timeRange: ClosedRange<LocalDateTime>) =
        "${timeRange.start.toInstant(ZoneOffset.UTC)
            .toEpochMilli()},${timeRange.endInclusive.toInstant(ZoneOffset.UTC).toEpochMilli()}"

    @TypeConverter
    fun dbToTimeRange(timeRange: String): ClosedRange<LocalDateTime> {
        val times = timeRange.split(",")
        return LocalDateTime.ofInstant(
            Instant.ofEpochMilli(times[0].toLong()),
            ZoneId.systemDefault()
        )..LocalDateTime.ofInstant(Instant.ofEpochMilli(times[1].toLong()), ZoneId.systemDefault())
    }

    @TypeConverter
    fun dbToListTimeRange(listTimeRange: List<String>): List<ClosedRange<LocalDateTime>> =
        ArrayList(listTimeRange).parallelStream().map(this::dbToTimeRange)
            .collect(Collectors.toList())

    @TypeConverter
    fun object2Gson(any: Any?) = Gson().toJson(any ?: "")

    @TypeConverter
    inline fun <reified T> gson2Object(gson: String) = Gson().fromJson<T>(gson, T::class.java)
}