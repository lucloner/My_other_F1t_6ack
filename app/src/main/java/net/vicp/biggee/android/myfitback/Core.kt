package net.vicp.biggee.android.myfitback

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Handler
import android.view.Gravity
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import com.onecoder.devicelib.FitBleKit
import com.onecoder.devicelib.armband.api.ArmBandManager
import com.onecoder.devicelib.armband.api.entity.StepFrequencyEntity
import com.onecoder.devicelib.armband.api.interfaces.RealTimeDataListener
import com.onecoder.devicelib.base.api.Manager
import com.onecoder.devicelib.base.control.entity.BleDevice
import com.onecoder.devicelib.base.control.entity.BluetoothBean
import com.onecoder.devicelib.base.control.interfaces.BleScanCallBack
import com.onecoder.devicelib.base.control.interfaces.CheckSystemBleCallback
import com.onecoder.devicelib.base.control.interfaces.DeviceStateChangeCallback
import com.onecoder.devicelib.base.control.manage.BleScanner
import com.onecoder.devicelib.base.control.manage.BluetoothLeService
import com.onecoder.devicelib.base.entity.BaseDevice
import com.onecoder.devicelib.base.entity.DeviceType
import com.onecoder.devicelib.base.protocol.entity.RTHeartRate
import com.onecoder.devicelib.heartrate.api.HeartRateMonitorManager
import com.onecoder.devicelib.heartrate.api.interfaces.HeartRateListener
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder
import com.tapadoo.alerter.Alerter
import net.vicp.biggee.android.myfitback.db.room.HeartRate
import net.vicp.biggee.android.myfitback.db.room.RoomDatabaseHelper
import net.vicp.biggee.android.myfitback.exe.Pool
import net.vicp.biggee.android.myfitback.ui.HeartRateChart
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentLinkedQueue

