package software.kanunnikoff.izhitsa

import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.CompletionInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import java.util.ArrayList

class SoftKeyboard : InputMethodService(), ModernKeyboardView.OnKeyPressListener {
    private var inputView: ModernKeyboardView? = null
    private var candidateView: CandidateView? = null
    private var completions: Array<CompletionInfo>? = null
    private var suggestions: List<String>? = null

    private val composing = StringBuilder()
    private var predictionOn = false
    private var completionOn = false
    private var capsLock = false
    private var lastShiftTime = 0L

    private var russianLayout: ImeKeyboardLayout? = null
    private var englishLayout: ImeKeyboardLayout? = null
    private var symbolsLayout: ImeKeyboardLayout? = null
    private var symbolsShiftLayout: ImeKeyboardLayout? = null
    private var digitsLayout: ImeKeyboardLayout? = null

    private var curKeyboard: KeyboardType? = null
    private var alphabetKeyboard: KeyboardType = KeyboardType.RUSSIAN

    private lateinit var wordSeparators: String

    override fun onCreate() {
        super.onCreate()
        wordSeparators = resources.getString(R.string.word_separators)
    }

    override fun onInitializeInterface() {
        if (russianLayout != null) return
        russianLayout = ImeKeyboardLayouts.russian()
        englishLayout = ImeKeyboardLayouts.english()
        symbolsLayout = ImeKeyboardLayouts.symbols()
        symbolsShiftLayout = ImeKeyboardLayouts.symbolsShift()
        digitsLayout = ImeKeyboardLayouts.digits()
    }

    override fun onCreateInputView(): View {
        if (russianLayout == null) onInitializeInterface()
        inputView = layoutInflater.inflate(R.layout.input, null) as ModernKeyboardView
        inputView?.setOnKeyPressListener(this)
        setKeyboard(KeyboardType.RUSSIAN)
        return inputView!!
    }

    override fun onEvaluateInputViewShown(): Boolean {
        return true
    }

    override fun onShowInputRequested(flags: Int, configChange: Boolean): Boolean {
        return true
    }

    override fun onCreateCandidatesView(): View {
        candidateView = CandidateView(this)
        candidateView?.setService(this)
        return candidateView as CandidateView
    }

    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInput(attribute, restarting)

        composing.setLength(0)
        updateCandidates()

        predictionOn = false
        completionOn = false
        completions = null

        when (attribute.inputType and InputType.TYPE_MASK_CLASS) {
            InputType.TYPE_CLASS_NUMBER,
            InputType.TYPE_CLASS_DATETIME,
            InputType.TYPE_CLASS_PHONE -> {
                setKeyboard(KeyboardType.DIGITS)
            }
            InputType.TYPE_CLASS_TEXT -> {
                setKeyboard(alphabetKeyboard)
                predictionOn = true

                val variation = attribute.inputType and InputType.TYPE_MASK_VARIATION
                if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                    variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                ) {
                    predictionOn = false
                }

                if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
                    variation == InputType.TYPE_TEXT_VARIATION_URI ||
                    variation == InputType.TYPE_TEXT_VARIATION_FILTER
                ) {
                    predictionOn = false
                }

