package com.madness.collision.qs

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.madness.collision.main.MainActivity
import com.madness.collision.unit.Unit
import com.madness.collision.util.X

internal class PrefActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.action != TileService.ACTION_QS_TILE_PREFERENCES) {
            finish()
            return
        }
        val cn: ComponentName? = if (X.aboveOn(X.O)) intent.getParcelableExtra(Intent.EXTRA_COMPONENT_NAME) else null
        if (cn != null) {
            when(cn.className) {
                TileServiceAudioTimer::class.qualifiedName -> {
                    Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        putExtras(MainActivity.forItem(Unit.UNIT_NAME_AUDIO_TIMER))
                    }
                }
                TileServiceApiViewer::class.qualifiedName -> {
                    Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        putExtras(MainActivity.forItem(Unit.UNIT_NAME_API_VIEWING))
                    }
                }
                else -> {
                    X.toast(this, "2333", Toast.LENGTH_SHORT)
                    null
                }
            }?.let { startActivity(it) }
        } else {
            X.toast(this, "2333", Toast.LENGTH_SHORT)
        }
        finish()
    }

}
