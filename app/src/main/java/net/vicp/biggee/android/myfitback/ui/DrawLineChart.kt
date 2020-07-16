package net.vicp.biggee.android.myfitback.ui

import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.listener.OnChartValueSelectedListener

abstract class DrawLineChart(
    val lineData: LineData,
    val name: String = "DEFAULT${System.currentTimeMillis()}"
) : OnChartValueSelectedListener {

    init {
        lineData.apply {
            if (dataSetCount < 1) {
                addDataSet(LineDataSet(null, name))
            }
        }
    }

    fun addEntry(entry: Entry, dataSetIndex: Int = 0) = lineData.addEntry(entry, dataSetIndex)
    fun addEntry(x: Float, y: Float, dataSetIndex: Int = 0) = addEntry(Entry(x, y), dataSetIndex)


    companion object {
        val widthX = 60F
        fun repaint(lineChart: LineChart) {
            lineChart.apply {
                notifyDataSetChanged()
                setVisibleXRangeMaximum(widthX)
                moveViewTo(data.xMax - widthX, data.yMax, YAxis.AxisDependency.LEFT)
            }
        }
    }
}