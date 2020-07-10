package net.vicp.biggee.android.myfitback.db.room

import androidx.room.Entity
import androidx.room.Ignore
import com.google.gson.JsonObject
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

    @Ignore
    var courses = ArrayList<Course>()
}