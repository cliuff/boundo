package com.madness.collision.versatile

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.madness.collision.R
import com.madness.collision.main.MainActivity
import com.madness.collision.unit.Unit
import com.madness.collision.unit.api_viewing.AccessAV
import com.madness.collision.util.P
import com.madness.collision.util.ThemeUtil
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

internal class ApkSharing: AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.updateTheme(this, getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE))
        setContentView(R.layout.fragment_loading)
        GlobalScope.launch {
            val extras = intent.extras
            if (extras == null) {
                finish()
                return@launch
            }
            val res: Uri? = extras.getParcelable(Intent.EXTRA_STREAM)
            if (res == null) {
                finish()
                return@launch
            }
            Intent(this@ApkSharing, MainActivity::class.java).run {
                val args = Bundle()
                args.putInt(AccessAV.EXTRA_LAUNCH_MODE, AccessAV.LAUNCH_MODE_LINK)
                args.putParcelable(AccessAV.EXTRA_DATA_STREAM, res)

                putExtras(MainActivity.forItem(Unit.UNIT_NAME_API_VIEWING, args))
                startActivity(this)
            }
            finish()
        }
    }
}
