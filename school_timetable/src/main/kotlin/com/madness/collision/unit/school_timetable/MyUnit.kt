/*
 * Copyright 2021 Clifford Liu
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

import android.content.*
import android.os.Bundle
import android.provider.CalendarContract
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.content.edit
import com.madness.collision.R
import com.madness.collision.settings.SettingsFunc
import com.madness.collision.unit.Unit
import com.madness.collision.unit.school_timetable.calendar.ICal
import com.madness.collision.unit.school_timetable.calendar.getCalendar
import com.madness.collision.unit.school_timetable.calendar.getTime4Calendar
import com.madness.collision.unit.school_timetable.data.CourseSingleton
import com.madness.collision.unit.school_timetable.data.Timetable
import com.madness.collision.unit.school_timetable.databinding.UnitSchoolTimetableBinding
import com.madness.collision.unit.school_timetable.parser.Parser
import com.madness.collision.util.*
import com.madness.collision.util.MathUtils.boundMin
import com.madness.collision.util.controller.getSavedFragment
import com.madness.collision.util.controller.saveFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt
import com.madness.collision.unit.school_timetable.R as MyR

class MyUnit: Unit(), View.OnClickListener{

    override val id: String = "ST"

    companion object {
        const val STATE_KEY_Tt = "TtFragment"

        var ref: WeakReference<MyUnit>? = null

        fun setTable(timetable: Timetable?){
            val page = ref?.get() ?: return
            page.timetableFragment.setTimetable(timetable)
        }
    }

    private lateinit var settingsPreferences: SharedPreferences
    private lateinit var iCalendarPreferences: SharedPreferences
    private lateinit var timetableFragment: TimetableFragment

    private var isTimeInformed = false
    private lateinit var mTimetable: Timetable
    private lateinit var viewBinding: UnitSchoolTimetableBinding

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        toolbar.setTitle(R.string.unit_school_timetable)
        inflateAndTint(MyR.menu.toolbar_tt, toolbar, iconColor)
        return true
    }

    override fun selectOption(item: MenuItem): Boolean {
        when (item.itemId) {
            MyR.id.ttTBManual -> {
                mainViewModel.displayFragment(TTManualFragment.newInstance())
                return true
            }
            MyR.id.ttTBRemove -> {
                mTimetable = Timetable()
                setTable(mTimetable)
                val context = context ?: return false
                GlobalScope.launch {
                    if (Timetable.hasPersistence(context)) {
                        X.deleteFolder(Timetable.getPersistenceFolder(context))
                        val file = File(F.valFilePubTtCurrent(context))
                        if (file.exists()) file.delete()
                    }
                }
                return true
            }
        }
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ref = WeakReference(this)
        timetableFragment = childFragmentManager.getSavedFragment(savedInstanceState, STATE_KEY_Tt)
                ?: TimetableFragment.newInstance()
    }

    override fun onDestroy() {
        ref = null
        super.onDestroy()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = context
        if (context != null) SettingsFunc.updateLanguage(context)
        viewBinding = UnitSchoolTimetableBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ensureAdded(MyR.id.stTimetableContainer, timetableFragment, true)
        val context = context ?: return
        GlobalScope.launch {
            mTimetable = Timetable.fromPersistence(context)
            launch(Dispatchers.Main){
                setTable(mTimetable)
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val context = context ?: return
        iCalendarPreferences = context.getSharedPreferences(P.PREF_TIMETABLE, Context.MODE_PRIVATE)
        settingsPreferences = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
        democratize()
        mainViewModel.contentWidthTop.observe(viewLifecycleOwner) {
            viewBinding.ttContainer.alterPadding(top = it)
        }
        val minMargin = X.size(context, 80f, X.DP).roundToInt()
        val gapMargin = X.size(context, 30f, X.DP).roundToInt()
        mainViewModel.contentWidthBottom.observe(viewLifecycleOwner) {
            val margin = it + gapMargin
            viewBinding.ttImport.alterMargin(bottom = margin.boundMin(minMargin))
            // update view
            viewBinding.ttImport.requestLayout()
        }

        if (settingsPreferences.getBoolean(P.TT_CAL_DEFAULT_GOOGLE, true))
            viewBinding.ttCalendarDefault.isChecked = true
        viewBinding.ttCalendarDefault.setOnCheckedChangeListener{ _, isChecked ->
            settingsPreferences.edit { putBoolean(P.TT_CAL_DEFAULT_GOOGLE, isChecked) }
        }

        if (iCalendarPreferences.getBoolean(P.TT_APP_MODE, true))
            viewBinding.ttAppMode.isChecked = true
        viewBinding.ttAppMode.setOnCheckedChangeListener { _, isChecked ->
            iCalendarPreferences.edit { putBoolean(P.TT_APP_MODE, isChecked) }
            mTimetable.produceICal(context)
        }

        val v: Array<View> = arrayOf(viewBinding.ttTime, viewBinding.ttImport, viewBinding.ttExport,
                viewBinding.ttWeekIndicator, viewBinding.ttCodeHtml)
        v.forEach {
            it.setOnClickListener(this)
        }
        if (settingsPreferences.getBoolean(P.TT_MANUAL, true)) {
            mainViewModel.displayFragment(TTManualFragment.newInstance())
            settingsPreferences.edit { putBoolean(P.TT_MANUAL, false) }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        childFragmentManager.saveFragment(outState, STATE_KEY_Tt, timetableFragment)
        super.onSaveInstanceState(outState)
    }

    private fun openIcsFile(context: Context, file: File){
        try{
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            val type = "text/calendar"
            val flags: Int = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            intent.flags = flags
            intent.setDataAndType(file.getProviderUri(context), type)
            if (settingsPreferences.getBoolean(P.TT_CAL_DEFAULT_GOOGLE, true)) {
                intent.component = ComponentName("com.google.android.calendar", "com.google.android.calendar.ICalLauncher")
            }
            context.startActivity(intent)
        } catch ( e: ActivityNotFoundException) {
            e.printStackTrace()
            notifyBriefly(MyR.string.ics_Toast_open_fail)
        }
    }

    override fun onClick(view: View) {
        val context = context ?: return
        when(view.id) {
            MyR.id.ttExport -> {
                val popExport = CollisionDialog(context, R.string.text_cancel).apply {
                    setTitleCollision(MyR.string.ttExport, 0, 0)
                    setContent(0)
                    setCustomContent(MyR.layout.tt_pop_export)
                    setListener { dismiss() }
                    show()
                }
                val dateStart = iCalendarPreferences.getString(P.TT_DATE_START, P.TT_DATE_START_DEFAULT)
                // button export to calendar
                popExport.findViewById<View>(MyR.id.ttExportCal).setOnClickListener {
                    if (!isTimeInformed && dateStart == P.TT_DATE_START_DEFAULT){
                        popExport.dismiss()
                        notify(MyR.string.ttTimeNotice)
                        isTimeInformed = true
                    }else{
                        popExport.dismiss()
                        export2Cal()
                    }
                }
                // button export to iCalendar file
                popExport.findViewById<View>(MyR.id.ttExportFile).setOnClickListener {
                    if (!isTimeInformed && dateStart == P.TT_DATE_START_DEFAULT){
                        popExport.dismiss()
                        notify(MyR.string.ttTimeNotice)
                        isTimeInformed = true
                    }else{
                        popExport.dismiss()
                        export2ICalFile()
                    }
                }
                // button export to iCalendar file
                popExport.findViewById<View>(MyR.id.ttExportUndo).setOnClickListener {
                    popExport.dismiss()
                    undoImports()
                }
            }
            MyR.id.ttImport -> {
                val dialog = CollisionDialog(context, R.string.text_cancel).apply {
                    setTitleCollision(0, 0, 0)
                    setContent(0)
                    setCustomContent(MyR.layout.tt_pop_import)
                    setListener { dismiss() }
                    show()
                }
                dialog.findViewById<View>(MyR.id.ttImportParse).setOnClickListener {
                    dialog.dismiss()
                    val parser = Parser(context)
                    if (parser.parseFromClipboard()) {
                        mTimetable = parser.timetable
                    }
                }
                dialog.findViewById<View>(MyR.id.ttImportNew).setOnClickListener {
                    dialog.dismiss()
                    GlobalScope.launch {
                        val course = CourseSingleton()
                        course.name = getString(MyR.string.ics_alter_cname)
                        course.legacyClassPhase = "11"
                        course.legacyClassPeriod = 'b'
                        course.legacyTemplate = 'a'
                        mTimetable.courses.add(0, course)
                        mTimetable.persist(context, true)
                        mTimetable.renderTimetable()
                        launch(Dispatchers.Main){
                            setTable(mTimetable)
                        }
                    }
                }
            }
            MyR.id.ttWeekIndicator -> {
                val popExport = CollisionDialog(context, R.string.text_cancel).apply {
                    setTitleCollision(0, R.string.ttWeekIndicatorHint, InputType.TYPE_CLASS_NUMBER)
                    setContent(0)
                    setCustomContent(MyR.layout.tt_pop_export)
                    setListener { dismiss() }
                    show()
                }
                // button export to calendar
                popExport.findViewById<View>(MyR.id.ttExportCal).setOnClickListener {
                    var n = popExport.title.text?.toString() ?: "20"
                    if (n.isEmpty()) n = "20"
                    popExport.dismiss()
                    Timetable.produceWeekIndicator(context, n.toInt())
                    exportIndicator2Cal()
                }
                // button export to iCalendar file
                popExport.findViewById<View>(MyR.id.ttExportFile).setOnClickListener {
                    popExport.dismiss()
                    exportIndicator2ICalFile()
                }
                // button export to iCalendar file
                popExport.findViewById<View>(MyR.id.ttExportUndo).setOnClickListener {
                    popExport.dismiss()
                    undoIndicatorImports()
                }
            }
            MyR.id.ttTime -> mainViewModel.displayFragment(TimeFragment.newInstance())
            MyR.id.ttCodeHtml -> {
                val file = File(F.valFilePubTtCode(context))
                if (file.exists()) {
                    FilePop.by(context, file, "text/html", "", imageLabel = "School Timetable Web")
                            .show(childFragmentManager, FilePop.TAG)
                } else {
                    notify(R.string.text_no_content)
                }
            }
        }
    }

    private fun undoIndicatorImports(){
        val context = context ?: return
        val file = File(F.valFilePubTtIndicator(context))
        if (!file.exists()){
            notifyBriefly(R.string.text_error)
            return
        }
        val filesPath = F.valCachePubTtUndo(context)
        val folder = File(filesPath)
        F.prepareDir(folder)
        folder.deleteRecursively()
        folder.mkdirs()
        undoByPause(file, filesPath)
    }

    private fun undoImports(){
        val context = context ?: return
        val file = File(F.valFilePubTtPrevious(context))
        if (!file.exists()){
            notifyBriefly(MyR.string.ics_Import_Toast_Null)
            return
        }
        val filesPath = F.valCachePubTtUndo(context)
        val folder = File(filesPath)
        F.prepareDir(folder)
        folder.deleteRecursively()
        folder.mkdirs()
        undoByPause(file, filesPath)
    }

    private fun undoByPause(previousICal: File, undoCacheDir: String){
        val r = FileReader(previousICal)
        val content = r.readText()
        r.close()
        val undoList: MutableList<String> = ArrayList()
        val regex = "^BEGIN:VEVENT$(?<body>.+?)^END:VEVENT$".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE))
        for (re in regex.findAll(content)) undoList.add(ICal.getUndo(re.groups[1]?.value ?: ""))
        val undoIterator = undoList.iterator().iterator()

        val context = context ?: return
        val popContinue = CollisionDialog(context, R.string.text_cancel, MyR.string.ics_Undo_AlertDialog_Button, true)
        popContinue.setTitleCollision(0, 0, 0)
        popContinue.setContent(MyR.string.ics_Undo_AlertDialog_Message)
        popContinue.setListener({ popContinue.dismiss() }, { undoBySegment(undoIterator, undoCacheDir, popContinue) })
        popContinue.show()
        undoBySegment(undoIterator, undoCacheDir, popContinue)
    }

    private fun undoBySegment(undoIterator: Iterator<String>, undoCacheDir: String, pop: CollisionDialog){
        if(!undoIterator.hasNext()) {
            pop.dismiss()
            notifyBriefly(MyR.string.ics_Undo_Toast_Nailed)
            return
        }
        val path = F.createPath(undoCacheDir, "${Calendar.getInstance().time.time}.ics")
        val newOutPutFile: Formatter
        try {
            newOutPutFile = Formatter(path)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            val context = context ?: return
            X.toast(context, "file $path got lost", Toast.LENGTH_LONG)
            return
        }
        newOutPutFile.format(undoIterator.next())
        newOutPutFile.close()
        val context = context ?: return
        openIcsFile(context, File(path))
    }

    private fun export2ICalFile(){
        val context = context ?: return
        val iCalFile = File(F.valFilePubTtCurrent(context))
        if (iCalFile.exists()) {
            FilePop.by(context, iCalFile, "text/calendar", "", imageLabel = "School Timetable")
                    .show(childFragmentManager, FilePop.TAG)
        } else {
            notify(MyR.string.ics_Import_Toast_Null)
        }
    }

    private fun exportIndicator2ICalFile(){
        val context = context ?: return
        val ultimatePath = F.valFilePubTtIndicator(context)
        if (ultimatePath.isEmpty()) {
            notifyBriefly(R.string.text_error)
            return
        }
        val iCalFile = File(ultimatePath)
        if (iCalFile.exists()) {
            FilePop.by(context, iCalFile, "text/calendar", "", imageLabel = "School Week Indicator")
                    .show(childFragmentManager, FilePop.TAG)
        } else {
            notifyBriefly(R.string.text_no_content)
        }
    }

    private fun export2Cal(){
        //todo export as calendar events
        val context = context ?: return
        val file = File(F.valFilePubTtCurrent(context))
        if (file.exists()) {
            openIcsFile(context, file)
            val previousFile = File(F.valFilePubTtPrevious(context))
            if (!F.prepare4(previousFile)) return
            try { X.copyFileLessTwoGB(file, previousFile)
            } catch ( e: IOException) { e.printStackTrace() }
        } else {
            notify(MyR.string.ics_Import_Toast_Null)
        }
    }

    private fun exportIndicator2Cal(){
        val context = context ?: return
        val file = File(F.valFilePubTtIndicator(context))
        if (file.exists()) {
            openIcsFile(context, file)
        } else {
            notifyBriefly(R.string.text_error)
        }
    }

    @Suppress("unused")
    private fun getCalendars(){
        val context = context ?: return
        val dialog = CollisionDialog.alert(context, 0)
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        for (calendar in getCalendar(context)){
            val button = Button(context)
            button.textSize = 18f
            button.isAllCaps = false
            (button.layoutParams as ViewGroup.LayoutParams).run {
                width = ViewGroup.LayoutParams.MATCH_PARENT
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
            button.text = calendar.name
            val colorPro = ThemeUtil.getColor(context, R.attr.colorActionPass)
            button.setTextColor(colorPro)
            button.background = RippleUtil.getSelectableDrawablePure(
                    ThemeUtil.getBackColor(colorPro, 0.2f),
                    X.size(context, 5f, X.DP)
            )
            button.setOnClickListener {
                dialog.dismiss()
                var event: ContentValues
                var baseEvent: ContentValues
                for (co in mTimetable.courses){
                    baseEvent = ContentValues()
                    baseEvent.put(CalendarContract.Events.TITLE, co.name)
                    baseEvent.put(CalendarContract.Events.EVENT_LOCATION, co.educating.location)
                    baseEvent.put(CalendarContract.Events.DESCRIPTION, co.educating.educator)
                    baseEvent.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                    baseEvent.put(CalendarContract.Events.CALENDAR_ID, calendar.id)
                    for (t4c in getTime4Calendar(context, co)){
                        Log.d("TimetableActivity", "t4c.time:" + t4c.time[0])
                        Log.d("TimetableActivity", "t4c.time:" + t4c.time[1])
                        Log.d("TimetableActivity", "t4c.time:" + t4c.time[2])
                        Log.d("TimetableActivity", t4c.week)
                        event = ContentValues(baseEvent)
                        event.put(CalendarContract.Events._ID, co.legacyUid + t4c.week)// todo correct uid
                        event.put(CalendarContract.Events.DTSTART, t4c.time[0])
                        event.put(CalendarContract.Events.DTEND, t4c.time[1])
                        event.put(CalendarContract.Events.EXDATE, t4c.time[2])
                        if (t4c.single)
                            event.put(CalendarContract.Events.RRULE, "FREQ=WEEKLY;INTERVAL=2")
                        else
                            event.put(CalendarContract.Events.RRULE, "FREQ=WEEKLY")
                        //val url: Uri? = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, event)
                    }
                }
                notifyBriefly(R.string.text_done)
            }
            layout.addView(button)
        }
        if (layout.childCount == 0)
            dialog.setContent(R.string.text_no_content)
        else
            dialog.setCustomContent(layout)
        dialog.show()
    }
}