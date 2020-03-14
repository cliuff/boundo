package com.madness.collision.versatile

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.madness.collision.main.MainActivity
import com.madness.collision.unit.Unit
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.M)
class TextProcessingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var flagAction = false
        var actionIntent: Intent? = null
        GlobalScope.launch {
            val extras = intent.extras
            if (extras == null){
                finish()
                return@launch
            }
            val text = extras.getCharSequence(Intent.EXTRA_PROCESS_TEXT) ?: ""
            val readOnly = extras.getBoolean(Intent.EXTRA_PROCESS_TEXT_READONLY, true)
            if (text.isEmpty()) {
                finish()
                return@launch
            }
            if (!readOnly){
                Intent().run {
                    putExtra(Intent.EXTRA_PROCESS_TEXT, text)
                    setResult(Activity.RESULT_OK, this)
                }
            }
            actionIntent = Intent(this@TextProcessingActivity, MainActivity::class.java)
            val args = Bundle()
            args.putString(Intent.EXTRA_TEXT, text.toString())
            actionIntent?.putExtras(MainActivity.forItem(Unit.UNIT_NAME_API_VIEWING, args))
            flagAction = true
        }.invokeOnCompletion {
            if (!flagAction) return@invokeOnCompletion
            startActivity(actionIntent)
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0){
            setResult(Activity.RESULT_OK)
            finish()
        }
    }
}
