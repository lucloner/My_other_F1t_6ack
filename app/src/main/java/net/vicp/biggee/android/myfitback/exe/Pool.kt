package net.vicp.biggee.android.myfitback.exe

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

object Pool : ConcurrentHashMap<Thread, Runnable>(), ThreadFactory,
    Thread.UncaughtExceptionHandler {
    val name by lazy { "${this::class.simpleName}${hashCode()}" }
    val singlePoll by lazy { Executors.newSingleThreadScheduledExecutor(this) }
    val pool by lazy { Executors.newWorkStealingPool() }
    val cachePool by lazy { Executors.newCachedThreadPool(this) }
    val group by lazy { ThreadGroup(name) }

    init {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun newThread(r: Runnable?): Thread {
        val t = Thread(group, r, "$name-${r.hashCode()}").apply { isDaemon = true }
        return t
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        Log.e(name, "错误:$t", e)
    }
}