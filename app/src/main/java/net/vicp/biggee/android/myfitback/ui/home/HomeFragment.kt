package net.vicp.biggee.android.myfitback.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import net.vicp.biggee.android.myfitback.R
import net.vicp.biggee.android.myfitback.dev.Core

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        homeViewModel =
                ViewModelProviders.of(this).get(HomeViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        val textView: TextView = root.findViewById(R.id.text_home)
        homeViewModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })
        val btn_t1:Button=root.findViewById(R.id.f_h_btn_t1)
        btn_t1.setOnClickListener{
            Core.t1(this.requireActivity())
        }
        requireActivity().startService(
            Intent(
                activity,
                Class.forName("com.onecoder.devicelib.base.control.manage.BluetoothLeService")
            )
        )
        return root
    }
}