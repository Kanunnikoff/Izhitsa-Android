package software.kanunnikoff.izhitsa

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.os.Build
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager

class MyInputMethodService : InputMethodService(), KeyboardView.OnKeyboardActionListener {
    private var keyboardView: KeyboardView? = null
    private var keyboard: Keyboard? = null

    private var caps = false

    override fun onCreateInputView(): View? {
        keyboardView = layoutInflater.inflate(R.layout.keyboard_view, null) as KeyboardView
        keyboard = Keyboard(this, R.xml.russian_keys_layout)
        keyboardView?.keyboard = keyboard
        keyboardView?.setOnKeyboardActionListener(this)
        keyboardView?.isPreviewEnabled = false
        return keyboardView
    }

    override fun onPress(i: Int) {
    }

    override fun onRelease(i: Int) {
    }

    override fun onKey(primaryCode: Int, keyCodes: IntArray) {
        val inputConnection = currentInputConnection

        if (inputConnection != null) {
            when (primaryCode) {
                Keyboard.KEYCODE_DELETE -> {
                    val selectedText = inputConnection.getSelectedText(0)

                    if (TextUtils.isEmpty(selectedText)) {
                        inputConnection.deleteSurroundingText(1, 0)
                    } else {
                        inputConnection.commitText("", 1)
                    }
                }
                Keyboard.KEYCODE_SHIFT -> {
                    caps = !caps
                    keyboard?.isShifted = caps
                    keyboardView?.invalidateAllKeys()
                }
                Keyboard.KEYCODE_DONE -> inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                Keyboard.KEYCODE_MODE_CHANGE -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    switchToNextInputMethod(false)
                } else {
                    val imeManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

                    val token = window.window?.attributes?.token
                    imeManager.switchToNextInputMethod(token, false)
                }
                else -> {
                    var code = primaryCode.toChar()

                    if (Character.isLetter(code) && caps) {
                        code = Character.toUpperCase(code)
                    }

                    inputConnection.commitText(code.toString(), 1)
                }
            }
        }
    }

    override fun onText(charSequence: CharSequence) {
    }

    override fun swipeLeft() {
    }

    override fun swipeRight() {
    }

    override fun swipeDown() {
    }

    override fun swipeUp() {
    }
}