                if ((attribute.inputType and InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                    predictionOn = false
                    completionOn = isFullscreenMode
                }

                updateShiftKeyState(attribute)
            }
            else -> {
                setKeyboard(alphabetKeyboard)
                updateShiftKeyState(attribute)
            }
        }
    }

    override fun onFinishInput() {
        super.onFinishInput()
        composing.setLength(0)
        updateCandidates()
        setCandidatesViewShown(false)
    }

    override fun onStartInputView(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInputView(attribute, restarting)
        if (russianLayout == null) onInitializeInterface()
        val target = curKeyboard ?: alphabetKeyboard
        setKeyboard(target)
        inputView?.refreshKeys()
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)

        if (composing.isNotEmpty() && (newSelStart != candidatesEnd || newSelEnd != candidatesEnd)) {
            composing.setLength(0)
            updateCandidates()
            currentInputConnection?.finishComposingText()
        }
    }

    private fun keyDownUp(keyEventCode: Int) {
        val ic = currentInputConnection
        ic?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode))
        ic?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyEventCode))
    }

    private fun sendKey(keyCode: Int) {
        when {
            keyCode == '\n'.code -> keyDownUp(KeyEvent.KEYCODE_ENTER)
            keyCode in '0'.code..'9'.code -> keyDownUp(keyCode - '0'.code + KeyEvent.KEYCODE_0)
            else -> currentInputConnection?.commitText(keyCode.toChar().toString(), 1)
        }
    }

    override fun onKey(primaryCode: Int) {
        if (isWordSeparator(primaryCode)) {
            if (composing.isNotEmpty()) commitTyped(currentInputConnection)
            sendKey(primaryCode)
            updateShiftKeyState(currentInputEditorInfo)
        } else when (primaryCode) {
            KEYCODE_DELETE -> handleBackspace()
            KEYCODE_SHIFT -> handleShift()
            KeyEvent.KEYCODE_BACK -> handleClose()
            KEYCODE_LANGUAGE_SWITCH -> {
                setKeyboard(if (alphabetKeyboard == KeyboardType.RUSSIAN) KeyboardType.ENGLISH else KeyboardType.RUSSIAN)
            }
            KEYCODE_MODE_CHANGE -> {
                if (curKeyboard == KeyboardType.SYMBOLS || curKeyboard == KeyboardType.SYMBOLS_SHIFT) {
                    setKeyboard(alphabetKeyboard)
                } else {
                    setKeyboard(KeyboardType.SYMBOLS)
                    inputView?.setShifted(false)
                }
            }
            KEYCODE_MODE_2_CHANGE -> {
                if (curKeyboard == KeyboardType.SYMBOLS_SHIFT) {
                    setKeyboard(KeyboardType.SYMBOLS)
                } else if (curKeyboard == KeyboardType.SYMBOLS) {
                    setKeyboard(KeyboardType.SYMBOLS_SHIFT)
                }
            }
            KEYCODE_MODE_3_CHANGE -> {
                if (curKeyboard == KeyboardType.DIGITS) {
                    setKeyboard(KeyboardType.SYMBOLS)
                } else {
                    setKeyboard(KeyboardType.DIGITS)
                }
            }
            KEYCODE_MODE_4_CHANGE -> setKeyboard(alphabetKeyboard)
            else -> handleCharacter(primaryCode)
        }
    }

    private fun handleBackspace() {
        val length = composing.length
        val ic = currentInputConnection ?: return
        if (length > 0) {
            composing.delete(length - 1, length)
            ic.setComposingText(composing, 1)
            updateCandidates()
        } else {
            keyDownUp(KeyEvent.KEYCODE_DEL)
        }
    }

    private fun handleShift() {
        val view = inputView ?: return
        when (curKeyboard) {
            KeyboardType.SYMBOLS -> {
                setKeyboard(KeyboardType.SYMBOLS_SHIFT)
                return
            }
            KeyboardType.SYMBOLS_SHIFT -> {
                setKeyboard(KeyboardType.SYMBOLS)
                return
            }
            KeyboardType.RUSSIAN, KeyboardType.ENGLISH -> {
                checkToggleCapsLock()
                view.setShifted(capsLock || !view.isShifted())
            }
            else -> Unit
        }
    }

    private fun handleCharacter(primaryCode: Int) {
        val view = inputView
        var code = primaryCode
        if (view != null && view.isShifted()) {
            code = Character.toUpperCase(code)
        }
        composing.append(code.toChar())
        currentInputConnection?.setComposingText(composing, 1)
        updateShiftKeyState(currentInputEditorInfo)
        updateCandidates()
    }

    private fun handleClose() {
        commitTyped(currentInputConnection)
        requestHideSelf(0)
    }

    private fun commitTyped(inputConnection: InputConnection?) {
        if (inputConnection != null && composing.isNotEmpty()) {
            inputConnection.commitText(composing, composing.length)
            composing.setLength(0)
            updateCandidates()
        }
    }

    private fun updateShiftKeyState(attr: EditorInfo?) {
        if (attr != null && inputView != null) {
            val ic = currentInputConnection
            val caps = ic?.getCursorCapsMode(attr.inputType) ?: 0
            if (curKeyboard == KeyboardType.RUSSIAN || curKeyboard == KeyboardType.ENGLISH) {
                inputView?.setShifted(capsLock || caps != 0)
            }
        }
    }

    private fun updateCandidates() {
        if (!completionOn) {
            if (composing.isNotEmpty()) {
                val list = ArrayList<String>()
                list.add(composing.toString())
                setSuggestions(list, true, true)
            } else {
                setSuggestions(null, false, false)
            }
        }
    }

    fun setSuggestions(suggestions: List<String>?, completions: Boolean, typedWordValid: Boolean) {
        this.suggestions = suggestions?.let { ArrayList(it) }
        if (candidateView != null) {
            setCandidatesViewShown(!suggestions.isNullOrEmpty())
            candidateView?.setSuggestions(suggestions, completions, typedWordValid)
        }
    }

    private fun checkToggleCapsLock() {
        val now = System.currentTimeMillis()
        if (lastShiftTime + 800 > now) {
            capsLock = !capsLock
            lastShiftTime = 0
        } else {
            lastShiftTime = now
        }
    }

    private fun isWordSeparator(code: Int): Boolean {
        return wordSeparators.contains(code.toChar())
    }

    fun pickSuggestionManually(index: Int) {
        val ic = currentInputConnection ?: return

        val currentCompletions = completions
        if (completionOn && currentCompletions != null &&
            index >= 0 && index < currentCompletions.size
        ) {
            val ci = currentCompletions[index]
            ic.commitCompletion(ci)
            composing.setLength(0)
            updateCandidates()
            return
        }

        val currentSuggestions = suggestions
        if (currentSuggestions == null || index < 0 || index >= currentSuggestions.size) {
            return
        }

        val suggestion = currentSuggestions[index]
        ic.commitText(suggestion, 1)
        composing.setLength(0)
        updateCandidates()
    }

    private fun setKeyboard(type: KeyboardType) {
        if (russianLayout == null) onInitializeInterface()
        curKeyboard = type
        if (type == KeyboardType.RUSSIAN || type == KeyboardType.ENGLISH) {
            alphabetKeyboard = type
        }
        inputView?.setKeyboardLayout(layoutFor(type))
    }

    private fun layoutFor(type: KeyboardType): ImeKeyboardLayout? {
        return when (type) {
            KeyboardType.ENGLISH -> englishLayout
            KeyboardType.SYMBOLS -> symbolsLayout
            KeyboardType.SYMBOLS_SHIFT -> symbolsShiftLayout
            KeyboardType.DIGITS -> digitsLayout
            KeyboardType.RUSSIAN -> russianLayout
        }
    }

    private enum class KeyboardType {
        RUSSIAN,
        ENGLISH,
        SYMBOLS,
        SYMBOLS_SHIFT,
        DIGITS
    }

    companion object {
        const val KEYCODE_SHIFT = -1
        const val KEYCODE_MODE_CHANGE = -2
        const val KEYCODE_DELETE = -5
        const val KEYCODE_LANGUAGE_SWITCH = -101
        const val KEYCODE_MODE_2_CHANGE = -102
        const val KEYCODE_MODE_3_CHANGE = -103
        const val KEYCODE_MODE_4_CHANGE = -104
    }
}
