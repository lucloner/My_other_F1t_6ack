package net.vicp.biggee.android.myfitback.ui

import android.app.Activity
import android.util.Log
import android.view.Gravity
import android.widget.AutoCompleteTextView
import android.widget.NumberPicker
import android.widget.SimpleAdapter
import android.widget.TextView
import com.google.android.flexbox.FlexboxLayout
import com.onecoder.devicelib.base.api.Manager
import com.onecoder.devicelib.base.control.entity.BleDevice
import com.onecoder.devicelib.base.entity.BaseDevice
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder
import com.pdog.dimension.dp
import net.sourceforge.pinyin4j.PinyinHelper
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType
import net.vicp.biggee.android.myfitback.Core
import net.vicp.biggee.android.myfitback.R
import net.vicp.biggee.android.myfitback.db.room.Course
import java.util.stream.Collectors
import kotlin.random.Random

object CourseDialogue : Core.DevBounded {

    lateinit var dialog: DialogPlus
    lateinit var activity: Activity
    lateinit var dialogSelected: TextView
    lateinit var fbMember: FlexboxLayout
    private val rnd by lazy { Random(hashCode()) }
    private val variousTextViews = HashSet<TextView>()
    lateinit var hour: NumberPicker
    lateinit var minute: NumberPicker
    lateinit var second: NumberPicker
    lateinit var courseType: AutoCompleteTextView
    val courseTypesAdapter by lazy {
        SimpleAdapter(
            activity,
            ArrayList<HashMap<String, String>>().apply {
                Course.baseTitle.forEach {
                    val py = PinyinHelper.toHanYuPinyinString(it, HanyuPinyinOutputFormat().apply {
                        caseType = HanyuPinyinCaseType.LOWERCASE
                        toneType = HanyuPinyinToneType.WITHOUT_TONE
                    }, "_", true).split("_").parallelStream().map { s -> s.first() }
                        .collect(Collectors.toList())
                    val map = HashMap<String, String>().apply {
                        put("brandName", it)
                        put("searchText", String(py.toCharArray()))
                    }
                    add(map)
                }
            },
            R.layout.main_item_three_line_row,
            arrayOf("searchText", "brandName"),
            intArrayOf(R.id.searchText, R.id.brandName)
        )
    }

    fun init(activity: Activity) {
        this.activity = activity
    }

    fun newDialogue() {
        variousTextViews.clear()
        dialog = DialogPlus.newDialog(activity)
            .setContentHolder(ViewHolder(R.layout.dialogue_course))
            .setGravity(Gravity.CENTER)
            .create().apply {
                dialogSelected = findViewById(R.id.d_c_tv_title2) as TextView
                fbMember = (findViewById(R.id.d_c_fb_member) as FlexboxLayout).apply {
                    Core.memberList.forEach {
                        addView(TextView(activity).apply {
                            variousTextViews.add(this)
                            text = it.name
                            setBackgroundColor(rnd.nextInt())
                            textSize = 5F.dp
                            setOnClickListener { _ ->
                                dialogSelected.text = it.name
                                Core.member = it
                            }
                        })
                    }
                }

                hour = (findViewById(R.id.hourpicker) as NumberPicker).apply {
                    minValue = 0
                    maxValue = 12
                }
                minute = (findViewById(R.id.minuteicker) as NumberPicker).apply {
                    minValue = 0
                    maxValue = 60
                }
                second = (findViewById(R.id.secondicker) as NumberPicker).apply {
                    minValue = 0
                    maxValue = 60
                }
                courseType =
                    (findViewById(R.id.d_c_tv_course) as AutoCompleteTextView).apply {
                        setAdapter(courseTypesAdapter)
                        setOnItemClickListener { _, view, _, _ ->
                            val brandName = view.findViewById<TextView>(R.id.brandName).text
                            setText(brandName)
                            setSelection(brandName.length)
                        }
                    }
                Log.d(this::class.simpleName, "启动开课对话框")
                show()
            }
    }

    override fun devBounded(
        device: BaseDevice,
        manager: Manager,
        deviceName: String?,
        status: Int
    ): Boolean {
        if (status >= BleDevice.STATE_CONNECTED) {
            newDialogue()
            return true
        }
        return false
    }


}