package com.madness.collision.unit.school_timetable

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.madness.collision.unit.school_timetable.data.Timetable
import kotlinx.android.synthetic.main.st_timetable.*

internal class TimetableFragment : Fragment() {

    companion object {

        fun newInstance(): TimetableFragment {
            return TimetableFragment()
        }
    }

    private lateinit var mContext: Context
    private lateinit var mAdapter: TimetableAdapter
    private var timetableToLoad: Timetable? = null

    fun setTimetable(timetable: Timetable?) {
        if (stTimetableRecycler == null) {
            timetableToLoad = timetable
            return
        }
        mAdapter.timetable = timetable ?: Timetable()
        val lm = stTimetableRecycler.layoutManager as GridLayoutManager?
        if (timetable != null && lm?.spanCount != timetable.columns && timetable.columns > 0) {
            stTimetableRecycler.layoutManager = GridLayoutManager(context, timetable.columns)
        }
        mAdapter.notifyDataSetChanged()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = context ?: return
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.st_timetable, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // initializing mAdapter in onCreate causes inflating error when rotating device
        mAdapter = TimetableAdapter(mContext, Timetable())
        stTimetableRecycler.adapter = mAdapter
        if (timetableToLoad != null) {
            setTimetable(timetableToLoad)
            timetableToLoad = null
        }
    }

}
