package net.vicp.biggee.android.myfitback.db.room

import androidx.room.Entity
import androidx.room.Ignore
import java.time.Duration
import java.time.LocalDateTime

@Entity(primaryKeys = arrayOf("startTime", "duration"))
data class Course(
    val startTime: LocalDateTime,
    val createTime: LocalDateTime = LocalDateTime.now()
) : ArrayList<HeartRate>() {
    var userId = ""
    var teacherId = ""
    var subject = ""
    var details = ""

    @Ignore
    override var size: Int = 0
        @Ignore
        get() {
            field = super.size
            return field
        }

    var duration = -1L

    constructor(courseRange: ClosedRange<LocalDateTime>) : this(courseRange.start) {
        duration = Duration.between(courseRange.start, courseRange.endInclusive).toMillis()
    }

    fun setMember(member: Member? = null, teacher: Member? = null) {
        userId = member?.uid ?: ""
        teacherId = teacher?.uid ?: ""
        Course.member = member
        Course.teacher = teacher
    }

    companion object {
        var teacher: Member? = null
        var member: Member? = null
    }
}