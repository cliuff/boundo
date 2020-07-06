/*
 * Copyright 2020 Clifford Liu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.madness.collision.unit.school_timetable

import android.app.TimePickerDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.edit
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.madness.collision.R
import com.madness.collision.settings.SettingsFunc
import com.madness.collision.unit.Unit
import com.madness.collision.unit.school_timetable.data.Timetable
import com.madness.collision.util.P
import com.madness.collision.util.SystemUtil
import com.madness.collision.util.ThemeUtil
import com.madness.collision.util.alterPadding
import kotlinx.android.synthetic.main.tt_time.*
import java.text.SimpleDateFormat
import java.util.*
import com.madness.collision.unit.school_timetable.R as MyR

internal class TimeFragment: Unit() {

    override val id: String = "ST-Time"

    companion object {
        const val TIME_AM = 1
        const val TIME_PM = 2
        const val TIME_EVE = 3

        @JvmStatic
        fun newInstance() = TimeFragment()
    }

    private lateinit var iCalendarPreferences: SharedPreferences
    private val timeViewModel: TimeViewModel by viewModels()
    private lateinit var durationClassInferior: TextInputEditText
    private lateinit var durationRestInferior: TextInputEditText
    private lateinit var durationRestSuperior: TextInputEditText
    private lateinit var durationRestInferiorAm: TextInputEditText
    private lateinit var durationRestSuperiorAm: TextInputEditText
    private lateinit var durationRestInferiorPm: TextInputEditText
    private lateinit var durationRestSuperiorPm: TextInputEditText
    private lateinit var durationRestInferiorEve: TextInputEditText
    private lateinit var durationRestSuperiorEve: TextInputEditText

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        val menu = toolbar.menu
        toolbar.setTitle(MyR.string.ics_Button_date_picker)
        toolbar.inflateMenu(MyR.menu.toolbar_tt_time)
        menu.findItem(MyR.id.ttTimeTBDone).icon.setTint(ThemeUtil.getColor(context, R.attr.colorActionPass))
        menu.findItem(MyR.id.ttTimeTBRestore).icon.setTint(ThemeUtil.getColor(context, R.attr.colorActionAlert))
        return true
    }

    override fun selectOption(item: MenuItem): Boolean {
        when (item.itemId){
            MyR.id.ttTimeTBDone -> {
                actionDone()
                mainViewModel.popUpBackStack()
                return true
            }
            MyR.id.ttTimeTBRestore -> {
                actionReset()
                return true
            }
        }
        return false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val context = context
        if (context != null) SettingsFunc.updateLanguage(context)
        return inflater.inflate(MyR.layout.tt_time, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val context = context ?: return
        val views = view ?: return
        iCalendarPreferences = context.getSharedPreferences(P.PREF_TIMETABLE, Context.MODE_PRIVATE)
        democratize(mainViewModel)
        mainViewModel.contentWidthTop.observe(viewLifecycleOwner) {
            ttTimeContainer.alterPadding(top = it)
        }
        durationClassInferior = views.findViewById(MyR.id.ttTimeInputDurationClass)
        durationRestInferior = views.findViewById(MyR.id.ttTimeInputDurationBreakShort)
        durationRestSuperior = views.findViewById(MyR.id.ttTimeInputDurationBreakLong)
        durationRestInferiorAm = views.findViewById(MyR.id.ttTimeInputDurationBreakMorningShort)
        durationRestSuperiorAm = views.findViewById(MyR.id.ttTimeInputDurationBreakMorningLong)
        durationRestInferiorPm = views.findViewById(MyR.id.ttTimeInputDurationBreakAfternoonShort)
        durationRestSuperiorPm = views.findViewById(MyR.id.ttTimeInputDurationBreakAfternoonLong)
        durationRestInferiorEve = views.findViewById(MyR.id.ttTimeInputDurationBreakEveningShort)
        durationRestSuperiorEve = views.findViewById(MyR.id.ttTimeInputDurationBreakEveningLong)
        timeViewModel.timeDateStart.observe(viewLifecycleOwner) { formatDate() }
        timeViewModel.timeAm.observe(viewLifecycleOwner) { formatTime(TIME_AM) }
        timeViewModel.timePm.observe(viewLifecycleOwner) { formatTime(TIME_PM) }
        timeViewModel.timeEve.observe(viewLifecycleOwner) { formatTime(TIME_EVE) }
        val dateFormat = SimpleDateFormat("yyyyMMdd", SystemUtil.getLocaleApp())
        val dateFormat1 = SimpleDateFormat("yyyyMMdd HH", SystemUtil.getLocaleApp())
        views.findViewById<AppCompatAutoCompleteTextView>(MyR.id.ttTimePickDate).setOnFocusChangeListener { view, b ->
            if (!b) return@setOnFocusChangeListener
            // todo date
            val date = dateFormat1.parse(timeViewModel.timeDateStart.value!! + " 13")?.time ?: System.currentTimeMillis()
            val picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText(MyR.string.ics_Button_set_date).setSelection(date).build()
            picker.addOnPositiveButtonClickListener {
                val cal = Calendar.getInstance()
                cal.timeInMillis = it
                val re = dateFormat.format(cal.time)
                timeViewModel.timeDateStart.value = re
            }
            picker.show(childFragmentManager, this::class.java.simpleName)
            view.clearFocus()
        }
        views.findViewById<AppCompatAutoCompleteTextView>(MyR.id.ttTimePickTimeAm).setOnFocusChangeListener { view, b ->
            if (!b) return@setOnFocusChangeListener
            pickTime(TIME_AM)
            view.clearFocus()
        }
        views.findViewById<AppCompatAutoCompleteTextView>(MyR.id.ttTimePickTimePm).setOnFocusChangeListener { view, b ->
            if (!b) return@setOnFocusChangeListener
            pickTime(TIME_PM)
            view.clearFocus()
        }
        views.findViewById<AppCompatAutoCompleteTextView>(MyR.id.ttTimePickTimeEve).setOnFocusChangeListener { view, b ->
            if (!b) return@setOnFocusChangeListener
            pickTime(TIME_EVE)
            view.clearFocus()
        }

        loadTimeSettings()

        ttTimeInputDurationClass.nextFocusDownId = MyR.id.ttTimeInputDurationBreakShort
        ttTimeInputDurationBreakShort.nextFocusDownId = MyR.id.ttTimeInputDurationBreakLong
        ttTimeInputDurationBreakLong.imeOptions = EditorInfo.IME_ACTION_DONE
        ttTimeInputDurationBreakMorningShort.nextFocusDownId = MyR.id.ttTimeInputDurationBreakMorningLong
        ttTimeInputDurationBreakMorningLong.nextFocusDownId = MyR.id.ttTimeInputDurationBreakAfternoonShort
        ttTimeInputDurationBreakAfternoonShort.nextFocusDownId = MyR.id.ttTimeInputDurationBreakAfternoonLong
        ttTimeInputDurationBreakAfternoonLong.nextFocusDownId = MyR.id.ttTimeInputDurationBreakEveningShort
        ttTimeInputDurationBreakEveningShort.nextFocusDownId = MyR.id.ttTimeInputDurationBreakEveningLong
        ttTimeInputDurationBreakEveningLong.imeOptions = EditorInfo.IME_ACTION_DONE
    }

    private fun getTimeValue(timeMode: Int) = when(timeMode){
        TIME_AM -> timeViewModel.timeAm.value!!
        TIME_PM -> timeViewModel.timePm.value!!
        TIME_EVE -> timeViewModel.timeEve.value!!
        else -> ""
    }

    private fun getTimeBtn(timeMode: Int) = when(timeMode){
        TIME_AM -> ttTimePickTimeAm
        TIME_PM -> ttTimePickTimePm
        TIME_EVE -> ttTimePickTimeEve
        else -> ttTimePickTimeAm
    }

    private fun pickTime(timeMode: Int){
        val time = getTimeValue(timeMode)
        val (h, m) = getTimeDestructured(time)
        TimePickerDialog(context, { _, hourOfDay, minute ->
            val newTime = "${if (hourOfDay < 10) "0" else ""}$hourOfDay${if (minute < 10) "0" else ""}$minute"
            when(timeMode){
                TIME_AM -> timeViewModel.timeAm.value = newTime
                TIME_PM -> timeViewModel.timePm.value = newTime
                TIME_EVE -> timeViewModel.timeEve.value = newTime
            }
        }, h.toInt(), m.toInt(),true).show()
    }

    private fun loadTimeSettings(){
        timeViewModel.run {
            iCalendarPreferences.run {
                timeDateStart.value = getString(P.TT_DATE_START, P.TT_DATE_START_DEFAULT)!!
                timeAm.value = getString(P.TT_TIME_MORNING, P.TT_TIME_MORNING_DEFAULT)!!
                timePm.value = getString(P.TT_TIME_AFTERNOON, P.TT_TIME_AFTERNOON_DEFAULT)!!
                timeEve.value = getString(P.TT_TIME_EVENING, P.TT_TIME_EVENING_DEFAULT)!!
                durationClassInferior.setText(getInt(P.TT_TIME_CLASS, P.TT_TIME_CLASS_DEFAULT).toString())
                durationRestSuperior.setText(getInt(P.TT_TIME_BREAK_SUPERIOR, P.TT_TIME_BREAK_SUPERIOR_DEFAULT).toString())
                durationRestInferior.setText(getInt(P.TT_TIME_BREAK_INFERIOR, P.TT_TIME_BREAK_INFERIOR_DEFAULT).toString())
                durationRestInferiorAm.setText(getInt(P.TT_TIME_BREAK_MORNING_INFERIOR, -1).let {  if (it != -1) it.toString() else "" })
                durationRestSuperiorAm.setText(getInt(P.TT_TIME_BREAK_MORNING_SUPERIOR, -1).let {  if (it != -1) it.toString() else "" })
                durationRestInferiorPm.setText(getInt(P.TT_TIME_BREAK_AFTERNOON_INFERIOR, -1).let {  if (it != -1) it.toString() else "" })
                durationRestSuperiorPm.setText(getInt(P.TT_TIME_BREAK_AFTERNOON_SUPERIOR, -1).let {  if (it != -1) it.toString() else "" })
                durationRestInferiorEve.setText(getInt(P.TT_TIME_BREAK_EVENING_INFERIOR, -1).let {  if (it != -1) it.toString() else "" })
                durationRestSuperiorEve.setText(getInt(P.TT_TIME_BREAK_EVENING_SUPERIOR, -1).let {  if (it != -1) it.toString() else "" })
            }
        }
    }

    private fun formatDate(){
        val (y, m, d) = getDateDestructured()
        ttTimePickDate.setText(String.format("%s.%s.%s", y, m, d))
    }

    private fun formatTime(timeMode: Int){
        val time = getTimeValue(timeMode)
        val (h, m) = getTimeDestructured(time)
        getTimeBtn(timeMode).setText(String.format("%s:%s", h, m))
    }

    private fun getDateDestructured() = "(\\d{4})(\\d{2})(\\d{2})".toRegex().find(timeViewModel.timeDateStart.value!!)!!.destructured
    private fun getTimeDestructured(time: String) = "(\\d{2})(\\d{2})".toRegex().find(time)!!.destructured

    private fun actionReset(){
        iCalendarPreferences.edit {
            remove(P.TT_TIME_CLASS)
            remove(P.TT_TIME_BREAK_SUPERIOR)
            remove(P.TT_TIME_BREAK_INFERIOR)
            remove(P.TT_TIME_BREAK_MORNING_INFERIOR)
            remove(P.TT_TIME_BREAK_MORNING_SUPERIOR)
            remove(P.TT_TIME_BREAK_AFTERNOON_INFERIOR)
            remove(P.TT_TIME_BREAK_AFTERNOON_SUPERIOR)
            remove(P.TT_TIME_BREAK_EVENING_INFERIOR)
            remove(P.TT_TIME_BREAK_EVENING_SUPERIOR)
            remove(P.TT_DATE_START)
            remove(P.TT_TIME_MORNING)
            remove(P.TT_TIME_AFTERNOON)
            remove(P.TT_TIME_EVENING)
        }
        loadTimeSettings()
        updateConfig()
    }

    private val TextInputEditText.intValue : Int
        get() = text?.toString().let { if (it.isNullOrBlank()) -1 else it.toInt() }

    private fun actionDone(){
        iCalendarPreferences.edit {
            timeViewModel.run {
                putInt(P.TT_TIME_CLASS, durationClassInferior.intValue)
                putInt(P.TT_TIME_BREAK_INFERIOR, durationRestInferior.intValue)
                putInt(P.TT_TIME_BREAK_SUPERIOR, durationRestSuperior.intValue)
                if (durationRestInferiorAm.intValue == -1) remove(P.TT_TIME_BREAK_MORNING_INFERIOR)
                else
                    putInt(P.TT_TIME_BREAK_MORNING_INFERIOR, durationRestInferiorAm.intValue)
                if (durationRestSuperiorAm.intValue == -1) remove(P.TT_TIME_BREAK_MORNING_SUPERIOR)
                else
                    putInt(P.TT_TIME_BREAK_MORNING_SUPERIOR, durationRestSuperiorAm.intValue)
                if (durationRestInferiorPm.intValue == -1) remove(P.TT_TIME_BREAK_AFTERNOON_INFERIOR)
                else
                    putInt(P.TT_TIME_BREAK_AFTERNOON_INFERIOR, durationRestInferiorPm.intValue)
                if (durationRestSuperiorPm.intValue == -1) remove(P.TT_TIME_BREAK_AFTERNOON_SUPERIOR)
                else
                    putInt(P.TT_TIME_BREAK_AFTERNOON_SUPERIOR, durationRestSuperiorPm.intValue)
                if (durationRestInferiorEve.intValue == -1) remove(P.TT_TIME_BREAK_EVENING_INFERIOR)
                else
                    putInt(P.TT_TIME_BREAK_EVENING_INFERIOR, durationRestInferiorEve.intValue)
                if (durationRestSuperiorEve.intValue == -1) remove(P.TT_TIME_BREAK_EVENING_SUPERIOR)
                else
                    putInt(P.TT_TIME_BREAK_EVENING_SUPERIOR, durationRestSuperiorEve.intValue)
                putString(P.TT_DATE_START, timeDateStart.value)
                putString(P.TT_TIME_MORNING, timeAm.value)
                putString(P.TT_TIME_AFTERNOON, timePm.value)
                putString(P.TT_TIME_EVENING, timeEve.value)
            }
        }
        updateConfig()
    }

    private fun updateConfig(){
        val context = context ?: return
        if (Timetable.hasPersistence(context)){
            Timetable.fromPersistence(context).produceICal(context)
        }
    }
}