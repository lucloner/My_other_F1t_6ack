package net.vicp.biggee.android.myfitback

import android.Manifest
import android.app.Activity
import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.NumberPicker
import android.widget.TextView
import androidx.core.view.children
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import com.google.gson.Gson
import com.onecoder.devicelib.FitBleKit
import com.onecoder.devicelib.armband.api.ArmBandManager
import com.onecoder.devicelib.armband.api.entity.HistoryDataEntity
import com.onecoder.devicelib.armband.api.entity.StepFrequencyEntity
import com.onecoder.devicelib.armband.api.interfaces.RealTimeDataListener
import com.onecoder.devicelib.armband.api.interfaces.SynchHistoryDataCallBack
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
import com.pdog.dimension.dp
import com.tapadoo.alerter.Alerter
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import net.vicp.biggee.android.myfitback.db.room.Course
import net.vicp.biggee.android.myfitback.db.room.HeartRate
import net.vicp.biggee.android.myfitback.db.room.Member
import net.vicp.biggee.android.myfitback.db.room.RoomDatabaseHelper
import net.vicp.biggee.android.myfitback.exe.Pool
import net.vicp.biggee.android.myfitback.ui.HeartRateChart
import pub.devrel.easypermissions.EasyPermissions
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.random.Random

object Core : BleScanCallBack, RealTimeDataListener, CheckSystemBleCallback,
    DeviceStateChangeCallback, HeartRateListener, Callable<Any>,
    EasyPermissions.PermissionCallbacks, EasyPermissions.RationaleCallbacks, BroadcastReceiver(),
    IWXAPIEventHandler, SynchHistoryDataCallBack {
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
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.ACCESS_NETWORK_STATE,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) Manifest.permission.FOREGROUND_SERVICE else null
    )
    val heartRateHistory = ConcurrentLinkedQueue<HeartRate>()
    val stepHistory = ConcurrentLinkedQueue<StepFrequencyEntity>()
    var battery = -1
    var monitor = false
    val sqlite by lazy { RoomDatabaseHelper.getInstance(activity) }
    var dialogPlus: DialogPlus? = null
    var dialogLayout: FlexboxLayout? = null
    val blServiceIntent by lazy {
        val i = Intent(activity, BluetoothLeService::class.java)
        activity.startService(i)
        return@lazy i
    }
    val permissionGranted = HashSet<String>()
    val permissionDenied = HashSet<String>()
    val requestCodeBase by lazy { hashCode() and 128 }
    lateinit var tmpFile: File

    // APP_ID 替换为你的应用从官方网站申请到的合法appID
    const val APP_ID = "wx5cfc47c7c05b8d97"
    const val ACCESS_TOKEN =
        "https://api.weixin.qq.com/sns/oauth2/access_token?appid=$APP_ID&secret={1}&code={2}&grant_type=authorization_code"

    // IWXAPI 是第三方app和微信通信的openApi接口
    // 通过WXAPIFactory工厂，获取IWXAPI的实例
    val api by lazy { WXAPIFactory.createWXAPI(activity, APP_ID, true) }
    val memberList = LinkedHashSet<Member>().apply {
        add(Member.sampleMale)
        add(Member.sampleFemale)
    }
    val courseList = LinkedHashSet<Course>()
    lateinit var member: Member
    lateinit var course: Course
    var subject: Course? = null

    fun regToWx() {
        // 将应用的appId注册到微信
        api.registerApp(APP_ID)
        //建议动态监听微信启动广播进行注册到微信
        activity.registerReceiver(this, IntentFilter(ConstantsAPI.ACTION_REFRESH_WXAPP))
    }

    fun syncActivity(activity: Activity?): Activity {
        activity ?: return this.activity
        if (!this::activity.isInitialized || this.activity != activity) {
            this.activity = activity
        }
        return this.activity
    }

    fun queryPermissions(activity: Activity? = null) {
        syncActivity(activity)
        if (!EasyPermissions.hasPermissions(this.activity, *permissions)) {
            EasyPermissions.requestPermissions(
                this.activity,
                "需要获取您的权限",
                requestCodeBase,
                *permissions
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

    fun connect() {
        if (!connected) {
            now = -1L
            sdk = FitBleKit.getInstance()
            sdk.initSDK(activity)
            scanner = BleScanner()
            bleBeanSet.clear()
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

        dialogLayout = FlexboxLayout(activity)
        dialogPlus = DialogPlus.newDialog(activity)
            .setContentHolder(ViewHolder(dialogLayout))
            .setGravity(Gravity.CENTER)
            .create()
        dialogLayout?.apply dialogLayout@{
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
                            this@dialogLayout.removeAllViews()
                            unKnownBound()
                        } else {
                            dialogPlus?.dismiss()
                            bound()
                        }
                    }
                })
            }
        }
        dialogPlus?.show()
    }

    fun unKnownBound() {
        dialogLayout?.apply {
            addView(TextView(activity).apply {
                text = "请选择[${baseDevice.name}]类型:"
            })
            deviceTypes.forEach {
                addView(Button(activity).apply {
                    text = it.name
                    setOnClickListener { _ ->
                        dialogPlus?.dismiss()
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
                    if (baseDevice.deviceType == DeviceType.ArmBand) {
                        (manager as ArmBandManager).apply {
                            if (needSysBlePaired == true) {
                                pair("123456")
                                confirmPassword()
                            }
                            synchHistoryData(this@Core)
                            Log.e(this::class.simpleName, "手环协议类型:$protocolType")
                        }
                    }
                }
                BleDevice.STATE_CONNECTING -> {
                    msg = "正在连接"
                    connected = true
                }
                BleDevice.STATE_DISCONNECTED -> {
                    msg = "连接断开"
                    connected = false
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
            heartRateChart?.addHeartRate(heartRateHistory.poll() ?: continue)
        }
        heartRateChart?.repaint()
        return 0
    }

    fun shot() {
        dialogLayout = FlexboxLayout(activity)
        dialogPlus =
            DialogPlus.newDialog(activity).setContentHolder(ViewHolder(dialogLayout)).create()
        dialogLayout?.apply {
            flexWrap = FlexWrap.WRAP
            addView(TextView(activity).apply {
                text = "请选择检测到的设备:"
            })
            addView(Button(activity).apply {
                text = "相册"
                setOnClickListener {
                    dialogPlus?.dismiss()
                    activity.startActivityForResult(Intent(Intent.ACTION_PICK).apply {
                        type = "image/*"
                    }, requestCodeBase + 1)
                }
            })
            addView(Button(activity).apply {
                text = "拍照"
                setOnClickListener {
                    dialogPlus?.dismiss()
                    val fname = "IMG_${System.currentTimeMillis()}.jpg"
                    tmpFile = File(fname)
                    activity.startActivityForResult(
                        Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                            putExtra(MediaStore.EXTRA_OUTPUT,
                                activity.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                    ContentValues().apply {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                            Alerter.create(activity).setText("安卓版本Q以上").show()
                                            put(
                                                MediaStore.Images.Media.RELATIVE_PATH,
                                                "DCIM/Pictures"
                                            )
                                        } else {
                                            Alerter.create(activity).setText("安卓版本O以下").show()
                                            tmpFile = File(
                                                Environment.getExternalStorageDirectory(),
                                                "Pictures"
                                            ).apply { mkdirs() }
                                            tmpFile = File(tmpFile, fname).apply {
                                                delete()
                                                deleteOnExit()
                                            }
                                            put(
                                                MediaStore.Images.Media.DATA,
                                                tmpFile.absolutePath
                                            )
                                        }
                                        put(MediaStore.Images.Media.DISPLAY_NAME, fname)
                                        put(MediaStore.Images.Media.MIME_TYPE, "image/JPEG")
                                    }
                                ))
                        },
                        requestCodeBase + 2
                    )
                }
            })
        }
        dialogPlus?.show()
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        permissionDenied.addAll(perms)
        Log.e(this::class.simpleName, "===拒绝权限===$requestCode===$permissionDenied")
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        permissionGranted.addAll(perms)
        Log.e(this::class.simpleName, "===拒绝权限===$requestCode===$permissionGranted")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Alerter.create(activity).setText("请求权限成功:$permissionGranted\n失败:$permissionDenied").show()
    }

    override fun onRationaleDenied(requestCode: Int) {

    }

    override fun onRationaleAccepted(requestCode: Int) {

    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) {
            return
        }
        try {
            var bitmap: Bitmap? = null
            when (requestCode) {
                requestCodeBase + 1 -> {    //相册
                    bitmap = BitmapFactory.decodeStream(
                        activity.contentResolver.openInputStream(data!!.data!!)
                    )
                }
                requestCodeBase + 2 -> {    //拍照
                    val cursor = activity.contentResolver.query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        arrayOf(MediaStore.Images.Media._ID),
                        MediaStore.Images.Media.DISPLAY_NAME + "=? ",
                        arrayOf(tmpFile.name), null
                    )

                    bitmap =
                        BitmapFactory.decodeStream(
                            activity.contentResolver.openInputStream(
                                ContentUris.withAppendedId(
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                    cursor!!.apply { moveToFirst() }.getLong(0)
                                ).apply {
                                    Log.e(this::class.simpleName, "返回错误2:$this")
                                }
                            )
                        )
                    cursor.close()
                }
            }
            if (bitmap != null) {
                activity.findViewById<FlexboxLayout>(R.id.flexbox_layout)
                    .addView(ImageView(activity).apply {
                        adjustViewBounds = true
                        maxHeight = 300
                        //maxWidth = 300
                        setImageBitmap(bitmap)
                    })
            }
        } catch (e: Exception) {
            Log.e(this::class.simpleName, "返回错误:", e)
            Alerter.create(activity)
                .setText("错误:${e.localizedMessage}\t${e.stackTrace.contentToString()}").show()
        }
    }

    fun wxLogin() {
        api.handleIntent(activity.intent, this)
        //regToWx()
        api.sendReq(SendAuth.Req().apply {
            scope = "snsapi_userinfo"
            state = "${activity.packageName}${requestCodeBase - 1}"
        })
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        api.registerApp(APP_ID)
    }

    override fun onResp(p0: BaseResp?) {
        Log.d(this::class.simpleName, "获得WX返回:" + Gson().toJson(p0))
    }

    override fun onReq(p0: BaseReq?) {
        Log.d(this::class.simpleName, "发送WX请求:" + Gson().toJson(p0))
    }

    override fun onSynchStateChange(p0: Int) {

    }

    override fun onSynchAllHistoryData(p0: HistoryDataEntity?) {
        Alerter.create(activity).setText(Gson().toJson(p0)).show()
    }

    fun startCourse(connect: Boolean = connected) {
//        if(!connect) {
//            Alerter.create(activity)
//                .setText("还未连接蓝牙！")
//                .addButton("连接",onClick = View.OnClickListener {
//                    Alerter.hide()
//                    Pool.addJob(Runnable {
//                        while (Alerter.isShowing){
//                            Thread.sleep(1000)
//                        }
//                        connect()
//                        paint(true)
//                    })
//
//                })
//                .addButton("离线模式",onClick = View.OnClickListener {
//                    startCourse(true)
//                    paint(true)
//                })
//                .addButton("取消",onClick = View.OnClickListener {
//                    return@OnClickListener
//                })
//                .show()
//            connect()
//        }

        dialogPlus = DialogPlus.newDialog(activity)
            .setContentHolder(ViewHolder(R.layout.dialogue_course))
            .setGravity(Gravity.CENTER)
            .create().apply {
                val dialogSelected = findViewById(R.id.d_c_tv_title2) as TextView
                (findViewById(R.id.d_c_fb_member) as FlexboxLayout).apply {
                    val rnd = Random(requestCodeBase)
                    memberList.forEach {
                        addView(TextView(activity).apply {
                            text = it.name
                            setOnClickListener { _ ->
                                dialogSelected.text = it.name
                                member = it
                            }
                        })
                    }
                    children.forEach {
                        if (it is TextView) {
                            it.apply {
                                it.setBackgroundColor(rnd.nextInt())
                                it.textSize = 5F.dp
                            }
                        }
                    }
                }

                (findViewById(R.id.hourpicker) as NumberPicker).apply {
                    minValue = 0
                    maxValue = 12
                }
                (findViewById(R.id.minuteicker) as NumberPicker).apply {
                    minValue = 0
                    maxValue = 60
                }
                (findViewById(R.id.secondicker) as NumberPicker).apply {
                    minValue = 0
                    maxValue = 60
                }
                show()
            }
    }
}