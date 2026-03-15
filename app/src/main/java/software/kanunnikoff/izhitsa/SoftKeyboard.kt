package software.kanunnikoff.izhitsa

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.os.IBinder
import android.text.InputType
import android.text.method.MetaKeyKeyListener
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.CompletionInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import java.util.*

/**
 * Example of writing an input method for a soft keyboard.
 */
class SoftKeyboard : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private var mInputMethodManager: InputMethodManager? = null
    private var mInputView: LatinKeyboardView? = null
    private var mCandidateView: CandidateView? = null
    private var mCompletions: Array<CompletionInfo>? = null

    private val mComposing = StringBuilder()
    private var mPredictionOn = false
    private var mCompletionOn = false
    private var mLastDisplayWidth = 0
    private var mCapsLock = false
    private var mLastShiftTime: Long = 0
    private var mMetaState: Long = 0

    private var russianKeyboard: LatinKeyboard? = null
    private var englishKeyboard: LatinKeyboard? = null
    private var mSymbolsKeyboard: LatinKeyboard? = null
    private var mSymbolsShiftedKeyboard: LatinKeyboard? = null
    private var digitsKeyboard: LatinKeyboard? = null

    private var mCurKeyboard: LatinKeyboard? = null

    private var mWordSeparators: String? = null

    override fun onCreate() {
        super.onCreate()
        mInputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        mWordSeparators = resources.getString(R.string.word_separators)
    }

    private val displayContext: Context
        get() {
            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            return createDisplayContext(wm.defaultDisplay)
        }

    override fun onInitializeInterface() {
        val displayContext = displayContext
        if (russianKeyboard != null) {
            val displayWidth = maxWidth
            if (displayWidth == mLastDisplayWidth) return
            mLastDisplayWidth = displayWidth
        }
        russianKeyboard = LatinKeyboard(displayContext, R.xml.russian_keys_layout)
        englishKeyboard = LatinKeyboard(displayContext, R.xml.english_keys_layout)
        mSymbolsKeyboard = LatinKeyboard(displayContext, R.xml.symbols_keys_layout)
        mSymbolsShiftedKeyboard = LatinKeyboard(displayContext, R.xml.symbols_shift_keys_layout)
        digitsKeyboard = LatinKeyboard(displayContext, R.xml.digits_keys_layout)
    }

    override fun onCreateInputView(): View {
        mInputView = layoutInflater.inflate(R.layout.input, null) as LatinKeyboardView
        mInputView?.setOnKeyboardActionListener(this)
        setLatinKeyboard(russianKeyboard)
        return mInputView!!
    }

    private fun setLatinKeyboard(nextKeyboard: LatinKeyboard?) {
        mInputView?.keyboard = nextKeyboard
    }

    override fun onCreateCandidatesView(): View {
        mCandidateView = CandidateView(displayContext)
        mCandidateView?.setService(this)
        return mCandidateView!!
    }

    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInput(attribute, restarting)

        mComposing.setLength(0)
        updateCandidates()

        if (!restarting) {
            mMetaState = 0
        }

        mPredictionOn = false
        mCompletionOn = false
        mCompletions = null

        when (attribute.inputType and InputType.TYPE_MASK_CLASS) {
            InputType.TYPE_CLASS_NUMBER, InputType.TYPE_CLASS_DATETIME, InputType.TYPE_CLASS_PHONE -> {
                mCurKeyboard = digitsKeyboard
            }
            InputType.TYPE_CLASS_TEXT -> {
                mCurKeyboard = russianKeyboard
                mPredictionOn = true

                val variation = attribute.inputType and InputType.TYPE_MASK_VARIATION
                if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                    variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                ) {
                    mPredictionOn = false
                }

                if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
                    variation == InputType.TYPE_TEXT_VARIATION_URI ||
                    variation == InputType.TYPE_TEXT_VARIATION_FILTER
                ) {
                    mPredictionOn = false
                }

                if ((attribute.inputType and InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                    mPredictionOn = false
                    mCompletionOn = isFullscreenMode
                }

                updateShiftKeyState(attribute)
            }
            else -> {
                mCurKeyboard = englishKeyboard
                updateShiftKeyState(attribute)
            }
        }

        mCurKeyboard?.setImeOptions(resources, attribute.imeOptions)
    }

    override fun onFinishInput() {
        super.onFinishInput()

        mComposing.setLength(0)
        updateCandidates()
        setCandidatesViewShown(false)

        mCurKeyboard = russianKeyboard
        mInputView?.closing()
    }

    override fun onStartInputView(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInputView(attribute, restarting)
        setLatinKeyboard(mCurKeyboard)
        mInputView?.closing()
        val subtype = mInputMethodManager?.currentInputMethodSubtype
        subtype?.let { mInputView?.setSubtypeOnSpaceKey(it) }
    }

    override fun onCurrentInputMethodSubtypeChanged(subtype: android.view.inputmethod.InputMethodSubtype?) {
        subtype?.let { mInputView?.setSubtypeOnSpaceKey(it) }
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int,
        newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int
    ) {
        super.onUpdateSelection(
            oldSelStart, oldSelEnd, newSelStart, newSelEnd,
            candidatesStart, candidatesEnd
        )

        if (mComposing.isNotEmpty() && (newSelStart != candidatesEnd || newSelEnd != candidatesEnd)) {
            mComposing.setLength(0)
            updateCandidates()
            currentInputConnection?.finishComposingText()
        }
    }

    override fun onDisplayCompletions(completions: Array<CompletionInfo>?) {
        if (mCompletionOn) {
            mCompletions = completions
            if (completions == null) {
                setSuggestions(null, completions = false, typedWordValid = false)
                return
            }

            val stringList = completions.mapNotNull { it.text?.toString() }
            setSuggestions(stringList, completions = true, typedWordValid = true)
        }
    }

    private fun translateKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        mMetaState = MetaKeyKeyListener.handleKeyDown(mMetaState, keyCode, event)
        var c = event.getUnicodeChar(MetaKeyKeyListener.getMetaState(mMetaState))
        mMetaState = MetaKeyKeyListener.adjustMetaAfterKeypress(mMetaState)
        val ic = currentInputConnection
        if (c == 0 || ic == null) return false

        if (c and android.view.KeyCharacterMap.COMBINING_ACCENT != 0) {
            c = c and android.view.KeyCharacterMap.COMBINING_ACCENT_MASK
        }

        if (mComposing.isNotEmpty()) {
            val accent = mComposing[mComposing.length - 1]
            val composed = KeyEvent.getDeadChar(accent.toInt(), c)
            if (composed != 0) {
                c = composed
                mComposing.setLength(mComposing.length - 1)
            }
        }

        onKey(c, null)
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                if (event.repeatCount == 0 && mInputView != null) {
                    if (mInputView!!.handleBack()) return true
                }
            }
            KeyEvent.KEYCODE_DEL -> {
                if (mComposing.isNotEmpty()) {
                    onKey(Keyboard.KEYCODE_DELETE, null)
                    return true
                }
            }
            KeyEvent.KEYCODE_ENTER -> return false
            else -> if (PROCESS_HARD_KEYS) {
                if (keyCode == KeyEvent.KEYCODE_SPACE && (event.metaState and KeyEvent.META_ALT_ON) != 0) {
                    val ic = currentInputConnection
                    if (ic != null) {
                        ic.clearMetaKeyStates(KeyEvent.META_ALT_ON)
                        "android".forEach { keyDownUp(getRawKeyCodeForChar(it.toInt())) }
                        return true
                    }
                }
                if (mPredictionOn && translateKeyDown(keyCode, event)) return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun getRawKeyCodeForChar(c: Int): Int {
        return when (c.toChar()) {
            'a' -> KeyEvent.KEYCODE_A
            'n' -> KeyEvent.KEYCODE_N
            'd' -> KeyEvent.KEYCODE_D
            'r' -> KeyEvent.KEYCODE_R
            'o' -> KeyEvent.KEYCODE_O
            'i' -> KeyEvent.KEYCODE_I
            else -> 0
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (PROCESS_HARD_KEYS && mPredictionOn) {
            mMetaState = MetaKeyKeyListener.handleKeyUp(mMetaState, keyCode, event)
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun commitTyped(inputConnection: InputConnection) {
        if (mComposing.isNotEmpty()) {
            inputConnection.commitText(mComposing, mComposing.length)
            mComposing.setLength(0)
            updateCandidates()
        }
    }

    private fun updateShiftKeyState(attr: EditorInfo?) {
        if (attr != null && mInputView != null && (englishKeyboard == mInputView?.keyboard || russianKeyboard == mInputView?.keyboard)) {
            var caps = 0
            val ei = currentInputEditorInfo
            if (ei != null && ei.inputType != InputType.TYPE_NULL) {
                caps = currentInputConnection.getCursorCapsMode(attr.inputType)
            }
            mInputView?.isShifted = mCapsLock || caps != 0
        }
    }

    private fun keyDownUp(keyEventCode: Int) {
        currentInputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode))
        currentInputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyEventCode))
    }

    private fun sendKey(keyCode: Int) {
        when (keyCode) {
            '\n'.toInt() -> keyDownUp(KeyEvent.KEYCODE_ENTER)
            else -> {
                if (keyCode >= '0'.toInt() && keyCode <= '9'.toInt()) {
                    keyDownUp(keyCode - '0'.toInt() + KeyEvent.KEYCODE_0)
                } else {
                    currentInputConnection.commitText(keyCode.toChar().toString(), 1)
                }
            }
        }
    }

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        if (isWordSeparator(primaryCode)) {
            if (mComposing.isNotEmpty()) commitTyped(currentInputConnection)
            sendKey(primaryCode)
            updateShiftKeyState(currentInputEditorInfo)
        } else if (primaryCode == Keyboard.KEYCODE_DELETE) {
            handleBackspace()
        } else if (primaryCode == Keyboard.KEYCODE_SHIFT) {
            handleShift()
        } else if (primaryCode == Keyboard.KEYCODE_CANCEL) {
            handleClose()
        } else if (primaryCode == LatinKeyboardView.KEYCODE_LANGUAGE_SWITCH) {
            val current = mInputView?.keyboard
            if (current == russianKeyboard) {
                setLatinKeyboard(englishKeyboard)
            } else {
                setLatinKeyboard(russianKeyboard)
            }
        } else if (primaryCode == Keyboard.KEYCODE_MODE_CHANGE && mInputView != null) {
            val current = mInputView?.keyboard
            if (current == mSymbolsKeyboard || current == mSymbolsShiftedKeyboard) {
                setLatinKeyboard(russianKeyboard)
            } else {
                setLatinKeyboard(mSymbolsKeyboard)
                mSymbolsKeyboard?.isShifted = false
            }
        } else if (primaryCode == LatinKeyboardView.KEYCODE_MODE_2_CHANGE && mInputView != null) {
            val current = mInputView?.keyboard
            if (current == mSymbolsKeyboard) {
                setLatinKeyboard(mSymbolsShiftedKeyboard)
            } else {
                setLatinKeyboard(mSymbolsKeyboard)
            }
        } else if (primaryCode == LatinKeyboardView.KEYCODE_MODE_3_CHANGE && mInputView != null) {
            val current = mInputView?.keyboard
            if (current == mSymbolsKeyboard || current == mSymbolsShiftedKeyboard) {
                setLatinKeyboard(digitsKeyboard)
            } else {
                setLatinKeyboard(mSymbolsKeyboard)
            }
        } else if (primaryCode == LatinKeyboardView.KEYCODE_MODE_4_CHANGE && mInputView != null) {
            val current = mInputView?.keyboard
            if (current == mSymbolsKeyboard || current == mSymbolsShiftedKeyboard || current == digitsKeyboard) {
                setLatinKeyboard(russianKeyboard)
            }
        } else {
            handleCharacter(primaryCode, keyCodes)
        }
    }

    override fun onText(text: CharSequence) {
        val ic = currentInputConnection ?: return
        ic.beginBatchEdit()
        if (mComposing.isNotEmpty()) commitTyped(ic)
        ic.commitText(text, 0)
        ic.endBatchEdit()
        updateShiftKeyState(currentInputEditorInfo)
    }

    private fun updateCandidates() {
        if (!mCompletionOn) {
            if (mComposing.isNotEmpty()) {
                setSuggestions(listOf(mComposing.toString()), completions = true, typedWordValid = true)
            } else {
                setSuggestions(null, completions = false, typedWordValid = false)
            }
        }
    }

    fun setSuggestions(suggestions: List<String>?, completions: Boolean, typedWordValid: Boolean) {
        if (!suggestions.isNullOrEmpty() || isExtractViewShown) {
            setCandidatesViewShown(true)
        }
        mCandidateView?.setSuggestions(suggestions, completions, typedWordValid)
    }

    private fun handleBackspace() {
        val length = mComposing.length
        if (length > 1) {
            mComposing.delete(length - 1, length)
            currentInputConnection.setComposingText(mComposing, 1)
            updateCandidates()
        } else if (length > 0) {
            mComposing.setLength(0)
            currentInputConnection.commitText("", 0)
            updateCandidates()
        } else {
            keyDownUp(KeyEvent.KEYCODE_DEL)
        }
        updateShiftKeyState(currentInputEditorInfo)
    }

    private fun handleShift() {
        val currentKeyboard = mInputView?.keyboard
        if (russianKeyboard == currentKeyboard || englishKeyboard == currentKeyboard) {
            checkToggleCapsLock()
            mInputView?.isShifted = mCapsLock || !mInputView!!.isShifted
        } else if (currentKeyboard == mSymbolsKeyboard) {
            mSymbolsKeyboard?.isShifted = true
            setLatinKeyboard(mSymbolsShiftedKeyboard)
            mSymbolsShiftedKeyboard?.isShifted = true
        } else if (currentKeyboard == mSymbolsShiftedKeyboard) {
            mSymbolsShiftedKeyboard?.isShifted = false
            setLatinKeyboard(mSymbolsKeyboard)
            mSymbolsKeyboard?.isShifted = false
        }
    }

    private fun handleCharacter(primaryCode: Int, keyCodes: IntArray?) {
        var code = primaryCode
        if (isInputViewShown && mInputView!!.isShifted) {
            code = Character.toUpperCase(code)
        }
        mComposing.append(code.toChar())
        currentInputConnection.setComposingText(mComposing, 1)
        updateShiftKeyState(currentInputEditorInfo)
        updateCandidates()
    }

    private fun handleClose() {
        commitTyped(currentInputConnection)
        requestHideSelf(0)
        mInputView?.closing()
    }

    private fun checkToggleCapsLock() {
        val now = System.currentTimeMillis()
        if (mLastShiftTime + 800 > now) {
            mCapsLock = !mCapsLock
            mLastShiftTime = 0
        } else {
            mLastShiftTime = now
        }
    }

    private fun isWordSeparator(code: Int): Boolean {
        return mWordSeparators?.contains(code.toChar()) == true
    }

    fun pickSuggestionManually(index: Int) {
        if (mCompletionOn && mCompletions != null && index >= 0 && index < mCompletions!!.size) {
            val ci = mCompletions!![index]
            currentInputConnection.commitCompletion(ci)
            mCandidateView?.clear()
            updateShiftKeyState(currentInputEditorInfo)
        } else if (mComposing.isNotEmpty()) {
            commitTyped(currentInputConnection)
        }
    }

    override fun swipeRight() {
        if (mCompletionOn) pickSuggestionManually(0)
    }

    override fun swipeLeft() = handleBackspace()
    override fun swipeDown() = handleClose()
    override fun swipeUp() {}
    override fun onPress(primaryCode: Int) {}
    override fun onRelease(primaryCode: Int) {}

    companion object {
        const val PROCESS_HARD_KEYS = true
    }
}
