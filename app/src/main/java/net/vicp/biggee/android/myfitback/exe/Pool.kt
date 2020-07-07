package net.vicp.biggee.android.myfitback.exe

import android.util.Log
import java.util.*
import java.util.concurrent.*

object Pool : ConcurrentHashMap<Int, Future<out Any>>(), ThreadFactory,
    Thread.UncaughtExceptionHandler, ForkJoinPool.ForkJoinWorkerThreadFactory, Runnable {
    val name by lazy { "${this::class.simpleName}${hashCode()}" }
    val singlePool by lazy { Executors.newSingleThreadScheduledExecutor(this) }
    val pool by lazy { ForkJoinPool(coreSize, this, this, false) }
    val cachePool by lazy { Executors.newCachedThreadPool(this) }
    val group by lazy { ThreadGroup(name) }
    val coreSize by lazy { Runtime.getRuntime().availableProcessors() }
    val workSpace by lazy { ConcurrentLinkedQueue<Callable<out Any>>() }
    val workAround by lazy { Collections.synchronizedList(ArrayList<Callable<out Any>>()) }

    private val semaphore = Semaphore(1)

    init {
        Thread.setDefaultUncaughtExceptionHandler(this)
        singlePool.scheduleAtFixedRate(this, 1000, 100, TimeUnit.MILLISECONDS)
    }

    fun addJob(callable: Callable<out Any>): Int {
        workSpace.offer(callable)
        return callable.hashCode()
    }

    fun addJob(runnable: Runnable) = addJob(Callable { runnable.run() })

    override fun newThread(r: Runnable?): Thread {
        val t = Thread(group, r, "$name-${r.hashCode()}").apply { isDaemon = true }
        return t
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        if (e is RejectedExecutionException) {
            addJob(Runnable { t.run() })
            return
        }
        Log.e(name, "错误:$t", e)
    }

    override fun newThread(pool: ForkJoinPool?): ForkJoinWorkerThread {
        return MyForkJoinWorkerThread(pool ?: this.pool)
    }

    class MyForkJoinWorkerThread(pool: ForkJoinPool = Pool.pool) : ForkJoinWorkerThread(pool) {

        override fun onStart() {
            if (pool.activeThreadCount > coreSize * coreSize || group.activeCount() > coreSize * 10) {
                addJob(Runnable { super.onStart() })
                return
            }
            super.onStart()
        }
    }

    override fun run() {
        if (!semaphore.tryAcquire()) {
            return
        }
        try {
            workAround.parallelStream().forEach { workSpace.offer(it) }
            while (workSpace.isNotEmpty()) {
                val poll = workSpace.poll() ?: continue
                put(poll.hashCode(), pool.submit(poll))
            }
        } finally {
            semaphore.release()
        }
    }
}