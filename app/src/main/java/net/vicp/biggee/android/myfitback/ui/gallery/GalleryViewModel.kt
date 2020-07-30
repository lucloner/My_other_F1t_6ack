package net.vicp.biggee.android.myfitback.ui.gallery

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import net.vicp.biggee.android.myfitback.ui.HeartRateChart

class GalleryViewModel : ViewModel() {
    val text = MutableLiveData<String>().apply {
        value = "This is gallery Fragment"
    }
    val data by lazy {
        MutableLiveData(
            HeartRateChart.current?.lineData ?: HeartRateChart().lineData
        )
    }
    val dataHeart by lazy {
        MutableLiveData(
            HeartRateChart.current?.lineData ?: HeartRateChart().lineData
        )
    }
    val dataLevel by lazy {
        MutableLiveData(
            HeartRateChart.current?.lineData ?: HeartRateChart().lineData
        )
    }
}