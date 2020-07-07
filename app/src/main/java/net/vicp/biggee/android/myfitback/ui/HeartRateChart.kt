package net.vicp.biggee.android.myfitback.ui

import com.github.mikephil.charting.charts.LineChart
import net.vicp.biggee.android.myfitback.db.room.HeartRate
import java.util.concurrent.atomic.DoubleAdder

class HeartRateChart(chart: LineChart) : DrawLineChart(chart) {
    val x = DoubleAdder()
    fun addHeartRate(heartRate: HeartRate) {
        val painted = lineChart.data.entryCount
        if (painted != 0) {
            x.add(heartRate.utc.toDouble() / 1000)
        }
        addEntry(x.sum().toFloat(), heartRate.heartRate.toFloat())
    }
}