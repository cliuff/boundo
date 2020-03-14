package com.madness.collision.unit.themed_wallpaper

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.madness.collision.main.MainActivity
import com.madness.collision.unit.Unit

internal class ConfigActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Intent(this@ConfigActivity, MainActivity::class.java).run {
            putExtras(MainActivity.forItem(Unit.UNIT_NAME_THEMED_WALLPAPER))
            startActivity(this)
        }
        finish()
    }
}
