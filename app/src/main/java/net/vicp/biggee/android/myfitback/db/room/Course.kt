package net.vicp.biggee.android.myfitback.db.room

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.time.Duration
import java.time.LocalDateTime

@Entity
data class Course(
    @PrimaryKey val timeRange: ClosedRange<LocalDateTime>,
    val createTime: LocalDateTime = LocalDateTime.now()
) : ArrayList<HeartRate>() {
    var userId = ""
    var teacherId = ""
    var subject = ""

    @Ignore
    override var size: Int = 0
        @Ignore
        get() = super.size

    @Ignore
    @Transient
    var duration = Duration.between(timeRange.start, timeRange.endInclusive)

    constructor(startTime: LocalDateTime, duration: Duration) : this(
        startTime..startTime.plus(
            duration
        )
    )
}