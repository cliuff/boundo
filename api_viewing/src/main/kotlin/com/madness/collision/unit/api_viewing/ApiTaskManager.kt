package com.madness.collision.unit.api_viewing

import android.util.LongSparseArray
import kotlinx.coroutines.*
import kotlin.random.Random

internal object ApiTaskManager{
    /**
     * run immediately
     */
    const val TASK_NOW = 1
    /**
     * pause all other tasks
     */
    const val TASK_JOIN = 2
    /**
     * after other tasks finish
     */
    //const val TASK_PEND = 3

    private val jobs = LongSparseArray<Job>()

    fun now(dispatcher: CoroutineDispatcher = Dispatchers.Default, task: Runnable) = now(dispatcher) { task.run() }

    fun now(dispatcher: CoroutineDispatcher = Dispatchers.Default, task: () -> Unit): Job = add(TASK_NOW, dispatcher, task)

    fun join(dispatcher: CoroutineDispatcher = Dispatchers.Default, task: Runnable) = join(dispatcher) { task.run() }

    fun join(dispatcher: CoroutineDispatcher = Dispatchers.Default, task: () -> Unit): Job = add(TASK_JOIN, dispatcher, task)

    infix fun plus(task: Runnable) = plus { task.run() }

    infix fun plus(task: () -> Unit): Job = add(TASK_NOW, task = task)

    private fun (() -> Unit).launchJob(dispatcher: CoroutineDispatcher = Dispatchers.Default): Job {
        return GlobalScope.launch(dispatcher) { invoke() }
    }

    private fun (() -> Unit).joinJob(dispatcher: CoroutineDispatcher = Dispatchers.Default): Job {
        val j = launchJob(dispatcher)
        GlobalScope.launch { j.join() }
        return j
    }

    fun add(taskParam: Int, dispatcher: CoroutineDispatcher = Dispatchers.Default, task: Runnable) = add(taskParam, dispatcher) { task.run() }

    fun add(taskParam: Int, dispatcher: CoroutineDispatcher = Dispatchers.Default, task: () -> Unit): Job{
        if (Random(System.currentTimeMillis()).nextInt(6) == 0) synchronized(jobs){ tidy() }
        return when (taskParam) {
            TASK_NOW -> task.launchJob(dispatcher)
            TASK_JOIN -> task.joinJob(dispatcher)
            else -> task.launchJob(dispatcher)
        }.also { jobs.append(System.currentTimeMillis(), it) }
    }

    private fun tidy(){
        val list = mutableListOf<Long>()
        for (i in 0 until jobs.size()){
            val j: Job
            try {
                j = jobs.valueAt(i) ?: continue
            } catch (e: ClassCastException) {
                e.printStackTrace()
                continue
            }
            if (j.isCompleted || j.isCancelled) list.add(jobs.keyAt(i))
        }
        list.forEach { jobs.remove(it) }
    }

    fun cancelAll(){
        val list = mutableListOf<Long>()
        for (i in 0 until jobs.size()){
            val j = jobs.valueAt(i) ?: continue
            if (j.isCompleted || j.isCancelled) list.add(jobs.keyAt(i))
            else j.cancel()
        }
        list.forEach { jobs.remove(it) }
    }
}