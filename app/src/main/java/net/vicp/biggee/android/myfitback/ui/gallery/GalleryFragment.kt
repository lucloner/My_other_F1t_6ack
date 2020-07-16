package net.vicp.biggee.android.myfitback.ui.gallery

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import com.github.mikephil.charting.charts.LineChart
import net.vicp.biggee.android.myfitback.Core
import net.vicp.biggee.android.myfitback.CoreService
import net.vicp.biggee.android.myfitback.R
import net.vicp.biggee.android.myfitback.ui.HeartRateChart

class GalleryFragment : Fragment(), Core.SaveViewModel {

    private var galleryViewModel: GalleryViewModel? = null
    private lateinit var textView: TextView
    private lateinit var lineChart: LineChart

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_gallery, container, false)
        textView = root.findViewById(R.id.text_gallery)

        lineChart = root.findViewById(R.id.f_h_chart)
        load()
        return root
    }

    override fun onPause() {
        save()
        super.onPause()
        Log.d(this::class.simpleName, "onPause!!!")
    }

    override fun save() {
        Log.d(this::class.simpleName, "save!!!")
        CoreService.putViewMode(galleryViewModel ?: return)
    }

    override fun load() {
        Core.currentFragment = this

        galleryViewModel = CoreService.getGalleryViewMode()
        if (galleryViewModel == null) {
            galleryViewModel =
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
                    .create(GalleryViewModel::class.java)
        }

        Log.d(this::class.simpleName, "读取保存视图")
        galleryViewModel?.apply {
            text.observe(viewLifecycleOwner) {
                textView.text = it
            }
            data.observe(viewLifecycleOwner) {
                lineChart.data = it
            }
        }

        save()

        lineChart.apply {
            HeartRateChart.current?.also {
                it.setLineChart(this)
                setOnChartValueSelectedListener(it)
            }
            invalidate()
        }
    }
}