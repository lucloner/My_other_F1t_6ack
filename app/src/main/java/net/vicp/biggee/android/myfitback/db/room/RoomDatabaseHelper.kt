package net.vicp.biggee.android.myfitback.db.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(version = 1, entities = [HeartRate::class])
abstract class RoomDatabaseHelper : RoomDatabase() {
    abstract fun data(): SQLCommands

    companion object {
        private lateinit var INSTANCE: RoomDatabaseHelper

        @Synchronized
        fun getInstance(context: Context? = null): RoomDatabaseHelper {
            if (!this::INSTANCE.isLateinit) {
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