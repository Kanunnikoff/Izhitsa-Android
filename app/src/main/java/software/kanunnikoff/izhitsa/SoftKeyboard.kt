package software.kanunnikoff.izhitsa

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.provider.Settings
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.CompletionInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
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
import software.kanunnikoff.izhitsa.compose.KeyIcon
import software.kanunnikoff.izhitsa.compose.KeyInfo
import software.kanunnikoff.izhitsa.compose.KeyLongPressAction
import software.kanunnikoff.izhitsa.compose.KeyboardKeyCodes
import software.kanunnikoff.izhitsa.compose.KeyboardLayouts
import software.kanunnikoff.izhitsa.compose.KeyboardPanel
import software.kanunnikoff.izhitsa.compose.KeyboardScreen
import software.kanunnikoff.izhitsa.compose.KeyboardToolbarAction
import software.kanunnikoff.izhitsa.stickers.Sticker
import software.kanunnikoff.izhitsa.stickers.StickerContentSender
import software.kanunnikoff.izhitsa.stickers.StickerRepository
import java.util.Locale

class SoftKeyboard : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private lateinit var russianDictionary: RussianDictionary
    private val composingText = StringBuilder()
    private var completions: Array<CompletionInfo>? = null
    private var completionSuggestions: List<String> = emptyList()
    private var predictionEnabled = false
    private var completionEnabled = false
    private var sensitiveInput = false
    private var inputClass = InputType.TYPE_CLASS_TEXT
    private var supportsStickerContent = false
    private var shiftState = ShiftState.OFF
    private var lastShiftTimeMillis = 0L
    private var alternativeTap: AlternativeTap? = null
    private var wordSeparators = ""

    private val currentLayout = mutableStateOf(KeyboardLayouts.Russian)
    private val currentPanel = mutableStateOf(KeyboardPanel.KEYS)
    private val currentSuggestions = mutableStateOf<List<String>>(emptyList())
    private val currentClipboardText = mutableStateOf<String?>(null)
    private lateinit var stickerContentSender: StickerContentSender

    private var baseLayout: List<List<KeyInfo>> = KeyboardLayouts.Russian
    private var layoutMode = LayoutMode.ALPHA

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val keyboardViewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() = keyboardViewModelStore

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()

        wordSeparators = resources.getString(R.string.word_separators)
        russianDictionary = RussianDictionary(context = applicationContext)
        russianDictionary.prepare()
        stickerContentSender = StickerContentSender(
            repository = StickerRepository(context = applicationContext)
        )

        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onDestroy() {
        if (lifecycleRegistry.currentState == Lifecycle.State.RESUMED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        }

        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        keyboardViewModelStore.clear()

        super.onDestroy()
    }

    override fun onWindowShown() {
        super.onWindowShown()

        if (lifecycleRegistry.currentState == Lifecycle.State.CREATED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }

        if (lifecycleRegistry.currentState == Lifecycle.State.STARTED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
    }

    override fun onWindowHidden() {
        if (lifecycleRegistry.currentState == Lifecycle.State.RESUMED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        }

        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }

        super.onWindowHidden()
    }

    override fun onCreateInputView(): View {
        window?.window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(this)
            decorView.setViewTreeViewModelStoreOwner(this)
            decorView.setViewTreeSavedStateRegistryOwner(this)
        }

        return ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@SoftKeyboard)
            setViewTreeViewModelStoreOwner(this@SoftKeyboard)
            setViewTreeSavedStateRegistryOwner(this@SoftKeyboard)
            setContent {
                KeyboardScreen(
                    rows = currentLayout.value,
                    isNumberLayout = this@SoftKeyboard.layoutMode == LayoutMode.NUMBERS,
                    panel = currentPanel.value,
                    suggestions = currentSuggestions.value,
                    clipboardText = currentClipboardText.value,
                    supportsStickerContent = supportsStickerContent,
                    onKeyClick = ::onKey,
                    onKeyLongPressAction = ::handleKeyLongPressAction,
                    onAlternativeSelected = ::onAlternativeSelected,
                    onSuggestionClick = ::commitSuggestion,
                    onToolbarAction = ::handleToolbarAction,
                    onEmojiPicked = ::commitDirectText,
                    onReactionPicked = ::commitDirectText,
                    onStickerPicked = ::commitSticker,
                    onClosePanel = ::showLettersPanel
                )
            }
        }
    }

    /**
     * Оставляет экранную клавиатуру доступной при подключённой аппаратной клавиатуре.
     * Это важно для явно выбранного пользователем метода ввода на эмуляторах,
     * планшетах и устройствах с внешней клавиатурой.
     */
    override fun onEvaluateInputViewShown(): Boolean {
        super.onEvaluateInputViewShown()

        return true
    }

    override fun onStartInput(
        attribute: EditorInfo,
        restarting: Boolean
    ) {
        super.onStartInput(attribute, restarting)

        composingText.clear()
        completions = null
        completionSuggestions = emptyList()
        alternativeTap = null
        predictionEnabled = false
        completionEnabled = false
        sensitiveInput = false
        shiftState = ShiftState.OFF
        lastShiftTimeMillis = 0L
        currentPanel.value = KeyboardPanel.KEYS
        inputClass = attribute.inputType and InputType.TYPE_MASK_CLASS
        supportsStickerContent = stickerContentSender.isSupported(
            editorInfo = attribute
        )

        when (inputClass) {
            InputType.TYPE_CLASS_NUMBER,
            InputType.TYPE_CLASS_DATETIME,
            InputType.TYPE_CLASS_PHONE -> {
                layoutMode = LayoutMode.NUMBERS
                baseLayout = KeyboardLayouts.Russian
                publishLayout(KeyboardLayouts.Numbers)
            }

            InputType.TYPE_CLASS_TEXT -> configureTextInput(attribute = attribute)

            else -> {
                layoutMode = LayoutMode.ALPHA
                baseLayout = KeyboardLayouts.Russian
                publishLayout(baseLayout)
            }
        }

        refreshTextState()
    }

    override fun onStartInputView(
        info: EditorInfo,
        restarting: Boolean
    ) {
        super.onStartInputView(info, restarting)

        AppPreferences(context = applicationContext).hasUsedKeyboard = true
        updateAutomaticShift()
        refreshTextState()
    }

    override fun onFinishInput() {
        composingText.clear()
        completionSuggestions = emptyList()
        currentSuggestions.value = emptyList()
        currentPanel.value = KeyboardPanel.KEYS
        setCandidatesViewShown(false)

        super.onFinishInput()
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) {
        super.onUpdateSelection(
            oldSelStart,
            oldSelEnd,
            newSelStart,
            newSelEnd,
            candidatesStart,
            candidatesEnd
        )

        if (
            composingText.isNotEmpty() &&
            (newSelStart != candidatesEnd || newSelEnd != candidatesEnd)
        ) {
            composingText.clear()
            currentInputConnection?.finishComposingText()
        }

        alternativeTap = null
        updateAutomaticShift()
        refreshTextState()
    }

    override fun onDisplayCompletions(newCompletions: Array<CompletionInfo>?) {
        if (!completionEnabled) {
            return
        }

        completions = newCompletions
        completionSuggestions = newCompletions
            ?.mapNotNull { completion ->
                completion.text?.toString()
            }
            .orEmpty()
            .take(MaxSuggestionCount)

        refreshTextState()
    }

    private fun configureTextInput(attribute: EditorInfo) {
        baseLayout = KeyboardLayouts.Russian
        layoutMode = LayoutMode.ALPHA
        predictionEnabled = true

        val variation = attribute.inputType and InputType.TYPE_MASK_VARIATION
        sensitiveInput = variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD

        if (
            sensitiveInput ||
            variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
            variation == InputType.TYPE_TEXT_VARIATION_URI ||
            variation == InputType.TYPE_TEXT_VARIATION_FILTER
        ) {
            predictionEnabled = false
        }

        if ((attribute.inputType and InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
            predictionEnabled = false
            completionEnabled = isFullscreenMode
        }

        publishLayout(baseLayout)
    }

    private fun onKey(key: KeyInfo) {
        when {
            isWordSeparator(code = key.code) -> {
                alternativeTap = null
                currentInputConnection?.let(::commitTyped)
                sendKey(keyCode = key.code)
                updateAutomaticShift()
            }

            key.code == KeyboardKeyCodes.DELETE -> handleBackspace()
            key.code == KeyboardKeyCodes.SHIFT -> handleShift()
            key.code == KeyboardKeyCodes.MODE_ALPHA -> showAlphabetLayout()
            key.code == KeyboardKeyCodes.SYMBOLS -> showSymbolsLayout()
            key.code == KeyboardKeyCodes.MORE_SYMBOLS -> showMoreSymbolsLayout()
            key.code == KeyboardKeyCodes.LANGUAGE -> switchLanguage()
            else -> handleCharacter(key = key)
        }

        refreshTextState()
    }

    private fun handleKeyLongPressAction(action: KeyLongPressAction) {
        when (action) {
            KeyLongPressAction.SHOW_EMOJI -> currentPanel.value = KeyboardPanel.EMOJI
        }
    }

    private fun onAlternativeSelected(
        key: KeyInfo,
        alternative: String
    ) {
        alternativeTap = null

        insertText(
            text = alternative,
            useComposingRegion = predictionEnabled && inputClass == InputType.TYPE_CLASS_TEXT
        )

        if (shiftState == ShiftState.ONESHOT) {
            shiftState = ShiftState.OFF
            publishCurrentMode()
        }

        refreshTextState()
    }

    private fun handleCharacter(key: KeyInfo) {
        val label = key.label ?: key.code.toChar().toString()
        val now = System.currentTimeMillis()
        val previousTap = alternativeTap
        val canCycleAlternative = key.alternatives.size > 1 &&
            previousTap != null &&
            previousTap.keyCode == key.code &&
            now - previousTap.timestampMillis <= AlternativeTapTimeoutMillis

        if (canCycleAlternative) {
            val nextIndex = (previousTap.alternativeIndex + 1) % key.alternatives.size
            val previousAlternative = key.alternatives[previousTap.alternativeIndex]
            val nextAlternative = key.alternatives[nextIndex]

            replaceLastInput(
                previousText = previousAlternative,
                replacementText = nextAlternative
            )

            alternativeTap = AlternativeTap(
                keyCode = key.code,
                alternativeIndex = nextIndex,
                timestampMillis = now
            )
        } else {
            insertText(
                text = label,
                useComposingRegion = predictionEnabled && inputClass == InputType.TYPE_CLASS_TEXT
            )

            alternativeTap = if (key.alternatives.size > 1) {
                AlternativeTap(
                    keyCode = key.code,
                    alternativeIndex = key.alternatives.indexOf(label).coerceAtLeast(0),
                    timestampMillis = now
                )
            } else {
                null
            }
        }

        if (shiftState == ShiftState.ONESHOT) {
            shiftState = ShiftState.OFF
            publishCurrentMode()
        }
    }

    private fun insertText(
        text: String,
        useComposingRegion: Boolean
    ) {
        val inputConnection = currentInputConnection ?: return

        if (useComposingRegion) {
            composingText.append(text)
            inputConnection.setComposingText(composingText, 1)
        } else {
            inputConnection.commitText(text, 1)
        }
    }

    private fun replaceLastInput(
        previousText: String,
        replacementText: String
    ) {
        val inputConnection = currentInputConnection ?: return

        if (composingText.endsWith(previousText)) {
            composingText.delete(
                composingText.length - previousText.length,
                composingText.length
            )
            composingText.append(replacementText)
            inputConnection.setComposingText(composingText, 1)
        } else {
            inputConnection.deleteSurroundingTextInCodePoints(
                previousText.codePointCount(0, previousText.length),
                0
            )
            inputConnection.commitText(replacementText, 1)
        }
    }

    private fun commitDirectText(text: String) {
        val inputConnection = currentInputConnection ?: return

        commitTyped(inputConnection = inputConnection)
        inputConnection.commitText(text, 1)
        alternativeTap = null
        refreshTextState()
    }

    private fun commitSticker(sticker: Sticker) {
        if (!supportsStickerContent) {
            return
        }

        val inputConnection = currentInputConnection ?: return

        commitTyped(inputConnection = inputConnection)
        stickerContentSender.commit(
            inputConnection = inputConnection,
            sticker = sticker
        )
    }

    private fun commitSuggestion(suggestion: String) {
        val inputConnection = currentInputConnection ?: return
        val completionIndex = completionSuggestions.indexOf(suggestion)

        if (
            completionEnabled &&
            completionIndex >= 0 &&
            completions != null &&
            completionIndex < completions!!.size
        ) {
            inputConnection.commitCompletion(completions!![completionIndex])
        } else {
            if (composingText.isNotEmpty()) {
                composingText.clear()
            } else {
                val textBeforeCursor = inputConnection.getTextBeforeCursor(
                    TextContextCharacterCount,
                    0
                )
                val currentWord = SuggestionEngine.extractCurrentWord(textBeforeCursor)

                if (currentWord.isNotEmpty()) {
                    inputConnection.deleteSurroundingTextInCodePoints(
                        currentWord.codePointCount(0, currentWord.length),
                        0
                    )
                }
            }

            inputConnection.commitText("$suggestion ", 1)
        }

        inputConnection.finishComposingText()
        alternativeTap = null
        updateAutomaticShift()
        refreshTextState()
    }

    private fun commitTyped(inputConnection: InputConnection) {
        if (composingText.isEmpty()) {
            return
        }

        inputConnection.commitText(composingText, 1)
        composingText.clear()
    }

    private fun handleBackspace() {
        val inputConnection = currentInputConnection ?: return
        alternativeTap = null

        when {
            composingText.isNotEmpty() -> {
                val lastCodePointStart = composingText.offsetByCodePoints(
                    composingText.length,
                    -1
                )
                composingText.delete(lastCodePointStart, composingText.length)

                if (composingText.isEmpty()) {
                    inputConnection.finishComposingText()
                } else {
                    inputConnection.setComposingText(composingText, 1)
                }
            }

            !inputConnection.getSelectedText(0).isNullOrEmpty() -> {
                inputConnection.commitText("", 1)
            }

            else -> {
                inputConnection.deleteSurroundingTextInCodePoints(1, 0)
            }
        }

        updateAutomaticShift()
        refreshTextState()
    }

    private fun handleShift() {
        if (layoutMode != LayoutMode.ALPHA) {
            return
        }

        alternativeTap = null
        val now = System.currentTimeMillis()

        if (shiftState == ShiftState.CAPS_LOCK) {
            shiftState = ShiftState.OFF
            lastShiftTimeMillis = 0L
            publishCurrentMode()
            return
        }

        shiftState = when (shiftState) {
            ShiftState.ONESHOT -> {
                if (lastShiftTimeMillis + ShiftDoubleTapTimeoutMillis > now) {
                    ShiftState.CAPS_LOCK
                } else {
                    ShiftState.OFF
                }
            }

            ShiftState.OFF -> ShiftState.ONESHOT
            ShiftState.CAPS_LOCK -> ShiftState.OFF
        }

        lastShiftTimeMillis = now
        publishCurrentMode()
    }

    private fun updateAutomaticShift() {
        if (
            layoutMode != LayoutMode.ALPHA ||
            shiftState == ShiftState.CAPS_LOCK
        ) {
            return
        }

        val inputConnection = currentInputConnection ?: return
        val inputType = currentInputEditorInfo?.inputType ?: InputType.TYPE_CLASS_TEXT
        val shouldCapitalize = inputConnection.getCursorCapsMode(inputType) != 0
        val nextState = if (shouldCapitalize) {
            ShiftState.ONESHOT
        } else {
            ShiftState.OFF
        }

        if (shiftState != nextState) {
            shiftState = nextState
            publishCurrentMode()
        }
    }

    private fun showAlphabetLayout() {
        layoutMode = LayoutMode.ALPHA
        shiftState = ShiftState.OFF
        currentPanel.value = KeyboardPanel.KEYS
        publishLayout(baseLayout)
        updateAutomaticShift()
    }

    private fun showSymbolsLayout() {
        layoutMode = LayoutMode.SYMBOLS1
        shiftState = ShiftState.OFF
        currentPanel.value = KeyboardPanel.KEYS
        publishLayout(KeyboardLayouts.Symbols)
    }

    private fun showMoreSymbolsLayout() {
        if (layoutMode != LayoutMode.SYMBOLS1) {
            return
        }

        layoutMode = LayoutMode.SYMBOLS2
        shiftState = ShiftState.OFF
        publishLayout(KeyboardLayouts.Symbols2)
    }

    private fun switchLanguage() {
        baseLayout = if (baseLayout == KeyboardLayouts.Russian) {
            KeyboardLayouts.English
        } else {
            KeyboardLayouts.Russian
        }

        if (layoutMode == LayoutMode.ALPHA) {
            publishCurrentMode()
        }
    }

    private fun publishCurrentMode() {
        val layout = when (layoutMode) {
            LayoutMode.ALPHA -> baseLayout
            LayoutMode.SYMBOLS1 -> KeyboardLayouts.Symbols
            LayoutMode.SYMBOLS2 -> KeyboardLayouts.Symbols2
            LayoutMode.NUMBERS -> KeyboardLayouts.Numbers
        }

        publishLayout(layout)
    }

    private fun publishLayout(layout: List<List<KeyInfo>>) {
        val withCaps = applyCaps(
            layout = layout,
            enabled = shiftState.isCapsEnabled
        )
        currentLayout.value = applyEditorAction(layout = withCaps)
    }

    private fun applyCaps(
        layout: List<List<KeyInfo>>,
        enabled: Boolean
    ): List<List<KeyInfo>> {
        return layout.map { row ->
            row.map { key ->
                when {
                    key.code == KeyboardKeyCodes.SHIFT -> {
                        key.copy(isActive = shiftState.isCapsEnabled)
                    }

                    layoutMode == LayoutMode.ALPHA &&
                        key.label?.length == 1 &&
                        key.code > 0 &&
                        !key.isModifier -> {
                        val transformedLabel = if (enabled) {
                            key.label.uppercase(Locale.getDefault())
                        } else {
                            key.label.lowercase(Locale.getDefault())
                        }
                        val transformedAlternatives = key.alternatives.map { alternative ->
                            if (enabled) {
                                alternative.uppercase(Locale.getDefault())
                            } else {
                                alternative.lowercase(Locale.getDefault())
                            }
                        }

                        key.copy(
                            code = transformedLabel.first().code,
                            label = transformedLabel,
                            alternatives = transformedAlternatives
                        )
                    }

                    else -> key
                }
            }
        }
    }

    private fun applyEditorAction(layout: List<List<KeyInfo>>): List<List<KeyInfo>> {
        val editorInfo = currentInputEditorInfo ?: return layout
        val action = editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION

        return layout.map { row ->
            row.map { key ->
                if (key.code != KeyboardKeyCodes.ENTER) {
                    key
                } else {
                    when (action) {
                        EditorInfo.IME_ACTION_DONE -> key.copy(
                            label = null,
                            icon = KeyIcon.DONE
                        )

                        EditorInfo.IME_ACTION_SEARCH -> key.copy(
                            label = null,
                            icon = KeyIcon.SEARCH
                        )

                        EditorInfo.IME_ACTION_SEND -> key.copy(
                            label = null,
                            icon = KeyIcon.SEND
                        )

                        EditorInfo.IME_ACTION_GO -> key.copy(
                            label = "Перейти",
                            icon = null
                        )

                        EditorInfo.IME_ACTION_NEXT -> key.copy(
                            label = "Далее",
                            icon = null
                        )

                        else -> key.copy(
                            label = null,
                            icon = KeyIcon.ENTER
                        )
                    }
                }
            }
        }
    }

    private fun sendKey(keyCode: Int) {
        if (keyCode == KeyboardKeyCodes.ENTER) {
            sendEditorAction()
            return
        }

        currentInputConnection?.commitText(keyCode.toChar().toString(), 1)
    }

    private fun sendEditorAction() {
        val inputConnection = currentInputConnection ?: return
        val editorInfo = currentInputEditorInfo
        val action = editorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
            ?: EditorInfo.IME_ACTION_NONE
        val noEnterAction = editorInfo
            ?.imeOptions
            ?.and(EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0

        if (
            !noEnterAction &&
            action != EditorInfo.IME_ACTION_NONE &&
            action != EditorInfo.IME_ACTION_UNSPECIFIED
        ) {
            inputConnection.performEditorAction(action)
        } else {
            keyDownUp(keyEventCode = KeyEvent.KEYCODE_ENTER)
        }
    }

    private fun keyDownUp(keyEventCode: Int) {
        val inputConnection = currentInputConnection ?: return

        inputConnection.sendKeyEvent(
            KeyEvent(
                KeyEvent.ACTION_DOWN,
                keyEventCode
            )
        )
        inputConnection.sendKeyEvent(
            KeyEvent(
                KeyEvent.ACTION_UP,
                keyEventCode
            )
        )
    }

    private fun refreshTextState() {
        val inputConnection = currentInputConnection

        if (inputConnection == null || sensitiveInput) {
            currentSuggestions.value = emptyList()
            return
        }

        val textBeforeCursor = inputConnection.getTextBeforeCursor(
            TextContextCharacterCount,
            0
        )
        val extractedText = inputConnection.getExtractedText(
            ExtractedTextRequest(),
            0
        )?.text
        val hasText = !extractedText.isNullOrEmpty() ||
            !textBeforeCursor.isNullOrEmpty() ||
            composingText.isNotEmpty()

        currentSuggestions.value = when {
            !hasText -> emptyList()
            completionSuggestions.isNotEmpty() -> completionSuggestions
            inputClass == InputType.TYPE_CLASS_TEXT -> {
                SuggestionEngine.suggestionsFor(
                    textBeforeCursor = textBeforeCursor,
                    composingText = composingText,
                    dictionary = russianDictionary
                )
            }

            else -> {
                val currentValue = textBeforeCursor
                    ?.toString()
                    ?.takeLastWhile { character ->
                        !character.isWhitespace()
                    }
                    .orEmpty()

                listOfNotNull(currentValue.takeIf(String::isNotBlank))
            }
        }
    }

    private fun handleToolbarAction(action: KeyboardToolbarAction) {
        when (action) {
            KeyboardToolbarAction.SWITCH_INPUT_METHOD -> switchInputMethod()

            KeyboardToolbarAction.STICKERS -> {
                currentPanel.value = KeyboardPanel.STICKERS
            }

            KeyboardToolbarAction.CLIPBOARD -> {
                refreshClipboardText()
                currentPanel.value = KeyboardPanel.CLIPBOARD
            }

            KeyboardToolbarAction.SETTINGS -> openInputMethodSettings()
        }
    }

    @Suppress("DEPRECATION")
    private fun switchInputMethod() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            switchToNextInputMethod(false)
            return
        }

        val inputMethodManager = getSystemService(
            Context.INPUT_METHOD_SERVICE
        ) as InputMethodManager
        val windowToken = window?.window?.attributes?.token

        inputMethodManager.switchToNextInputMethod(windowToken, false)
    }

    private fun refreshClipboardText() {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val primaryClip = clipboardManager.primaryClip

        currentClipboardText.value = if (
            primaryClip != null &&
            primaryClip.itemCount > 0
        ) {
            primaryClip
                .getItemAt(0)
                .coerceToText(this)
                ?.toString()
                ?.take(MaxClipboardCharacterCount)
        } else {
            null
        }
    }

    private fun openInputMethodSettings() {
        /*
         * Настройки открываются отдельной задачей, потому что метод ввода является
         * службой и не располагает собственным Activity-контекстом.
         */
        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        startActivity(intent)
    }

    private fun showLettersPanel() {
        currentPanel.value = KeyboardPanel.KEYS
    }

    private fun isWordSeparator(code: Int): Boolean {
        return code > 0 && wordSeparators.contains(code.toChar())
    }

    fun pickSuggestionManually(index: Int) {
        currentSuggestions.value
            .getOrNull(index)
            ?.let(::commitSuggestion)
    }

    private data class AlternativeTap(
        val keyCode: Int,
        val alternativeIndex: Int,
        val timestampMillis: Long
    )

    private enum class ShiftState {
        OFF,
        ONESHOT,
        CAPS_LOCK;

        val isCapsEnabled: Boolean
            get() = this != OFF
    }

    private enum class LayoutMode {
        ALPHA,
        SYMBOLS1,
        SYMBOLS2,
        NUMBERS
    }

    private companion object {
        const val MaxSuggestionCount = 3
        const val TextContextCharacterCount = 128
        const val MaxClipboardCharacterCount = 10_000
        const val AlternativeTapTimeoutMillis = 500L
        const val ShiftDoubleTapTimeoutMillis = 800L
    }
}
