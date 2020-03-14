package com.madness.collision.util

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.Drawable
import android.text.InputType
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import com.google.android.material.button.MaterialButton
import com.madness.collision.R
import kotlin.math.max
import kotlin.math.roundToInt

class CollisionDialog private constructor(private val mContext: Context, private val type: Char) : Dialog(mContext) {
    companion object {
        private const val TYPE_POP_UP = 'a'
        private const val TYPE_PROGRESS_BAR = 'b'

        /**
         * as an alert dialog
         * one button to dismiss
         */
        fun alert(context: Context, textId: Int) = alert(context, context.getString(textId))

        fun alert(context: Context, text: CharSequence) = CollisionDialog(context, TYPE_POP_UP).apply {
            buttonLeft.visibility = View.GONE
            buttonRight.visibility = View.GONE
            initializeColor(buttonIndifferent)
            buttonIndifferent.setText(R.string.text_alright)
            setTitleCollision(0, 0, 0)
            setContent(text)
            setListener{ dismiss() }
        }

        /**
         * display the information as content with two buttons,
         * one to copy the content, the other to dismiss
         */
        fun infoCopyable(context: Context, information: CharSequence) = CollisionDialog(context, R.string.text_cancel, R.string.text_copy, true).apply {
            setContent(information)
            setTitleCollision(0, 0, 0)
            setListener(View.OnClickListener { dismiss() }, View.OnClickListener {
                X.copyText2Clipboard(context, information, R.string.text_copy_content)
                dismiss()
            })
        }

        /**
         * display progressbar with transparent background
         * @param context context
         * @param progressBar progressbar
         */
        fun loading(context: Context, progressBar: ProgressBar) = CollisionDialog(context, TYPE_PROGRESS_BAR).apply { setContentView(progressBar) }
    }

    lateinit var buttonLeft: MaterialButton
        private set
    lateinit var buttonRight: MaterialButton
        private set
    lateinit var buttonIndifferent: MaterialButton
        private set
    lateinit var title: AppCompatEditText
    private lateinit var scrollView: ScrollView
    private lateinit var container: LinearLayout
    private lateinit var content: AppCompatTextView
    private lateinit var parent: LinearLayout

    init {
        when (type) {
            TYPE_POP_UP -> {
                window?.setBackgroundDrawableResource(R.drawable.res_dialog_md2)
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setContentView(R.layout.dialog_collision)
                buttonLeft = findViewById(R.id.collision_dialog_left)
                buttonRight = findViewById(R.id.collision_dialog_right)
                buttonIndifferent = findViewById(R.id.collision_dialog_indifferent)
                title = findViewById(R.id.collision_dialog_title)
                scrollView = findViewById(R.id.collision_dialog_scroll)
                container = findViewById(R.id.collision_dialog_container)
                content = findViewById(R.id.collision_dialog_content)
                parent = findViewById(R.id.collision_dialog_parent)
            }
            TYPE_PROGRESS_BAR -> {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                window?.decorView?.background = null
            }
        }
    }

    /**
     * a dialog that has no button
     */
    constructor(context: Context) : this(context, TYPE_POP_UP) {
        noButtons()
    }

    /**
     * a dialog that has one button
     */
    constructor(context: Context, indifferentId: Int) : this(context, TYPE_POP_UP) {
        buttonLeft.visibility = View.GONE
        buttonRight.visibility = View.GONE
        initializeColor(buttonIndifferent)
        val indifferent = context.getString(indifferentId)
        buttonIndifferent.text = indifferent
    }

    /**
     * a dialog that has two buttons
     */
    constructor(context: Context, leftId: Int, rightId: Int, distinctive: Boolean) : this(context, TYPE_POP_UP) {
        val left = context.getString(leftId)
        val right = context.getString(rightId)
        buttonIndifferent.visibility = View.GONE
        buttonLeft.text = left
        buttonRight.text = right
        if (distinctive) {
            makePro(buttonRight)
            makeCon(buttonLeft)
        } else {
            initializeColor(buttonLeft)
            initializeColor(buttonRight)
        }
    }

    /**
     * a dialog that has three buttons
     */
    constructor(context: Context, leftId: Int, rightId: Int, indifferentId: Int, distinctive: Boolean) : this(context, leftId, rightId, distinctive) {
        val indifferent = context.getString(indifferentId)
        buttonIndifferent.visibility = View.VISIBLE
        buttonIndifferent.text = indifferent
        initializeColor(buttonIndifferent)
    }

    override fun show() {
        if (type == TYPE_POP_UP) {
            decentHeight()
            decentButtons()
        }
        super.show()
    }

    fun showIndecently() {
        super.show()
    }

