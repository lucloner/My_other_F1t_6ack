package net.vicp.biggee.android.myfitback.dev

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.collection.LongSparseArray
import androidx.collection.isNotEmpty
import androidx.collection.size
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.YAxis.AxisDependency
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.onecoder.devicelib.FitBleKit
import com.onecoder.devicelib.armband.api.ArmBandManager
import com.onecoder.devicelib.armband.api.entity.StepFrequencyEntity
import com.onecoder.devicelib.armband.api.interfaces.RealTimeDataListener
import com.onecoder.devicelib.base.api.Manager
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
import com.tapadoo.alerter.Alerter
import kotlinx.coroutines.sync.Semaphore
import net.vicp.biggee.android.myfitback.R
import java.util.concurrent.Executors
import kotlin.math.max

object Core : BleScanCallBack, RealTimeDataListener, CheckSystemBleCallback,
    DeviceStateChangeCallback, HeartRateListener, OnChartValueSelectedListener {
    lateinit var activity: Activity
    lateinit var sdk: FitBleKit
    lateinit var blService: BluetoothLeService
    lateinit var baseDevice: BaseDevice
    var chart: LineChart? = null
    lateinit var scanner: BleScanner
    lateinit var manager: Manager
    lateinit var handler: Handler
    val bleBeanSet = HashSet<BluetoothBean>()
    val timeout = 5
    var now = -1L
    var connected = false
    val deviceTypes = arrayOf(
        DeviceType.ArmBand,
        //DeviceType.BikeComputer,
        //DeviceType.Cadence,
        DeviceType.HRMonitor,
        //DeviceType.HubConfig,
        //DeviceType.Jump,
        //DeviceType.KettleBell,
        //DeviceType.Scale,
        DeviceType.Tracker
    )
    val permissions = arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.FOREGROUND_SERVICE
    )
    val heartRateHistory = LongSparseArray<Int>()
    val stepHistory = LongSparseArray<StepFrequencyEntity>()
    var battery = -1
    val paintSemaphore = Semaphore(1)
    val pool = Executors.newWorkStealingPool()

    fun syncActivity(activity: Activity?): Activity {
        if (activity != null && (!this::activity.isLateinit || this.activity != activity)) {
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
                this.activity,
                queryPermissions.toTypedArray(),
                permissions.hashCode() and 255
            )
        }
    }

    fun t1(activity: Activity? = null) {
        Alerter.create(syncActivity(activity)).setText("测试阶段2\n此处已无代码").show()
    }

    fun connect(activity: Activity? = null) {
        queryPermissions(syncActivity(activity))
        if (!connected) {
            now = -1L
            sdk = FitBleKit.getInstance()
            sdk.initSDK(this.activity)
            scanner = BleScanner()
            scanner.startScan(this)
        }
    }

    fun paint(
        activity: Activity? = null,
        handler: Handler,
        onOff: Boolean,
        lineChart: LineChart
    ) {
        syncActivity(activity)
        this.handler = handler
        if (onOff) {
            chart = lineChart.apply {
                data = (data ?: LineData()).apply {
                    if (dataSetCount < 1) {
                        addDataSet(LineDataSet(null, "HeartRate"))
                    }
                }
            }
        } else {
            chart = null
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
                Alerter.create(this.activity).setText("权限申请成功!").show()
            } else {
                Alerter.create(this.activity).setText("权限申请失败，或部分失败!").show()
            }
        }
    }

    override fun unFindDevice() {
        if (!Alerter.isShowing) {
            Alerter.create(this.activity).setText("未知触发unFindDevice").show()
        }
    }

    override fun findDevice(p0: BluetoothBean?) {
        p0?.bleDevice?.name ?: return
        if (bleBeanSet.add(p0) && !Alerter.isShowing) {
            Alerter.create(this.activity).setText("发现设备:${p0.bleDevice.name}").show()
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
        val alerter = Alerter.create(activity).setText("请选择连接的蓝牙设备")
            .disableOutsideTouch()
            .setDismissable(false)
            .setDuration(60 * 1000)
        bleBeanSet.forEach {
            alerter.addButton(
                it.bleDevice.name,//(${it.bleDevice.address})
                R.style.AlertButton, View.OnClickListener { v ->
                    Alerter.hide()
                    baseDevice = BaseDevice()
                    baseDevice.deviceType = it.deviceType
                    baseDevice.macAddress = it.bleDevice.address
                    baseDevice.name = it.bleDevice.name
                    if (baseDevice.deviceType == null) {
                        unKnownBound()
                    } else {
                        bound()
                    }
                })
        }
        alerter.show()
    }


    fun unKnownBound() {
        val alerter = Alerter.create(activity).setText("请选择连接的设备类型")
            .disableOutsideTouch()
            .setDismissable(false)
            .setDuration(60 * 1000)
        deviceTypes.forEach {
            alerter.addButton(it.name, R.style.AlertButton, View.OnClickListener { v ->
                Alerter.hide()
                baseDevice.deviceType = it
                bound()
            })
        }
        alerter.show()
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
        heartRateHistory.put(p0?.utc ?: return, p0.heartRate)
        repaint()
    }

    fun repaint() {
        pool.execute {
            if (!paintSemaphore.tryAcquire()) {
                return@execute
            }
            try {
                val msg = StringBuilder()
                chart?.apply {
                    if (heartRateHistory.isEmpty) {
                        return@apply
                    }

                    var hrMsg = ""
                    var maxY = 100
                    val cnt = heartRateHistory.size()
                    val hasPainted = data.entryCount
                    var realIndex = 0
                    for (index in 0 until cnt) {
                        val t = heartRateHistory.keyAt(index)
                        val r = heartRateHistory[t] ?: continue
                        if (r <= 0) {
                            continue
                        }
                        val entry = Entry(index.toFloat() + hasPainted, r.toFloat())
                        data.addEntry(entry, realIndex++)
                        maxY = max(maxY, r)
                        hrMsg = "心率:$r"
                    }

                    heartRateHistory.clear()

                    handler.post {
                        notifyDataSetChanged()
                        setVisibleXRangeMaximum(6F)
                        moveViewTo(data.entryCount - 7F, maxY.toFloat(), AxisDependency.LEFT)
                    }

                    msg.append(hrMsg)
                }

                if (!Alerter.isShowing) {
                    if (stepHistory.isNotEmpty()) {
                        var size = stepHistory.size
                        var time = stepHistory.keyAt(size - 1)
                        val step = stepHistory[time]
                        if (step != null && step.stepFrequency > 0) {
                            msg.append("\n计步器:${step.currentTotalSteps}")
                        }
                        stepHistory.clear()
                    }

                    if (battery >= 0) {
                        msg.append("\n电池:${battery}")
                    }

                    if (msg.isEmpty()) {
                        msg.append("连接提示:未获得信息")
                    }
                    Alerter.create(activity).setText(msg.toString()).show()
                }
            } catch (e: Exception) {
                Log.e(this.javaClass.simpleName, "repaint", e)
            } finally {
                paintSemaphore.release()
            }
        }
    }

    override fun onRealTimeStepFrequencyData(p0: StepFrequencyEntity?) {
        stepHistory.put(System.currentTimeMillis(), p0 ?: return)
        repaint()
    }

    override fun onGotBatteryLevel(p0: Int) {
        battery = p0
    }

    override fun onBleSwitchedBySystem(p0: Boolean) {
        if (!Alerter.isShowing) {
            Alerter.create(this.activity).setText("未知触发onBleSwitchedBySystem $p0").show()
        }
    }

    override fun onRequestSwitchOnBle() {
        if (!Alerter.isShowing) {
            Alerter.create(this.activity).setText("未知触发onRequestSwitchOnBle").show()
        }
    }

    override fun onEnableWriteToDevice(p0: String?, p1: Boolean) {
        if (!Alerter.isShowing) {
            Alerter.create(this.activity).setText("未知触发onEnableWriteToDevice $p0 $p1").show()
        }
    }

    override fun onStateChange(p0: String?, p1: Int) {
        if (!Alerter.isShowing) {
            Alerter.create(this.activity).setText("未知触发onStateChange $p0 $p1").show()
        }
    }

    override fun onHeartRateValueChange(p0: MutableList<RTHeartRate>?) {
        p0?.forEach {
            heartRateHistory.put(it.utc, it.heartRate)
        } ?: return
        repaint()
    }

    override fun onRealTimeHeartRateValue(p0: RTHeartRate?) {
        heartRateHistory.put(p0?.utc ?: return, p0.heartRate)
        repaint()
    }

    fun quit() {
        manager.disconnect(false)
        manager.closeDevice()
        heartRateHistory.clear()
        stepHistory.clear()
        bleBeanSet.clear()
    }

    override fun onNothingSelected() {

    }

    override fun onValueSelected(e: Entry?, h: Highlight?) {
        Toast.makeText(activity, e.toString(), Toast.LENGTH_SHORT).show()
    }
}