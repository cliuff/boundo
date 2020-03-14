package com.madness.collision.unit.audio_timer

import android.content.Context
import com.madness.collision.unit.Bridge
import com.madness.collision.unit.Unit

object MyBridge: Bridge() {

    override val unitName: String = Unit.UNIT_NAME_AUDIO_TIMER

    /**
     * @param args empty
     */
    override fun getUnitInstance(vararg args: Any?): Unit {
        return MyUnit()
    }

    fun start(context: Context) {
        AudioTimerService.start(context)
    }
}