    fun showAsContentHolder(width: Int, height: Int) {
        val rl = RelativeLayout(mContext)
        rl.minimumWidth = width
        rl.minimumHeight = height
        val bar = ProgressBar(mContext)
        val params = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
        params.addRule(RelativeLayout.CENTER_IN_PARENT)
        rl.addView(bar, params)
        setCustomContent(rl)
        super.show()
    }

    fun decentHeight() {
        parent.measure(shouldLimitHor = true)
        container.measure(shouldLimitHor = true)
        val totalContentHeight = parent.measuredHeight
        val extraHeight = totalContentHeight - container.measuredHeight
        val insetsHeight = mainApplication.insetTop + mainApplication.insetBottom
        val containableHeight = X.getCurrentAppResolution(mContext).y - insetsHeight
        val blankMargin = X.size(mContext, 70f, X.DP).roundToInt()
        val shouldResize = containableHeight - totalContentHeight < blankMargin
        val remainHeight = containableHeight - extraHeight - blankMargin
        if (remainHeight <= 0){
            clearCustomContent()
            noButtons()
            buttonIndifferent.visibility = View.VISIBLE
            initializeColor(buttonIndifferent)
            buttonIndifferent.setText(R.string.text_alright)
            setListener { dismiss() }
            setTitleCollision(0, 0, 0)
            setContent(R.string.text_error)
            return
        }
        val reHeight = if (shouldResize) remainHeight else ViewGroup.LayoutParams.WRAP_CONTENT
        (scrollView.layoutParams as LinearLayout.LayoutParams).run {
            width = container.measuredWidth
            height = reHeight
        }
    }

    fun setTitleCollision(title: CharSequence, hint: Int, type: Int) {
        val textView = this.title
        if (hint == 0 && type == 0) { //case text view
            textView.inputType = InputType.TYPE_NULL
            textView.isCursorVisible = false
            textView.keyListener = null
        } else { //case edit text
            if (type != 0)
                textView.inputType = type
            if (hint != 0)
                textView.setHint(hint)
            textView.setSelectAllOnFocus(true)
        }
        textView.setText(title)
        val params = textView.layoutParams as LinearLayout.LayoutParams
        val dp20 = X.size(mContext, 20f, X.DP).toInt()
        params.setMargins(dp20, dp20, dp20, 0)
    }

    fun setTitleCollision(titleId: Int, hint: Int, type: Int) {
        val textView = this.title
        if (titleId == 0 && hint == 0 && type == 0) { //make title invisible
            textView.visibility = View.GONE
            return
        } else if (titleId != 0 && hint == 0 && type == 0) { //case text view
            textView.inputType = InputType.TYPE_NULL
            textView.isCursorVisible = false
            textView.keyListener = null
        } else { //case edit text
            if (type != 0)
                textView.inputType = type
            if (hint != 0)
                textView.setHint(hint)
            textView.setSelectAllOnFocus(true)
        }
        if (titleId != 0) {
            val title = mContext.getString(titleId)
            textView.setText(title)
        }
        val params = textView.layoutParams as LinearLayout.LayoutParams
        val dp20 = X.size(mContext, 20f, X.DP).toInt()
        params.setMargins(dp20, dp20, dp20, 0)
    }

    fun setContent(contentId: Int) {
        if (contentId == 0) {
            content.visibility = View.GONE
            return
        }
        setContent(mContext.getString(contentId))
    }

    fun setContent(content: CharSequence) {
        val textView: AppCompatTextView = this.content
        textView.dartFuture(content)
        textView.visibility = View.VISIBLE
        val params = textView.layoutParams as LinearLayout.LayoutParams
        val dpDiez = X.size(mContext, 10f, X.DP).toInt()
        params.setMargins(dpDiez * 2, dpDiez, dpDiez * 2, 0)
    }

    fun setCustomContent(contentId: Int) {
        setCustomContent(LayoutInflater.from(mContext).inflate(contentId, null))
    }

    private fun clearCustomContent(){
        container.removeViews(1, container.childCount - 1)
    }

    fun setCustomContent(content: View) {
        clearCustomContent()
        container.addView(content)
        val textView: TextView = this.content
        if (textView.visibility == View.GONE && title.visibility == View.GONE) return
        val params = scrollView.layoutParams as LinearLayout.LayoutParams
        params.setMargins(0, X.size(mContext, 10f, X.DP).toInt(), 0, 0)
    }

    /**
     * set custom content without setting extra margins
     * @param contentId target view
     */
    fun setCustomContentMere(contentId: Int) {
        setCustomContentMere(LayoutInflater.from(mContext).inflate(contentId, null))
    }

    /**
     * set custom content without setting extra margins
     * @param content target view
     */
    fun setCustomContentMere(content: View) {
        clearCustomContent()
        container.addView(content)
    }

