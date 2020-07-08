package net.vicp.biggee.android.myfitback.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.ToggleButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import net.vicp.biggee.android.myfitback.Core
import net.vicp.biggee.android.myfitback.R
import kotlin.system.exitProcess

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var textView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel =
            ViewModelProviders.of(this).get(HomeViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        textView = root.findViewById(R.id.text_home)
        homeViewModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })

        val btn_t1 = root.findViewById<Button>(R.id.f_h_btn_t1).apply {
            setOnClickListener {
                textView.text = "主界面"
                Core.t1()
            }
        }
        val btn_connect = root.findViewById<Button>(R.id.f_h_btn_connect).apply {
            setOnClickListener {
                Core.connect()
            }
        }

        val tbtn_mon = root.findViewById<ToggleButton>(R.id.f_h_tbtn_mon).apply {
            setOnCheckedChangeListener { buttonView, isChecked ->
                Core.paint(isChecked)
            }
        }

        val btn_quit = root.findViewById<Button>(R.id.f_h_btn_quit).apply {
            setOnClickListener {
                Core.quit()
                Core.activity.stopService(Core.blServiceIntent)
                exitProcess(0)
            }
        }

        val btn_shot = root.findViewById<Button>(R.id.f_h_btn_shot).apply {
            setOnClickListener {
                Core.shot()
            }
        }

        return root
    }

    override fun onDetach() {
        Log.d(this::class.simpleName, "onDetach!!!")
        super.onDetach()
    }
}