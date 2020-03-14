package com.madness.collision.versatile

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.madness.collision.main.MainActivity
import com.madness.collision.unit.Unit
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ApiViewingSearchActivity : AppCompatActivity() {
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
            val text = extras.getCharSequence(Intent.EXTRA_TEXT) ?: ""
            if (text.isEmpty()) {
                finish()
                return@launch
            }
            actionIntent = Intent(this@ApiViewingSearchActivity, MainActivity::class.java)
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

}
