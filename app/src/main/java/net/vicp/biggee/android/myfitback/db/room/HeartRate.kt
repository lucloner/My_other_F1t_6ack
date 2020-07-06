package net.vicp.biggee.android.myfitback.db.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.onecoder.devicelib.base.protocol.entity.RTHeartRate
import java.time.LocalDateTime

@Entity
data class HeartRate(
    @PrimaryKey(autoGenerate = true) val utc: Long = System.currentTimeMillis(),
    val heartRate: Int,
    val mac: String,
    val createTime: LocalDateTime = LocalDateTime.now()
) {
    constructor(rtHeartRate: RTHeartRate) : this(
        rtHeartRate.utc,
        rtHeartRate.heartRate,
        rtHeartRate.mac
    )
}