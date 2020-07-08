package net.vicp.biggee.android.myfitback.ui.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.github.mikephil.charting.charts.LineChart
import net.vicp.biggee.android.myfitback.Core
import net.vicp.biggee.android.myfitback.R
import net.vicp.biggee.android.myfitback.ui.HeartRateChart

class GalleryFragment : Fragment() {

    private lateinit var galleryViewModel: GalleryViewModel

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        galleryViewModel =
            ViewModelProviders.of(this).get(GalleryViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_gallery, container, false)
        val textView: TextView = root.findViewById(R.id.text_gallery)
        galleryViewModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })

        val lineChart = root.findViewById<LineChart>(R.id.f_h_chart).apply {
            val heartRateChart = HeartRateChart(this)
            setOnChartValueSelectedListener(heartRateChart)
            invalidate()
            Core.heartRateChart = heartRateChart
        }
        return root
    }
}