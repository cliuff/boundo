package com.madness.collision.unit.audio_timer

import android.content.Context
import com.madness.collision.unit.Unit
import com.madness.collision.unit.UnitAccess

object AccessAT: UnitAccess(Unit.UNIT_NAME_AUDIO_TIMER) {

    fun start(context: Context) {
        getMethod("start", Context::class).invoke(context)
    }
}
