package com.madness.collision.unit.we_chat_evo

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.madness.collision.R
import com.madness.collision.util.SystemUtil
import com.madness.collision.util.ThemeUtil
import com.madness.collision.util.X

class InstantWeChatActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.let {
            SystemUtil.applyEdge2Edge(it)
            SystemUtil.applyStatusBarColor(this, it, false, isTransparentBar = true)
            SystemUtil.applyNavBarColor(this, it, false, isTransparentBar = true)
        }
        setContentView(R.layout.activity_instant_wechat)
        val root: ConstraintLayout = findViewById(R.id.instantWeChatRoot)
        root.background = if (ThemeUtil.getIsNight(this)) ColorDrawable(ThemeUtil.getColor(this, R.attr.colorABackground))
        else getDrawable(R.drawable.ic_tencent_mm_back_a)
        val bar: ProgressBar = findViewById(R.id.instant_wechat_bar)
        bar.postDelayed({
            try {
                startActivity(packageManager.getLaunchIntentForPackage("com.tencent.mm"))
                if (X.belowOff(X.P)) overridePendingTransition(R.anim.res_fade_in, R.anim.res_fade_out)
                finish()
            }catch( e: Exception){
                X.toast(this, R.string.WeChatLauncher_Launch_Fail, Toast.LENGTH_SHORT)
            }
        }, 400)
    }
}
