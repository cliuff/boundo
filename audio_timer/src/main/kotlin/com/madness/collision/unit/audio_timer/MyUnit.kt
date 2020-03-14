package com.madness.collision.unit.audio_timer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.edit
import androidx.lifecycle.observe
import com.madness.collision.R
import com.madness.collision.unit.Unit
import com.madness.collision.unit.audio_timer.databinding.UnitAudioTimerBinding
import com.madness.collision.util.P
import com.madness.collision.util.X
import com.madness.collision.util.alterPadding
import kotlin.math.roundToInt

class MyUnit : Unit() {

    private var _viewBinding: UnitAudioTimerBinding? = null
    private val viewBinding: UnitAudioTimerBinding
        get() = _viewBinding!!

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        toolbar.setTitle(R.string.unit_audio_timer)
        return true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _viewBinding = UnitAudioTimerBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onDestroyView() {
        _viewBinding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = context ?: return
        val pref = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
        val timeHour = pref.getInt(P.AT_TIME_HOUR, 0)
        val timeMinute = pref.getInt(P.AT_TIME_MINUTE, 0)
        if (timeHour != 0) viewBinding.atHour.setText(timeHour.toString())
        if (timeMinute != 0) viewBinding.atMinute.setText(timeMinute.toString())
        updateStatus()
        viewBinding.atStart.setOnClickListener {
            val intent = Intent(context, AudioTimerService::class.java)
            // stop if running already
            if (AudioTimerService.isRunning) {
                context.stopService(intent)
                Handler().postDelayed(this::updateStatus, 500)
                return@setOnClickListener
            }
            val hourInput = viewBinding.atHour.text?.toString() ?: ""
            val targetHour = if (hourInput.isEmpty()) 0 else hourInput.toInt()
            val minuteInput = viewBinding.atMinute.text?.toString() ?: ""
            val targetMinute = if (minuteInput.isEmpty()) 0 else minuteInput.toInt()
            val targetDuration = (targetHour * 60 + targetMinute) * 60000L
            context.stopService(intent)
            intent.putExtra(AudioTimerService.ARG_DURATION, targetDuration)
            context.startService(intent)
            Handler().postDelayed(this::updateStatus, 500)
            val shouldUpdateHour = hourInput.isNotEmpty()
            val shouldUpdateMinute = minuteInput.isNotEmpty()
            pref.edit {
                if (shouldUpdateHour) putInt(P.AT_TIME_HOUR, targetHour)
                else remove(P.AT_TIME_HOUR)
                if (shouldUpdateMinute) putInt(P.AT_TIME_MINUTE, targetMinute)
                else remove(P.AT_TIME_MINUTE)
            }
        }
    }

    private fun updateStatus() {
        val icon = if (AudioTimerService.isRunning) R.drawable.ic_clear_24 else R.drawable.ic_arrow_forward_24
        viewBinding.atStart.setImageResource(icon)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        democratize()
        val context = context ?: return
        mainViewModel.contentWidthBottom.observe(viewLifecycleOwner) {
            val params = viewBinding.atStart.layoutParams as ConstraintLayout.LayoutParams
            params.bottomMargin = it + X.size(context, 10f,X.DP).roundToInt()
        }
        mainViewModel.contentWidthTop.observe(viewLifecycleOwner) {
            viewBinding.atContainer.alterPadding(top = it)
        }
    }
}