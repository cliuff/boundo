package com.madness.collision.wearable.main

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.wear.ambient.AmbientModeSupport
import com.madness.collision.wearable.R
import com.madness.collision.wearable.misc.MiscMain
import com.madness.collision.wearable.util.P
import com.madness.collision.wearable.util.mainApplication
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

internal class MainActivity : AppCompatActivity(), AmbientModeSupport.AmbientCallbackProvider {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        applyInsets()

        viewModel.ambient = object : AmbientModeSupport.AmbientCallback(){}
        // Enables Always-on
        viewModel.ambientController = AmbientModeSupport.attach(this)

        GlobalScope.launch {
            applyUpdates(this@MainActivity)
        }
    }

    /**
     * update notification availability, notification channels and check app update
     */
    private fun applyUpdates(context: Context){
        // enable notification
        mainApplication.notificationAvailable = NotificationManagerCompat.from(context).areNotificationsEnabled()
        MiscMain.clearCache(context)
        MiscMain.ensureUpdate(context, context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE))
    }

    private fun applyInsets() {
        val app = mainApplication
        mainContainer.setOnApplyWindowInsetsListener { _, insets ->
            app.insetBottom = insets.systemWindowInsetBottom
            viewModel.insetBottom.value = app.insetBottom
            insets
        }
    }

    override fun getAmbientCallback(): AmbientModeSupport.AmbientCallback {
        return object : AmbientModeSupport.AmbientCallback(){
            override fun onAmbientOffloadInvalidated() {
                viewModel.ambient.onAmbientOffloadInvalidated()
            }

            override fun onEnterAmbient(ambientDetails: Bundle?) {
                viewModel.ambient.onEnterAmbient(ambientDetails)
            }

            override fun onExitAmbient() {
                viewModel.ambient.onExitAmbient()
            }

            override fun onUpdateAmbient() {
                viewModel.ambient.onUpdateAmbient()
            }
        }
    }
}
