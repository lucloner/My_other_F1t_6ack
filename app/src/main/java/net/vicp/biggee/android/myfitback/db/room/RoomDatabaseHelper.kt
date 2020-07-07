package net.vicp.biggee.android.myfitback.db.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.vicp.biggee.android.myfitback.exe.Pool

@Database(version = 1, entities = [HeartRate::class, Course::class])
@TypeConverters(Converter::class)
abstract class RoomDatabaseHelper : RoomDatabase() {
    abstract fun data(): SQLCommands

    fun addHeartRate(heartRate: HeartRate): HeartRate {
        Pool.addJob(Runnable { data().logHeartRate(heartRate) })
        return heartRate
    }

    companion object {
        private lateinit var INSTANCE: RoomDatabaseHelper

        @Synchronized
        fun getInstance(context: Context? = null): RoomDatabaseHelper {
            if (!this::INSTANCE.isInitialized) {
                INSTANCE = Room.databaseBuilder(
                    context!!,
                    RoomDatabaseHelper::class.java,
                    this::class.java.simpleName + ".db"
                ).build()
            }
            return INSTANCE
        }
    }
}