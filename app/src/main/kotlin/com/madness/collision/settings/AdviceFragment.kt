package com.madness.collision.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.madness.collision.Democratic
import com.madness.collision.R
import com.madness.collision.main.MainViewModel
import com.madness.collision.util.P
import com.madness.collision.util.X
import com.madness.collision.util.listenedTimelyBy
import com.madness.collision.util.mainApplication

internal class AdviceFragment : Fragment(), Democratic, View.OnClickListener{
    private lateinit var background: View
    private var count4DebugMode = 0
    private val mainViewModel: MainViewModel by activityViewModels()

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        toolbar.setTitle(R.string.Main_TextView_Advice_Text)
        return true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val context = context
        if (context != null) SettingsFunc.updateLanguage(context)
        return inflater.inflate(R.layout.fragment_advice, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val view = view ?: return
        democratize(mainViewModel)

        background = view.findViewById(R.id.advice_display_background)

        val vLogo = view.findViewById<ImageView>(R.id.adviceLogo)
        (vLogo.drawable as AnimatedVectorDrawable).start()
        arrayOf(
                view.findViewById(R.id.adviceDerive),
                view.findViewById(R.id.adviceLicense),
                vLogo as View
        ).forEach { it.listenedTimelyBy(this) }
    }

    override fun onClick(view: View) {
        val context = context ?: return
        when (view.id) {
            R.id.adviceDerive ->{
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = clipboard.primaryClip
                if (clipData == null) {
                    X.toast(context, R.string.text_no_content, Toast.LENGTH_SHORT)
                    return
                }
                val builder = StringBuilder()
                for (i in 0 until clipData.itemCount) builder.append(clipData.getItemAt(i).htmlText)
                clipboard.setPrimaryClip(ClipData.newPlainText("text", builder.toString()))
                X.toast(context, R.string.text_done, Toast.LENGTH_SHORT)
            }
            R.id.adviceLicense -> {
                findNavController().navigate(AdviceFragmentDirections.actionAdviceFragmentToOssActivity())
            }
            R.id.adviceLogo -> {
                count4DebugMode++
                if (count4DebugMode != 6) return
                count4DebugMode = 0
                val pref = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
                mainApplication.debug = !mainApplication.debug
                pref.edit { putBoolean(P.ADVANCED, mainApplication.debug) }
                X.toast(context, getString(R.string.Advice_Switch_Debug_Text) + ": O" + if (mainApplication.debug) "n" else "ff", Toast.LENGTH_LONG)
            }
        }
    }
}