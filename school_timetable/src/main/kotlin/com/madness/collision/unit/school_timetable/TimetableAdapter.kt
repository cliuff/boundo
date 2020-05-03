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

import android.content.Context
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.madness.collision.R
import com.madness.collision.unit.school_timetable.data.CourseSingleton
import com.madness.collision.unit.school_timetable.data.DayOfWeek
import com.madness.collision.unit.school_timetable.data.Repetition
import com.madness.collision.unit.school_timetable.data.Timetable
import com.madness.collision.unit.school_timetable.databinding.TtAdapterActionsBinding
import com.madness.collision.util.CollisionDialog
import com.madness.collision.util.ThemeUtil
import com.madness.collision.util.X
import com.madness.collision.util.dartFuture
import java.util.concurrent.atomic.AtomicBoolean
import com.madness.collision.unit.school_timetable.R as MyR

internal class TimetableAdapter(private var context: Context , var timetable: Timetable) : RecyclerView.Adapter<TimetableAdapter.TableViewHolder>() {

    class TableViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val card: MaterialCardView = view.findViewById(MyR.id.ttAdapterCard)
        val container: LinearLayout = view.findViewById(MyR.id.ttAdapterContainer)
        val space: Space = view.findViewById(MyR.id.ttAdapterSpace)
    }

    private val inflater = LayoutInflater.from(context)

    private val courses: MutableList<CourseSingleton>
        get() = timetable.courses
    private val alter = CollisionDialog(context, R.string.text_cancel, R.string.text_allset, true)
    init {
        alter.setTitleCollision(0, MyR.string.ics_alter_cname, InputType.TYPE_CLASS_TEXT)
        alter.setCustomContent(MyR.layout.dialog_tt_alter)
        alter.setContent(0)
    }
    private val etName: EditText = alter.title
    private val etTeacher: EditText = alter.findViewById(MyR.id.ics_alter_et_courseteacher)
    private val etRoom: EditText = alter.findViewById(MyR.id.ics_alter_et_courseroom)
    private val coursePeriodLL: LinearLayout = alter.findViewById(MyR.id.ics_alter_ll_courseperiod)
    private val radioWeek: RadioGroup = alter.findViewById(MyR.id.ics_alter_radio)
    private val spPeriod: Spinner = alter.findViewById(MyR.id.ics_alter_sp_classperiod)
    private val spPhase: Spinner = alter.findViewById(MyR.id.ics_alter_sp_classphase)
    private val focusDealer: View = alter.findViewById(MyR.id.ics_alter_focus_dealer)
    private val btnAltered: AtomicBoolean = AtomicBoolean(false)

    private val dp6 = X.size(context, 6f, X.DP).toInt()
    private val dp50 = X.size(context, 50f, X.DP).toInt()

    private val colorAm = ThemeUtil.getColor(context, R.attr.colorTtAm)
    private val colorPm = ThemeUtil.getColor(context, R.attr.colorTtPm)
    private val colorEve = ThemeUtil.getColor(context, R.attr.colorTtEve)
    private val colorAmInferior = getInferior(colorAm)
    private val colorPmInferior = getInferior(colorPm)
    private val colorEveInferior = getInferior(colorEve)