object Core : BleScanCallBack, RealTimeDataListener, CheckSystemBleCallback,
    DeviceStateChangeCallback, HeartRateListener, Callable<Any> {
    lateinit var activity: Activity
    lateinit var sdk: FitBleKit
    lateinit var blService: BluetoothLeService
    lateinit var baseDevice: BaseDevice
    var heartRateChart: HeartRateChart? = null
    lateinit var scanner: BleScanner
    lateinit var manager: Manager
    lateinit var handler: Handler
    val bleBeanSet = HashMap<String, BluetoothBean>()
    val timeout = 5
    var now = -1L
    var connected = false
    val deviceTypes = arrayOf(
        DeviceType.ArmBand,
        DeviceType.BikeComputer,
        DeviceType.Cadence,
        DeviceType.HRMonitor,
        DeviceType.HubConfig,
        DeviceType.Jump,
        DeviceType.KettleBell,
        DeviceType.Scale,
        DeviceType.Tracker
    )
    val permissions = arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.FOREGROUND_SERVICE
    )
    val heartRateHistory = ConcurrentLinkedQueue<HeartRate>()
    val stepHistory = ConcurrentLinkedQueue<StepFrequencyEntity>()
    var battery = -1
    var monitor = false
    val sqlite by lazy { RoomDatabaseHelper.getInstance(activity) }

    fun syncActivity(activity: Activity?): Activity {
        activity ?: return this.activity
        if (!this::activity.isInitialized || this.activity != activity) {
            this.activity = activity
        }
        return this.activity
    }

    fun queryPermissions(activity: Activity? = null) {
        val queryPermissions = permissions.toMutableList().apply {
            removeIf {
                ContextCompat.checkSelfPermission(
                    syncActivity(activity),
                    it
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        if (queryPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                Core.activity,
                queryPermissions.toTypedArray(),
                permissions.hashCode() and 255
            )
        }
    }

    fun t1(activity: Activity? = null) {
        Alerter.create(
            syncActivity(
                activity
            )
        ).setText("测试阶段2\n此处已无代码").show()
    }

    fun connect(activity: Activity? = null) {
        queryPermissions(
            syncActivity(activity)
        )
        if (!connected) {
            now = -1L
            sdk = FitBleKit.getInstance()
            sdk.initSDK(Core.activity)
            scanner = BleScanner()
            scanner.startScan(this)
        }
    }

    fun paint(onOff: Boolean) {
        monitor = onOff
        if (onOff) {
            Pool.workAround.add(this)
        } else {
            Pool.workAround.remove(this)
        }
    }

    fun onRequestPermissionsResult(
        activity: Activity? = null,
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        syncActivity(activity)
        if (requestCode == permissions.hashCode()) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Alerter.create(Core.activity).setText("权限申请成功!").show()
            } else {
                Alerter.create(Core.activity).setText("权限申请失败，或部分失败!").show()
            }
        }
    }

    override fun unFindDevice() {
//        if (!Alerter.isShowing) {
//            Alerter.create(activity).setText("未知触发unFindDevice").show()
//        }
    }

    override fun findDevice(p0: BluetoothBean?) {
        val name = p0?.bleDevice?.name ?: return
        if (!bleBeanSet.contains(name) && !Alerter.isShowing) {
            bleBeanSet.put(name, p0)
            Alerter.create(activity).setText("发现设备:$name").show()
        }
        if (now < 0) {
            now = System.currentTimeMillis()
        } else if (System.currentTimeMillis() > now + timeout * 1000) {
            scanner.stopScan()
            showScanResult()
        }
    }

    fun showScanResult() {
        if (bleBeanSet.isEmpty()) {
            return
        }

        val layout = FlexboxLayout(activity)
        val dialogPlus = DialogPlus.newDialog(activity)
            .apply {
                isCancelable = false
            }
            .setContentHolder(ViewHolder(layout))
            .setGravity(Gravity.CENTER)
            .create()
        layout.apply {
            flexWrap = FlexWrap.WRAP
            addView(TextView(activity).apply {
                text = "请选择检测到的设备:"
            })
            bleBeanSet.values.forEach {
                addView(Button(activity).apply {
                    val device =
                        BaseDevice(it.deviceType, it.bleDevice.name, it.bleDevice.address)
                    text = device.name
                    setOnClickListener {
                        baseDevice = device
                        if (device.deviceType == null) {
                            layout.removeAllViews()
                            unKnownBound(dialogPlus, layout)
                        } else {
                            dialogPlus.dismiss()
                            bound()
                        }
                    }
                })
            }
        }
        dialogPlus.show()
    }

    fun unKnownBound(
        dialogPlus: DialogPlus,
        layout: FlexboxLayout
    ) {
        layout.apply {
            addView(TextView(activity).apply {
                text = "请选择[${baseDevice.name}]类型:"
            })
            deviceTypes.forEach {
                addView(Button(activity).apply {
                    text = it.name
                    setOnClickListener { _ ->
                        dialogPlus.dismiss()
                        baseDevice.deviceType = it
                        bound()
                    }
                })
            }
        }
    }

    fun bound() {
        when (baseDevice.deviceType) {
            DeviceType.ArmBand -> {
                manager = ArmBandManager.getInstance().apply {
                    registerRealTimeDataListner(this@Core)
                }
            }
            DeviceType.HRMonitor -> {
                manager = HeartRateMonitorManager.getInstance().apply {
                    setHeartRateListener(this@Core)
                }
            }
            DeviceType.BikeComputer -> {
            }
            DeviceType.Cadence -> {
            }
            DeviceType.HubConfig -> {
            }
            DeviceType.Jump -> {
            }
            DeviceType.KettleBell -> {
            }
            DeviceType.Scale -> {
            }
            DeviceType.Tracker -> {
            }
            null -> {
            }
        }
        manager.apply {
            registerCheckSystemBleCallback(this@Core)
            registerStateChangeCallback(this@Core)
            disconnect(false)
            closeDevice()
            refreshDeviceCache()
            connectDevice(baseDevice)
            blService = BluetoothLeService.getInstance(null)
        }
    }

    override fun onRealTimeHeartRateData(p0: RTHeartRate?) {
        heartRateHistory.add(sqlite.addHeartRate(HeartRate(p0 ?: return)))
    }

    override fun onRealTimeStepFrequencyData(p0: StepFrequencyEntity?) {
        stepHistory.add(p0 ?: return) //TODO:添加计步器支持
    }

    override fun onGotBatteryLevel(p0: Int) {
        battery = p0
    }

    override fun onBleSwitchedBySystem(p0: Boolean) {
//        if (!Alerter.isShowing) {
//            Alerter.create(activity).setText("未知触发onBleSwitchedBySystem $p0").show()
//        }
    }

    override fun onRequestSwitchOnBle() {
//        if (!Alerter.isShowing) {
//            Alerter.create(activity).setText("未知触发onRequestSwitchOnBle").show()
//        }
    }

    override fun onEnableWriteToDevice(p0: String?, p1: Boolean) {
//        if (!Alerter.isShowing) {
//            Alerter.create(activity).setText("未知触发onEnableWriteToDevice $p0 $p1").show()
//        }
    }

    override fun onStateChange(p0: String?, p1: Int) {
        if (!Alerter.isShowing) {
            var msg = ""
            when (p1) {
                BleDevice.STATE_CONNECTED -> {
                    msg = "已连接"
                }
                BleDevice.STATE_CONNECTING -> {
                    msg = "正在连接"
                }
                BleDevice.STATE_DISCONNECTED -> {
                    msg = "连接断开"
                }
                BleDevice.STATE_SCANING -> {
                    msg = "正在扫描"
                }
                BleDevice.STATE_SERVICES_DISCOVERED -> {
                    msg = "已发现服务"
                }
                BleDevice.STATE_SERVICES_OPENCHANNELSUCCESS -> {
                    msg = "连接服务成功"
                }
            }
            Alerter.create(activity).setText("蓝牙连接状态:($p0)$msg").show()
        }
    }

    override fun onHeartRateValueChange(p0: MutableList<RTHeartRate>?) {
        p0?.forEach { sqlite.addHeartRate(HeartRate(it)) }
    }

    override fun onRealTimeHeartRateValue(p0: RTHeartRate?) {
        sqlite.addHeartRate(HeartRate(p0 ?: return))
    }

    fun quit() {
        manager.disconnect(false)
        manager.closeDevice()
        heartRateHistory.clear()
        stepHistory.clear()
        bleBeanSet.clear()
    }

    override fun call(): Any {
        //Log.i(this::class.simpleName,"===画图===$monitor===${heartRateHistory.size}")
        heartRateChart ?: return 2
        while (heartRateHistory.isNotEmpty()) {
            if (!monitor) {
                return 1
            }
            heartRateChart!!.addHeartRate(heartRateHistory.poll() ?: continue)
        }
        heartRateChart!!.repaint()
        return 0
    }

    fun shot() {
    }
}