package net.vicp.biggee.android.myfitback.dev

import android.Manifest
import android.app.Activity
import android.app.Service
import android.bluetooth.BluetoothGatt
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.onecoder.devicelib.FitBleKit
import com.onecoder.devicelib.armband.api.ArmBandManager
import com.onecoder.devicelib.armband.api.entity.StepFrequencyEntity
import com.onecoder.devicelib.armband.api.interfaces.RealTimeDataListener
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
import com.onecoder.devicelib.heartrate.control.HeartRateMonitorController
import com.onecoder.devicelib.heartrate.protocol.HeartRateMonitorProtocol
import com.onecoder.devicelib.tracker.api.TrackerManager
import com.onecoder.devicelib.utils.BluetoothUtils
import com.tapadoo.alerter.Alerter
import java.lang.Exception
import java.util.*
import java.util.stream.Collectors

object Core {
    val sdk by lazy { FitBleKit.getInstance() }
    val blService by lazy {
        var blService = BluetoothLeService.getInstance(context)
        while (blService == null) {
            blService = BluetoothLeService.getInstance(context)
            Thread.sleep(10)
        }
        return@lazy blService
    }
    val scanner by lazy {
        BleScanner().apply {
            addNameFilter(DeviceType.HRMonitor, arrayOf("BD"))
            //addNameFilter(DeviceType.ArmBand)
            registerDeviceStatusReceiver(context)
        }
    }
    lateinit var context: Context
    lateinit var baseDevice: BaseDevice
    fun t1(activity: Activity) {
        context = activity
        if (sdk.initSDK(activity)) {
            Alerter.create(activity).setText("Succeed").show()
        } else {
            Alerter.create(activity).setText("Failed").show()
            //return
        }

        openBluetooth(activity)
        scanner.startScan(object : BleScanCallBack{
            override fun unFindDevice() {
                Log.i("CORE_RUN", "0.1_")
            }

            override fun findDevice(p0: BluetoothBean?) {
                Log.i("CORE_RUN", "0.2_${p0?.bleDevice?.name}")
                scanner.stopScan()
            }
        })



        Log.e(
            "CORE",
            "bl:1${blService}:${BluetoothLeService.getImportanceOfNotificationChannel()}:${BluetoothLeService.getNotificationChannelId()}:${BluetoothLeService.getNotificationChannelName()}"
        )
        blService ?: return

        Log.e("CORE", "bl:2")
        //val hr = ArmBandManager.getInstance()
        val hr = HeartRateMonitorManager.getInstance()
        hr.setHeartRateListener(object : HeartRateListener {
            override fun onHeartRateValueChange(p0: MutableList<RTHeartRate>?) {
                Log.i("CORE_RUN", "1.1_${p0?.last()?.heartRate}")
            }

            override fun onRealTimeHeartRateValue(p0: RTHeartRate?) {
                Log.i("CORE_RUN", "1.2_${p0?.heartRate}")
            }
        })

        hr.apply {
            disconnect(false)
            closeDevice()
            refreshDeviceCache()

//            registerRealTimeDataListner(object : RealTimeDataListener{
//                override fun onRealTimeHeartRateData(p0: RTHeartRate?) {
//                    Log.i("CORE_RUN", "1_${p0?.heartRate}")
//                }
//
//                override fun onRealTimeStepFrequencyData(p0: StepFrequencyEntity?) {
//                    Log.i("CORE_RUN", "2_${p0?.currentTotalSteps}")
//                }
//            })
            registerCheckSystemBleCallback(object : CheckSystemBleCallback {
                override fun onBleSwitchedBySystem(p0: Boolean) {
                    Log.i("CORE_RUN", "3_${p0}")
                }

                override fun onRequestSwitchOnBle() {
                    Log.i("CORE_RUN", "4_")
                }
            })
            registerStateChangeCallback(object : DeviceStateChangeCallback {
                override fun onEnableWriteToDevice(p0: String?, p1: Boolean) {
                    Log.i("CORE_RUN", "5_${p0} $p1")
                }

                override fun onStateChange(p0: String?, p1: Int) {
                    Log.i("CORE_RUN", "6_${p0} $p1")
                }
            })

            baseDevice.deviceType = DeviceType.HRMonitor
            connectDevice(baseDevice)
            Alerter.create(activity).setText("Succeed01${connectDevice(baseDevice)}").show()


        }


    }

    private val permissions = arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.FOREGROUND_SERVICE
    )

    fun openBluetooth(activity: Activity) {
        this.context = activity
        val queryPermissions = permissions.toMutableList().apply {
            removeIf {
                ContextCompat.checkSelfPermission(
                    activity,
                    it
                ) == PackageManager.PERMISSION_GRANTED
            }
        }

        if (queryPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                queryPermissions.toTypedArray(),
                queryPermissions.hashCode() and 255
            )
            Alerter.create(activity).setText("permission is not ok!").show()
            Log.e("CORE", "not ok")
        }

        val blDev = BluetoothUtils.getBondedBluetoothClassicDevices()
        var msg =
            "open:${BluetoothUtils.openBluetooth()} support:${BluetoothUtils.isBleSupported()} enabled:${BluetoothUtils.isBluetoothEnabled()} bonded:${blDev.stream()
                .map { it.name }.collect(Collectors.toList())}"

        Alerter.create(activity)
            .setText(msg)
            .show()
        Log.e("CORE", "bl:$msg")

        val target = blDev.first { it.name.startsWith("BD") }

        baseDevice = BaseDevice().apply {
            macAddress = target.address
            deviceType = DeviceType.ArmBand
            name = target.name
        }

        Log.e("CORE", "bl tar:${target.name}")

    }


}