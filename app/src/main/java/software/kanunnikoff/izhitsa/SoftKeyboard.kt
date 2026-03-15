package software.kanunnikoff.izhitsa

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.CompletionInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import software.kanunnikoff.izhitsa.compose.KeyboardLayouts
import software.kanunnikoff.izhitsa.compose.KeyboardScreen
import software.kanunnikoff.izhitsa.compose.KeyInfo
import java.util.Locale

/**
 * Example of writing an input method for a soft keyboard.
 */
class SoftKeyboard : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private var mInputMethodManager: InputMethodManager? = null
    private var mCandidateView: CandidateView? = null
    private var mCompletions: Array<CompletionInfo>? = null

    private val mComposing = StringBuilder()
    private var mPredictionOn = false
    private var mCompletionOn = false
    private var shiftState = ShiftState.OFF
    private var mLastShiftTime: Long = 0

    private var mWordSeparators: String? = null
    
    private val currentLayout = mutableStateOf(KeyboardLayouts.Russian)
    private var baseLayout: List<List<KeyInfo>> = KeyboardLayouts.Russian

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val mViewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = mViewModelStore
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()

        mInputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        mWordSeparators = resources.getString(R.string.word_separators)

        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    override fun onWindowShown() {
        super.onWindowShown()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    private val displayContext: Context
        get() {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display?.let { createDisplayContext(it) } ?: this
            } else {
                @Suppress("DEPRECATION")
                val wm = getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
                @Suppress("DEPRECATION")
                createDisplayContext(wm.defaultDisplay)
            }
        }

    override fun onCreateInputView(): View {
        // Ensure the IME window root has view tree owners for Compose.
        window?.window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(this@SoftKeyboard)
            decorView.setViewTreeViewModelStoreOwner(this@SoftKeyboard)
            decorView.setViewTreeSavedStateRegistryOwner(this@SoftKeyboard)
        }

        return ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@SoftKeyboard)
            setViewTreeViewModelStoreOwner(this@SoftKeyboard)
            setViewTreeSavedStateRegistryOwner(this@SoftKeyboard)
            setContent {
                KeyboardScreen(
                    rows = currentLayout.value,
                    onKeyClick = { code -> onKey(code, null) }
                )
            }
        }
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

        mPredictionOn = false
        mCompletionOn = false
        mCompletions = null

        when (attribute.inputType and InputType.TYPE_MASK_CLASS) {
            InputType.TYPE_CLASS_NUMBER, InputType.TYPE_CLASS_DATETIME, InputType.TYPE_CLASS_PHONE -> {
                // TODO: Add digits layout to Compose
            }
            InputType.TYPE_CLASS_TEXT -> {
                baseLayout = KeyboardLayouts.Russian
                currentLayout.value = applyCaps(baseLayout, shiftState.isCapsEnabled)
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
            }
            else -> {
                baseLayout = KeyboardLayouts.English
                currentLayout.value = applyCaps(baseLayout, shiftState.isCapsEnabled)
            }
        }
    }

    override fun onFinishInput() {
        super.onFinishInput()

        mComposing.setLength(0)
        updateCandidates()
        setCandidatesViewShown(false)
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

    private fun commitTyped(inputConnection: InputConnection) {
        if (mComposing.isNotEmpty()) {
            inputConnection.commitText(mComposing, mComposing.length)
            mComposing.setLength(0)
            updateCandidates()
        }
    }

    private fun keyDownUp(keyEventCode: Int) {
        currentInputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode))
        currentInputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyEventCode))
    }

    private fun sendKey(keyCode: Int) {
        when (keyCode) {
            '\n'.code -> keyDownUp(KeyEvent.KEYCODE_ENTER)
            else -> {
                if (keyCode >= '0'.code && keyCode <= '9'.code) {
                    keyDownUp(keyCode - '0'.code + KeyEvent.KEYCODE_0)
                } else {
                    currentInputConnection.commitText(keyCode.toChar().toString(), 1)
                }
            }
        }
    }

    fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        if (isWordSeparator(primaryCode)) {
            if (mComposing.isNotEmpty()) commitTyped(currentInputConnection)
            sendKey(primaryCode)
        } else if (primaryCode == -5) { // KEYCODE_DELETE
            handleBackspace()
        } else if (primaryCode == -1) { // KEYCODE_SHIFT
            handleShift()
        } else if (primaryCode == -101) { // KEYCODE_LANGUAGE_SWITCH
            if (baseLayout == KeyboardLayouts.Russian) {
                baseLayout = KeyboardLayouts.English
            } else {
                baseLayout = KeyboardLayouts.Russian
            }
            currentLayout.value = applyCaps(baseLayout, shiftState.isCapsEnabled)
        } else {
            handleCharacter(primaryCode, keyCodes)
        }
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
    }

    private fun handleShift() {
        val now = System.currentTimeMillis()

        if (shiftState == ShiftState.CAPS_LOCK) {
            shiftState = ShiftState.OFF
            mLastShiftTime = 0
            currentLayout.value = applyCaps(baseLayout, shiftState.isCapsEnabled)
            return
        }

        shiftState = when (shiftState) {
            ShiftState.ONESHOT -> {
                if (mLastShiftTime + 800 > now) ShiftState.CAPS_LOCK else ShiftState.OFF
            }

            ShiftState.OFF -> ShiftState.ONESHOT
            ShiftState.CAPS_LOCK -> ShiftState.OFF
        }

        mLastShiftTime = now
        currentLayout.value = applyCaps(baseLayout, shiftState.isCapsEnabled)
    }

    private fun handleCharacter(primaryCode: Int, keyCodes: IntArray?) {
        var code = primaryCode

        if (shiftState.isCapsEnabled) {
            code = Character.toUpperCase(code)
        }

        mComposing.append(code.toChar())
        currentInputConnection.setComposingText(mComposing, 1)
        updateCandidates()

        if (shiftState == ShiftState.ONESHOT) {
            shiftState = ShiftState.OFF
            currentLayout.value = applyCaps(baseLayout, shiftState.isCapsEnabled)
        }
    }

    @Suppress("unused")
    private fun handleClose() {
        commitTyped(currentInputConnection)
        requestHideSelf(0)
    }

    private enum class ShiftState {
        OFF,
        ONESHOT,
        CAPS_LOCK;

        val isCapsEnabled: Boolean
            get() = this != OFF
    }

    private fun isWordSeparator(code: Int): Boolean {
        return mWordSeparators?.contains(code.toChar()) == true
    }

    private fun applyCaps(
        layout: List<List<KeyInfo>>,
        enabled: Boolean
    ): List<List<KeyInfo>> {
        if (!enabled) {
            return layout.map { row ->
                row.map { key ->
                    if (key.label?.length == 1 && key.code > 0 && !key.isModifier) {
                        val lower = key.label.lowercase(Locale.getDefault())
                        val lowerCode = lower[0].code
                        key.copy(label = lower, code = lowerCode)
                    } else {
                        key
                    }
                }
            }
        }

        return layout.map { row ->
            row.map { key ->
                if (key.label?.length == 1 && key.code > 0 && !key.isModifier) {
                    val upper = key.label.uppercase(Locale.getDefault())
                    val upperCode = upper[0].code
                    key.copy(label = upper, code = upperCode)
                } else {
                    key
                }
            }
        }
    }

    fun pickSuggestionManually(index: Int) {
        if (mCompletionOn && mCompletions != null && index >= 0 && index < mCompletions!!.size) {
            val ci = mCompletions!![index]
            currentInputConnection.commitCompletion(ci)
            mCandidateView?.clear()
        } else if (mComposing.isNotEmpty()) {
            commitTyped(currentInputConnection)
        }
    }
}
