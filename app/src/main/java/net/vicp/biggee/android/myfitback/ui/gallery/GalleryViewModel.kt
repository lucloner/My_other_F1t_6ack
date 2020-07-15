package net.vicp.biggee.android.myfitback.ui.gallery

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.mikephil.charting.data.LineData

class GalleryViewModel : ViewModel() {
    val text = MutableLiveData<String>().apply {
        value = "This is gallery Fragment"
    }
    val data: MutableLiveData<out LineData> = MutableLiveData(LineData())
}