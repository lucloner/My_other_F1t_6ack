package net.vicp.biggee.android.myfitback

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.*
import android.os.Process.THREAD_PRIORITY_BACKGROUND
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import net.vicp.biggee.android.myfitback.ui.gallery.GalleryViewModel
import net.vicp.biggee.android.myfitback.ui.home.HomeViewModel
import net.vicp.biggee.android.myfitback.ui.slideshow.SlideshowViewModel
import java.util.concurrent.ConcurrentHashMap

class CoreService : Service() {
    private var serviceLooper: Looper? = null
    private var serviceHandler: ServiceHandler? = null
    private var startMode: Int = 0             // indicates how to behave if the service is killed
    private val binder = LocalBinder()            // interface for clients that bind
    private var allowRebind: Boolean = false   // indicates whether onRebind should be used

    // Handler that receives messages from the thread
    private inner class ServiceHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            // Normally we would do some work here, like download a file.
            // For our sample, we just sleep for 5 seconds.
            try {
                Thread.sleep(5000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
                // Restore interrupt status.
                Thread.currentThread().interrupt()
            }

            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            stopSelf(msg.arg1)
        }
    }

    inner class CoreViewModel : ViewModel()

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): CoreService = this@CoreService
    }


    override fun onCreate() {
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread("ServiceStartArguments", THREAD_PRIORITY_BACKGROUND).apply {
            start()

            // Get the HandlerThread's Looper and use it for our Handler
            serviceLooper = looper
            serviceHandler = ServiceHandler(looper)
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Toast.makeText(this, "后台服务启动", Toast.LENGTH_SHORT).show()

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        serviceHandler?.obtainMessage()?.also { msg ->
            msg.arg1 = startId
            serviceHandler?.sendMessage(msg)
        }

        val pendingIntent: PendingIntent =
            Intent(this, this::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
            }

        val notification: Notification = Notification.Builder(this, Core.channelId)
            .setContentTitle("XXXX教练端")
            .setContentText("服务中")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setTicker("未知")
            .build()

        startForeground(Core.requestCodeBase - 1, notification)
        Log.i(this::class.simpleName, "服务已经启动!")

        // If we get killed, after returning from here, restart
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        // We don't provide binding, so return null
        return binder
    }

    override fun onDestroy() {
        Toast.makeText(this, "后台服务关闭", Toast.LENGTH_SHORT).show()
    }

    companion object {
        val viewModels = ConcurrentHashMap<String, ViewModel>()

        fun getHomeViewMode() = getViewMode(HomeViewModel::class.java)
        fun getGalleryViewMode() = getViewMode(GalleryViewModel::class.java)
        fun getSlideshowViewMode() = getViewMode(SlideshowViewModel::class.java)

        inline fun <reified T> getViewMode(clazz: Class<T>): T? {
            try {
                val key = T::class.qualifiedName ?: return null
                val result = viewModels.get(key) ?: return null
                return result as T
            } catch (e: Exception) {
                Log.e(this::class.simpleName, "获取保存视图出错!", e)
            }
            return null
        }

        fun <T : ViewModel> putViewMode(viewModel: T) =
            viewModels.putIfAbsent(viewModel::class.qualifiedName ?: "", viewModel)
    }
}