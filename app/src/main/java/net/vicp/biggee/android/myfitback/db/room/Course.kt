package net.vicp.biggee.android.myfitback.db.room

import androidx.room.Entity
import androidx.room.Ignore
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.CopyOnWriteArrayList

@Entity(primaryKeys = ["startTime", "duration"])
data class Course(
    val startTime: LocalDateTime,
    val createTime: LocalDateTime = LocalDateTime.now()
) : ArrayList<HeartRate>() {
    var userId = ""
    var teacherId = ""
    var subject = ""
    var details = ""
    var subList = CopyOnWriteArrayList<Course>()
    var heavy = 0
    var sumHR = 0
    var sumCal = 0

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
        val baseTitle = arrayOf(
            "波比跳",
            "高抬腿",
            "开合跳",
            "俯身登山",
            "腿弯举机坐姿腿弯举",
            "摸膝卷腹",
            "战绳",
            "椭圆机",
            "仰卧举腿",
            "俄罗斯转体",
            "螃蟹横爬",
            "折返跑",
            "静蹲",
            "俯地起身",
            "HIIT",
            "核心激活",
            "背部拉伸",
            "腿部拉伸",
            "炮筒前前后后",
            "仰卧卷腹"
        )
    }
}