    fun setColorRes(superior: Int, inferior: Int) {
        val colorSuperior = X.getColor(mContext, superior)
        val colorInferior = X.getColor(mContext, inferior)
        if (buttonLeft.visibility == View.VISIBLE) {
            buttonLeft.setTextColor(colorSuperior)
            buttonLeft selectBackColor colorInferior
        }
        if (buttonRight.visibility == View.VISIBLE) {
            buttonRight.setTextColor(colorSuperior)
            buttonRight selectBackColor colorInferior
        }
        if (buttonIndifferent.visibility == View.VISIBLE) {
            buttonIndifferent.setTextColor(colorSuperior)
            buttonIndifferent selectBackColor colorInferior
        }
    }

    private fun initializeColor(button: Button) {
        val colorRes = TypedValue()
        mContext.theme.resolveAttribute(R.attr.colorAccent, colorRes, true)
        button.setTextColor(colorRes.data)
        mContext.theme.resolveAttribute(R.attr.colorAccentBack, colorRes, true)
        button selectBackColor colorRes.data
    }

    private infix fun Button.selectBackRes(colorRes: Int) = this selectBackColor X.getColor(context, colorRes)

    private infix fun Button.selectBackColor(color: Int) {
        this.background = RippleUtil.getSelectableDrawablePure(color, context.resources.getDimension(R.dimen.radius))
    }

    fun setBackColor(button: MaterialButton, colorRes: Int){
        button selectBackRes colorRes
    }

    fun makePro(button: MaterialButton){
        val color = ThemeUtil.getColor(mContext, R.attr.colorActionPass)
        button.setTextColor(color)
        button selectBackColor ThemeUtil.getBackColor(color, 0.2f)
    }

    fun makeCon(button: MaterialButton){
        initializeColor(button)
    }

    fun makeAgainst(button: MaterialButton){
        val colorRes = TypedValue()
        mContext.theme.resolveAttribute(R.attr.colorActionAlert, colorRes, true)
        button.setTextColor(colorRes.data)
        mContext.theme.resolveAttribute(R.attr.colorActionAlertBack, colorRes, true)
        button selectBackColor colorRes.data
    }

    inline fun setListener(crossinline indifferent: (view: View) -> Unit) {
        View.OnClickListener { indifferent(buttonIndifferent) }.listen2(buttonIndifferent)
    }

    inline fun setListener(crossinline left: (view: View) -> Unit, crossinline right: (view: View) -> Unit) {
        View.OnClickListener { left(buttonLeft) }.listen2(buttonLeft)
        View.OnClickListener { right(buttonRight) }.listen2(buttonRight)
    }

    inline fun setListener(crossinline left: (view: View) -> Unit, crossinline right: (view: View) -> Unit, crossinline indifferent: (view: View) -> Unit) {
        View.OnClickListener { left(buttonLeft) }.listen2(buttonLeft)
        View.OnClickListener { right(buttonRight) }.listen2(buttonRight)
        View.OnClickListener { indifferent(buttonIndifferent) }.listen2(buttonIndifferent)
    }

    fun setListener(indifferent: View.OnClickListener) = buttonIndifferent.listenedBy(indifferent)

    fun setListener(left: View.OnClickListener, right: View.OnClickListener) {
        buttonLeft.listenedBy(left)
        buttonRight.listenedBy(right)
    }

    fun setListener(left: View.OnClickListener, right: View.OnClickListener, indifferent: View.OnClickListener) {
        buttonLeft.listenedBy(left)
        buttonRight.listenedBy(right)
        buttonIndifferent.listenedBy(indifferent)
    }

    fun setListenerIndifferent(indifferent: View.OnClickListener): Unit = buttonIndifferent.listenedBy(indifferent)

    fun setListenerLeft(left: View.OnClickListener): Unit = buttonLeft.listenedBy(left)

    fun setListenerRight(right: View.OnClickListener): Unit = buttonRight.listenedBy(right)

    fun decentButtons() {
        val btn: LinearLayout = findViewById(R.id.collision_dialog_btn)
        btn.measure()
        val mw = X.size(context, 200f, X.DP).toInt()
        parent.minimumWidth = max(btn.measuredWidth, mw)
    }

    fun noButtons() {
        findViewById<LinearLayout>(R.id.collision_dialog_btn).visibility = View.GONE
        buttonIndifferent.visibility = View.GONE
    }

    fun scroll2Top() {
        scrollView.smoothScrollTo(0, 0)
    }

    fun setBackgroundTintRes(res: Int) {
        setBackgroundTint(X.getColor(mContext, res))
    }

    fun setBackgroundTint(color: Int) {
        var drawable: Drawable = mContext.getDrawable(R.drawable.res_dialog_md2) ?: return
        drawable = drawable.mutate()
        drawable.setTint(color)
        window?.setBackgroundDrawable(drawable)
    }
}
