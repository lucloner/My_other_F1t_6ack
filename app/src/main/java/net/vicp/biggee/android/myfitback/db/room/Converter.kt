package net.vicp.biggee.android.myfitback.db.room

import androidx.room.TypeConverter
import com.google.gson.Gson
import java.time.LocalDateTime

open class Converter {
    @TypeConverter
    fun localDateTimeToDB(time: LocalDateTime) = object2Gson(time)

    @TypeConverter
    fun dbToLocalDateTime(gson: String) = gson2Object<LocalDateTime>(gson)

    @TypeConverter
    fun timeRangeToDB(timeRange: ClosedRange<LocalDateTime>) = object2Gson(timeRange)

    @TypeConverter
    fun dbToTimeRange(gson: String) = gson2Object<ClosedRange<LocalDateTime>>(gson)

    @TypeConverter
    fun object2Gson(any: Any?) = Gson().toJson(any ?: "")

    @TypeConverter
    inline fun <reified T> gson2Object(gson: String) = Gson().fromJson<T>(gson, T::class.java)
}