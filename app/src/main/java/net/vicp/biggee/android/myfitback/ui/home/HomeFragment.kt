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
import androidx.lifecycle.ViewModelProvider
import com.google.android.flexbox.FlexboxLayout
import net.vicp.biggee.android.myfitback.Core
import net.vicp.biggee.android.myfitback.R
import kotlin.system.exitProcess

class HomeFragment : Fragment() {
    private lateinit var textView: TextView
    private lateinit var fbLayout: FlexboxLayout
    private lateinit var tbtn_mon: ToggleButton
    private var homeViewModel: HomeViewModel? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Core.apply {
            if (boundCoreService) {
                homeViewModel = coreService.CoreViewModel().getHomeViewMode()
            }
            if (homeViewModel == null) {
                homeViewModel =
                    ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
                        .create(HomeViewModel::class.java)
            }
        }
        Log.d(this::class.simpleName, "读取保存视图:${homeViewModel?.text?.value}")

        val root = inflater.inflate(R.layout.fragment_home, container, false)
        textView = root.findViewById<TextView>(R.id.text_home).apply {
            text = homeViewModel?.text?.value ?: text
        }

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

        tbtn_mon = root.findViewById<ToggleButton>(R.id.f_h_tbtn_mon).apply {
            setOnCheckedChangeListener { _, isChecked ->
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

        val btn_wx_login = root.findViewById<Button>(R.id.f_h_btn_wxTest).apply {
            setOnClickListener {
                Core.wxLogin()
            }
        }

        val btn_course = root.findViewById<Button>(R.id.f_h_btn_course).apply {
            setOnClickListener {
                Core.startCourse()
            }
        }

        fbLayout = root.findViewById(R.id.flexbox_layout)

        return root
    }

    override fun onPause() {
        save()
        super.onPause()
    }

    fun save() {
        homeViewModel?.text?.value = textView.text.toString()
        if (Core.boundCoreService) {
            Core.coreService.CoreViewModel().putViewMode(homeViewModel!!)
        }
        Log.d(this::class.simpleName, "save!!!${textView.text}")
    }
}