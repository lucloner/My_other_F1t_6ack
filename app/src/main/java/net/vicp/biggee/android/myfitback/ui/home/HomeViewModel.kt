package net.vicp.biggee.android.myfitback.ui.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {
    val text: MutableLiveData<String> = MutableLiveData<String>().apply {
        value = "This is home Fragment"
    }
    val toggle = MutableLiveData<Boolean>(false)
}