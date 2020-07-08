package net.vicp.biggee.android.myfitback.ui

import android.widget.Toast
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import net.vicp.biggee.android.myfitback.R
import net.vicp.biggee.android.myfitback.db.room.HeartRate

class HeartRateChart(chart: LineChart) : DrawLineChart(chart) {
    init {
        (chart.apply {
            animateXY(2000, 2000, Easing.EaseInCubic)
        }.data.apply {
            isHighlightEnabled = true
        }.dataSets.first() as LineDataSet).apply {
            setDrawFilled(true)
            fillDrawable = chart.context.getDrawable(R.drawable.fade_red)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawCircles(false)
        }
    }

    var startX = -1.0
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
        addEntry(x.toFloat(), hr.toFloat())
    }

    override fun onNothingSelected() {

    }

    override fun onValueSelected(e: Entry?, h: Highlight?) {
        Toast.makeText(lineChart.context, "时间(秒):${e?.x} 心率:${e?.y}", Toast.LENGTH_SHORT).show()
    }
}