//    private val colorInferiorBlack = X.getColor(context, R.color.cardBackBlack)
    private val colorTTDark = ThemeUtil.getColor(context, R.attr.colorActionAlert)
    private val colorText = ThemeUtil.getColor(context, android.R.attr.textColor)

    private fun getInferior(color: Int): Int{
        return ThemeUtil.getBackColor(color, 0.4f)
    }

    init{
        val addNew: TextView = alter.findViewById(MyR.id.ttAlterAddPeriod)
        addNew.setOnClickListener { addRepetitionLayout(Repetition(0, 0)) }

        val clPeriods = arrayOf(
                context.getString(MyR.string.ics_alter_classlasting_1),
                context.getString(MyR.string.ics_alter_classlasting_2),
                context.getString(MyR.string.ics_alter_classlasting_3)
        )
        spPeriod.adapter = ArrayAdapter(context, R.layout.pop_list_item, clPeriods)

        val clPhases = arrayOf(
                context.getString(MyR.string.ics_alter_classtime_am1),
                context.getString(MyR.string.ics_alter_classtime_am2),
                context.getString(MyR.string.ics_alter_classtime_am3),
                context.getString(MyR.string.ics_alter_classtime_pm1),
                context.getString(MyR.string.ics_alter_classtime_pm2),
                context.getString(MyR.string.ics_alter_classtime_pm3),
                context.getString(MyR.string.ics_alter_classtime_eve1),
                context.getString(MyR.string.ics_alter_classtime_eve2),
                context.getString(MyR.string.ics_alter_classtime_eve3)
        )
        spPhase.adapter = ArrayAdapter(context, R.layout.pop_list_item, clPhases)
        spPhase.setPopupBackgroundResource(R.drawable.res_dialog_md2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TableViewHolder {
        return TableViewHolder(inflater.inflate(MyR.layout.adapter_tt, parent, false))
    }

    override fun onBindViewHolder(holder: TableViewHolder, position: Int) {
        holder.container.removeAllViews()
        var multipleView = false
        for (course in courses){
            if (course.legacyClassSchedule > position + 1) break
            if (course.legacyClassSchedule != position + 1) continue
            if (multipleView) {
                val blankView = Space(context)
                blankView.minimumHeight = dp6
                holder.container.addView(blankView)
            }
            multipleView = true

            val courseViews = inflater.inflate(MyR.layout.tt_adapter_course, holder.container, false)
            holder.container.addView(courseViews)
            courseViews.findViewById<AppCompatTextView>(MyR.id.ttAdapterCourseName).dartFuture(course.name)
            courseViews.findViewById<AppCompatTextView>(MyR.id.ttAdapterCoursePeriod).dartFuture(Repetition.toString(course.educating.repetitions))
            courseViews.findViewById<AppCompatTextView>(MyR.id.ttAdapterCourseRoom).dartFuture(course.educating.location)

            if(course.name == context.getString(MyR.string.ics_alter_cname) || course.legacyDuplicate) {
                courseViews.addOnAttachStateChangeListener(object: View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) {
                        v.removeOnAttachStateChangeListener(this)
                        v.performClick()
                    }
                    override fun onViewDetachedFromWindow(v: View) {}
                })
            }

            courseViews.setOnLongClickListener{ popActions(course) }
            courseViews.setOnClickListener { popCourseInfo(course) }
        }
        val dayPeri: Int = holder.adapterPosition / timetable.columns
        if (holder.container.childCount != 0) {
            holder.space.minimumHeight = 0
            when {
                dayPeri < timetable.rowAm -> colorAm
                dayPeri < timetable.rowAm + timetable.rowPm -> colorPm
                else -> colorEve
            }
        }else {
            holder.space.minimumHeight = dp50
            holder.card.cardElevation = 0f
            when {
//                isBlackTheme -> colorInferiorBlack
                dayPeri < timetable.rowAm -> colorAmInferior
                dayPeri < timetable.rowAm + timetable.rowPm -> colorPmInferior
                else -> colorEveInferior
            }
        }.let { holder.card.setCardBackgroundColor(it) }
    }

    private fun addRepetitionLayout(repetition: Repetition){

        inflater.inflate(MyR.layout.tt_adapter_periods, coursePeriodLL, false).apply {
            findViewById<AppCompatEditText>(MyR.id.ttAdapterPeriodsStart).setText(repetition.fromWeek.toString())
            findViewById<AppCompatEditText>(MyR.id.ttAdapterPeriodsEnd).setText(repetition.toWeek.toString())
            findViewById<Switch>(MyR.id.ttAdapterPeriodsFortnightly).isChecked = repetition.fortnightly
            findViewById<AppCompatImageButton>(MyR.id.ttAdapterPeriodsRemove).setOnClickListener{ coursePeriodLL.removeView(this) }
        }.let { coursePeriodLL.addView(it) }
    }

    /**
     * long click action
     */
    private fun popActions(course: CourseSingleton): Boolean {
        CollisionDialog(context, R.string.text_cancel).apply {
            setTitleCollision(course.name, 0, 0)
            setContent(0)
            val views = TtAdapterActionsBinding.inflate(layoutInflater)
            setCustomContent(views.root)
            setListener { dismiss() }

            views.ttAdapterActionsDuplicate.setOnClickListener {
                dismiss()
                actionDuplicate(course)
            }
            views.ttAdapterActionsRemove.setOnClickListener {
                dismiss()
                actionRemove(course)
            }
        }.show()
        return true
    }

    private fun actionDuplicate(course: CourseSingleton) {
        val newCourse: CourseSingleton = course.copy()
        newCourse.legacyDuplicate = true
        courses.add(0, newCourse)
        timetable.persist(context, true)
        timetable.renderTimetable()
        MyUnit.setTable(timetable)
    }

    private fun actionRemove(course: CourseSingleton) {
        courses.remove(course)
        timetable.persist(context, true)
        timetable.renderTimetable()
        timetable.produceICal(context)
        MyUnit.setTable(timetable)
    }

    private fun popCourseInfo(course: CourseSingleton){
        spPeriod.setSelection(course.legacyClassPeriod.toInt() - if (course.legacyTemplate == 'c') 96 else 97)
        when (course.legacyClassPhase){
            "11" -> 0; "12" -> 1; "13" -> 2; "21" -> 3; "22" -> 4
            "23" -> 5; "31" -> 6; "32" -> 7; "33" -> 8; else -> 0
        }.run { spPhase.setSelection(this) }

//                    alter.showIndecently()

        val listenerCancel = View.OnClickListener { alter.dismiss() }
        val listenerOk = View.OnClickListener {
            if (coursePeriodLL.childCount == 1)
                return@OnClickListener
            alter.dismiss()
            if (etName.text != null)
                course.name = etName.text.toString()

            if (etTeacher.text != null && etTeacher.text.toString().isNotEmpty())
                course.educating.educator = etTeacher.text.toString()
            else
                course.educating.educator = ""

            if (etRoom.text != null && etRoom.text.toString().isNotEmpty())
                course.educating.location = etRoom.text.toString()
            else
                course.educating.location = ""

            if (course.legacyTemplate == 'c')
                course.legacyClassPeriod = (spPeriod.selectedItemPosition + 96).toChar()
            else
                course.legacyClassPeriod = (spPeriod.selectedItemPosition + 97).toChar()

            val repetitions = ArrayList<Repetition>(coursePeriodLL.childCount - 1)
            for (c in 1 until coursePeriodLL.childCount){
                val scrollView: HorizontalScrollView = coursePeriodLL.getChildAt(c) as HorizontalScrollView
                val ll: LinearLayout = scrollView.getChildAt(0) as LinearLayout
                val etStart: EditText = ll.getChildAt(0) as EditText
                val etEnd: EditText = ll.getChildAt(2) as EditText
                val singlePeriodSwitch: Switch = ll.getChildAt(3) as Switch
                var fromWeek: Int
                var toWeek: Int
                fromWeek = if (etStart.text != null && etStart.text.toString().isNotEmpty())
                    Integer.parseInt(etStart.text.toString())
                else 1
                toWeek = if (etEnd.text != null && etEnd.text.toString().isNotEmpty())
                    Integer.parseInt(etEnd.text.toString())
                else 1
                repetitions.add(Repetition.parseFortnightly(fromWeek, toWeek, singlePeriodSwitch.isChecked))
            }
            course.educating.repetitions = repetitions.toTypedArray()

            when (radioWeek.checkedRadioButtonId){
                MyR.id.ics_alter_radio_mon -> DayOfWeek.MONDAY
                MyR.id.ics_alter_radio_tue -> DayOfWeek.TUESDAY
                MyR.id.ics_alter_radio_wed -> DayOfWeek.WEDNESDAY
                MyR.id.ics_alter_radio_thu -> DayOfWeek.THURSDAY
                MyR.id.ics_alter_radio_fri -> DayOfWeek.FRIDAY
                MyR.id.ics_alter_radio_sat -> DayOfWeek.SATURDAY
                MyR.id.ics_alter_radio_sun -> DayOfWeek.SUNDAY_LEGACY
                else -> DayOfWeek.MONDAY
            }.run { course.educating.dayOfWeek = this }
            when (spPhase.selectedItemPosition) {
                0 -> "11"; 1 -> "12"; 2 -> "13"; 3 -> "21"; 4 -> "22"
                5 -> "23"; 6 -> "31"; 7 -> "32"; 8 -> "33"; else -> "11"
            }.run { course.legacyClassPhase = this }
            course.renderUid()

            timetable.persist(context, true)
            timetable.renderTimetable()
            timetable.produceICal(context)
            MyUnit.setTable(timetable)
        }

        // below: when adding new course or duplicating course
        val color = if(course.legacyDuplicate) colorTTDark else colorText
        etName.setTextColor(color)
        etTeacher.setTextColor(color)
        etRoom.setTextColor(color)
        if(course.name == context.getString(MyR.string.ics_alter_cname) || course.legacyDuplicate){
            alter.setListener(View.OnClickListener { alter.dismiss(); actionRemove(course) }, listenerOk)
            btnAltered.set(true)
            // below: notify the user to change the course name when adding new course
            if (!course.legacyDuplicate) {
                X.toast(context, MyR.string.ttValidateName, Toast.LENGTH_LONG)
            }
            course.legacyDuplicate = false
        }else if (btnAltered.get()){
            alter.setListener(listenerCancel, listenerOk)
            btnAltered.set(false)
        }else{
            alter.setListener(listenerCancel, listenerOk)
        }
        focusDealer.requestFocus()
        etName.setText(course.name)
        etTeacher.setText(course.educating.educator)
        etRoom.setText(course.educating.location)
        coursePeriodLL.removeViews(1, coursePeriodLL.childCount - 1)
        course.educating.repetitions.forEach { addRepetitionLayout(it) }
        radioWeek.clearCheck()
        val button: RadioButton = radioWeek.getChildAt(course.educating.dayOfWeek - 1) as RadioButton
        button.isChecked = true

        alter.show()
        alter.scroll2Top()
    }

    override fun getItemCount(): Int = timetable.rows * timetable.columns
}
