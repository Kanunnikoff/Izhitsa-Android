package software.kanunnikoff.izhitsa

import android.content.Context
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.util.AttributeSet
import android.view.inputmethod.InputMethodSubtype

class LatinKeyboardView : KeyboardView {

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        isPreviewEnabled = false
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        isPreviewEnabled = false
    }

    override fun onLongPress(key: Keyboard.Key): Boolean {
        return if (key.codes[0] == Keyboard.KEYCODE_CANCEL) {
            onKeyboardActionListener.onKey(KEYCODE_OPTIONS, null)
            true
        } else {
            super.onLongPress(key)
        }
    }

    fun setSubtypeOnSpaceKey(subtype: InputMethodSubtype) {
        invalidateAllKeys()
    }

    companion object {
        const val KEYCODE_OPTIONS = -100
        const val KEYCODE_LANGUAGE_SWITCH = -101
        const val KEYCODE_MODE_2_CHANGE = -102
        const val KEYCODE_MODE_3_CHANGE = -103
        const val KEYCODE_MODE_4_CHANGE = -104
    }
}
