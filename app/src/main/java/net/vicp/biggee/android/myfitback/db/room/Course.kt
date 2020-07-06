package net.vicp.biggee.android.myfitback.db.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity
data class Course(
    @PrimaryKey val timeRange: ClosedRange<LocalDateTime>
) {
    var userId = ""
    var teacherId = ""
}