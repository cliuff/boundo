package com.madness.collision.instant.shortcut

import android.annotation.TargetApi
import com.madness.collision.R
import com.madness.collision.unit.Unit
import com.madness.collision.util.P
import com.madness.collision.util.X

@TargetApi(X.N_MR1)
internal object InstantShortcuts {

    val SHORTCUTS = listOf(
            InstantShortcut(P.SC_ID_API_VIEWER, R.string.apiViewer, Unit.UNIT_NAME_API_VIEWING),
            InstantShortcut(P.SC_ID_AUDIO_TIMER, R.string.unit_audio_timer, Unit.UNIT_NAME_AUDIO_TIMER)
    )

}
