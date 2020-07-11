package net.vicp.biggee.android.myfitback.db.room

import androidx.room.Entity
import androidx.room.Ignore
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
    @Ignore
    @Transient
    var rtHeartRate: RTHeartRate? = null
    var details = ""

    @Ignore
    var gender = "male"    //or "female"

    @Ignore
    var weight = 125F       //kg

    @Ignore
    var age = 40

    @Ignore
    var maxHeartRate: Int = 180
        get() {
            field = 220 - age
            return field
        }

    var burn: Double = 0.0
        get() {
            /*
            * maxhrtanaka = 208 - (0.7*age);
		    * lowercalculationlimit = Math.round(0.64*maxhrtanaka);
		    * uppercalculationlimit = 200;
            * caloricexpenditureperhour = Math.round(((-55.0969 + (0.6309*heartrate) + (0.1988*kilogramweight) + (0.2017*age))/4.184)*60);
            * caloricexpenditureperhour = Math.round(((-20.4022 + (0.4472*heartrate) - (0.1263*kilogramweight) + (0.074*age))/4.184)*60);
            * caloricexpenditure = Math.round(caloricexpenditureperhour*hours);
            * */
            field = if (gender == "male")
                ((-55.0969 + (0.6309 * heartRate) + (0.1988 * weight) + (0.2017 * age)) / 4.184) * 60
            else if (gender == "female")
                ((-20.4022 + (0.4472 * heartRate) + (0.1263 * weight) + (0.074 * age)) / 4.184) * 60
            else
                0.0 //TODO:人妖的公式还没有？？？
            return field
        }

    constructor(rtHeartRate: RTHeartRate) : this(
        rtHeartRate.utc,
        rtHeartRate.heartRate,
        rtHeartRate.mac
    ) {
        this.rtHeartRate = rtHeartRate
    }

    fun bind(member: Member) {
        member.also {
            gender = it.gender
            weight = it.weight
            age = it.age
        }
    }

    companion object {
        @JvmStatic
        val midHeartRate = 0.8
        val lowHeartRate = 0.6
    }

}