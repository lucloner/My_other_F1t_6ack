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

class HeartRateChart(chart: LineChart) : DrawLineChart(chart, title[0]) {
    var startX = -1.0

    init {
        chart.apply {
            animateXY(2000, 2000, Easing.EaseInCubic)
            data = data ?: LineData()
        }.data.apply {
            isHighlightEnabled = true
            if (dataSetCount >= 2) {
                startX = xMax.toDouble()
            }
            for (i in dataSetCount until 2) {
                addDataSet(LineDataSet(null, title[i]))
            }
        }.dataSets.apply {
            getDataSetHeartRate(get(0) as LineDataSet)
            getDataSetBurn(get(1) as LineDataSet)
        }
        Log.d(this::class.simpleName, "init!!! ${lineChart.data.dataSets}")
    }


    fun getDataSetHeartRate(lineDataSet: LineDataSet) = lineDataSet.apply {
        setDrawFilled(true)
        fillDrawable = lineChart.context.getDrawable(R.drawable.fade_red)
        mode = LineDataSet.Mode.CUBIC_BEZIER
        setDrawCircles(false)
    }

    fun getDataSetBurn(lineDataSet: LineDataSet) = lineDataSet.apply {
        setDrawFilled(true)
        fillDrawable = lineChart.context.getDrawable(R.drawable.fade_gold)
        mode = LineDataSet.Mode.STEPPED
        setDrawCircles(false)
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
        val yName = lineChart.data.getDataSetByIndex(h?.dataSetIndex ?: 0)
        Log.d(this::class.simpleName, "onValueSelected!!! $e $h")
        Toast.makeText(lineChart.context, "时间(秒):${e?.x} $yName:${e?.y}", Toast.LENGTH_SHORT).show()
    }

    companion object {
        val title = arrayOf("心率", "热量")
    }
}