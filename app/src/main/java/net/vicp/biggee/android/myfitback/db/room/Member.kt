package net.vicp.biggee.android.myfitback.db.room

import androidx.room.Entity
import androidx.room.Ignore
import com.google.gson.JsonObject
import com.onecoder.devicelib.base.entity.BaseUserInfo
import java.time.LocalDateTime

@Entity(primaryKeys = arrayOf("uid", "name", "role"))
data class Member(
    val uid: String = System.currentTimeMillis().toString().hashCode().toString(),
    val name: String = "",
    val role: String = "member",   //or "teacher" "admin" "etc"
    val createTime: LocalDateTime = LocalDateTime.now()
) {
    var userInfo: JsonObject = JsonObject()
    var details: String = ""
    var gender = "male"     //or "female"
    var weight = 125F       //kg
    var age = 40
    var height = 181

    @Ignore
    @Transient
    var baseUserInfo: BaseUserInfo? = null
        set(uInfo) {
            field = uInfo
            gender = if (field?.sex == BaseUserInfo.SEX_MAN) "male" else "female"
            age = field?.age ?: age
            weight = field?.weight ?: weight
            height = field?.height ?: height
        }

    @Ignore
    var courses = ArrayList<Course>()

    companion object {
        val sampleMale = Member(name = "测试男", role = "teacher")
        val sampleFemale = Member(name = "测试女", role = "member").apply {
            gender = "female"
            weight = 40F
            age = 25
            height = 160
        }
    }
}