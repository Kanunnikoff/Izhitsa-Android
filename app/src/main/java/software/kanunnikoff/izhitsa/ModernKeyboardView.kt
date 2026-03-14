package software.kanunnikoff.izhitsa

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import androidx.core.content.ContextCompat
import java.util.Locale

class ModernKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    interface OnKeyPressListener {
        fun onKey(primaryCode: Int)
    }

    private val handler = Handler(Looper.getMainLooper())
    private var listener: OnKeyPressListener? = null
    private var layout: ImeKeyboardLayout? = null
    private var shifted = false

    init {
        orientation = VERTICAL
        setBackgroundColor(ContextCompat.getColor(context, R.color.background))
    }

    fun setOnKeyPressListener(listener: OnKeyPressListener?) {
        this.listener = listener
    }

    fun setKeyboardLayout(layout: ImeKeyboardLayout?) {
        this.layout = layout
        rebuildKeyboard()
    }

    fun setShifted(shifted: Boolean) {
        this.shifted = shifted
        updateLabels()
    }

    fun isShifted(): Boolean = shifted

    fun refreshKeys() {
        updateLabels()
    }

    private fun rebuildKeyboard() {
        removeAllViews()
        val currentLayout = layout ?: return

        val keyHeight = resources.getDimensionPixelSize(R.dimen.key_height)
        for (rowKeys in currentLayout.rows) {
            val row = LinearLayout(context).apply {
                orientation = HORIZONTAL
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, keyHeight)
                weightSum = rowKeys.sumOf { it.weight.toDouble() }.toFloat()
            }

            for (key in rowKeys) {
                row.addView(createKeyView(key, keyHeight))
            }

            addView(row)
        }
    }

    private fun createKeyView(key: ImeKey, keyHeight: Int): View {
        val params = LayoutParams(0, keyHeight, key.weight)

        if (key.iconRes != 0) {
            val button = ImageButton(context).apply {
                layoutParams = params
                setImageResource(key.iconRes)
                setBackgroundResource(R.drawable.key_background_modern)
                scaleType = ImageView.ScaleType.CENTER
                contentDescription = key.contentDescription
            }
            bindKeyActions(button, key)
            button.tag = key
            return button
        }

        val button = Button(context).apply {
            layoutParams = params
            isAllCaps = false
            text = resolveLabel(key)
            setTextColor(ContextCompat.getColor(context, R.color.keyTextColor))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.key_background_modern)
            contentDescription = key.contentDescription
        }
        bindKeyActions(button, key)
        button.tag = key
        return button
    }

    private fun bindKeyActions(view: View, key: ImeKey) {
        if (key.repeatable) {
            view.setOnTouchListener(RepeatKeyTouchListener(key.code))
        } else {
            view.setOnClickListener {
                listener?.onKey(key.code)
            }
        }
    }

    private fun updateLabels() {
        for (i in 0 until childCount) {
            val rowView = getChildAt(i) as? LinearLayout ?: continue
            for (j in 0 until rowView.childCount) {
                val keyView = rowView.getChildAt(j)
                val key = keyView.tag as? ImeKey ?: continue
                if (key.iconRes != 0) continue
                if (keyView is Button) {
                    keyView.text = resolveLabel(key)
                }
            }
        }
    }

    private fun resolveLabel(key: ImeKey): String {
        val base = key.label ?: ""
        if (!shifted) return base
        key.shiftLabel?.let { return it }
        return if (base.length == 1 && base[0].isLetter()) {
            base.uppercase(Locale.getDefault())
        } else {
            base
        }
    }

    private inner class RepeatKeyTouchListener(private val keyCode: Int) : OnTouchListener {
        private var repeating = false

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            return when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    repeating = true
                    listener?.onKey(keyCode)
                    handler.postDelayed(repeatRunnable, REPEAT_START_DELAY_MS)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    repeating = false
                    handler.removeCallbacks(repeatRunnable)
                    true
                }
                else -> false
            }
        }

        private val repeatRunnable = object : Runnable {
            override fun run() {
                if (!repeating) return
                listener?.onKey(keyCode)
                handler.postDelayed(this, REPEAT_INTERVAL_MS)
            }
        }
    }

    companion object {
        private const val REPEAT_START_DELAY_MS = 400L
        private const val REPEAT_INTERVAL_MS = 60L
    }
}
