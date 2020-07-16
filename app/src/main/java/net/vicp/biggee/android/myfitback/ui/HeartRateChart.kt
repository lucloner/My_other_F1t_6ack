package net.vicp.biggee.android.myfitback.ui

import android.util.Log
import android.widget.Toast
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import net.vicp.biggee.android.myfitback.R
import net.vicp.biggee.android.myfitback.db.room.HeartRate

class HeartRateChart(lineData: LineData = LineData()) :
    DrawLineChart(lineData, title[0]) {
    var startX = -1.0
    var chart: LineChart? = null

    init {
        lineData.apply {
            isHighlightEnabled = true
            if (dataSetCount >= 2) {
                startX = xMax.toDouble()
            }
            for (i in dataSetCount until 2) {
                addDataSet(LineDataSet(null, title[i]))
            }
        }

        Log.d(this::class.simpleName, "init!!! ${lineData.dataSetCount}")
        current = this
    }

    fun getDataSetHeartRate(lineDataSet: LineDataSet) = lineDataSet.apply {
        setDrawFilled(true)
        fillDrawable = chart?.context?.getDrawable(R.drawable.fade_red)
        mode = LineDataSet.Mode.CUBIC_BEZIER
        setDrawCircles(false)
    }

    fun getDataSetBurn(lineDataSet: LineDataSet) = lineDataSet.apply {
        setDrawFilled(true)
        fillDrawable = chart?.context?.getDrawable(R.drawable.fade_gold)
        mode = LineDataSet.Mode.STEPPED
        setDrawCircles(false)
    }

    fun setLineChart(lineChart: LineChart) {
        chart = lineChart
        lineChart.apply {
            animateXY(2000, 2000, Easing.EaseInCubic)
            data = lineData.apply {
                dataSets.apply {
                    getDataSetHeartRate(get(0) as LineDataSet)
                    getDataSetBurn(get(1) as LineDataSet)
                }
            }
        }
    }

    fun addHeartRate(heartRate: HeartRate) {
        val utc = heartRate.utc.toDouble() / 1000
        val hr = heartRate.heartRate
        if (hr <= 0) {
            return
        }
        if (startX < 0) {
            startX = utc
        }
        val x = utc - startX
        addEntry(x.toFloat(), hr.toFloat(), 0)
        addEntry(x.toFloat(), heartRate.burn.toFloat(), 1)
        Log.d(this::class.simpleName, "addHeartRate!!! $x $hr ${heartRate.burn}")
    }

    override fun onNothingSelected() {
        Log.d(this::class.simpleName, "onNothingSelected!!!")
    }

    override fun onValueSelected(e: Entry?, h: Highlight?) {
        val yName = lineData.getDataSetByIndex(h?.dataSetIndex ?: 0)
        Log.d(this::class.simpleName, "onValueSelected!!! $e $h")
        Toast.makeText(
            chart?.context,
            "时间(秒):${e?.x ?: 0 / 1000} $yName:${e?.y?.toInt()}",
            Toast.LENGTH_SHORT
        ).show()
    }

    companion object {
        val title = arrayOf("心率", "热量")

        @Volatile
        var current: HeartRateChart? = null
            set(value) {
                val old = field
                field = value
                old?.chart?.data = LineData()
            }

        fun repaint() {
            repaint(current?.chart ?: return)
        }
    }
}