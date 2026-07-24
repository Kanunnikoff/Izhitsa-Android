package software.kanunnikoff.izhitsa

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.CompletionInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
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

/**
 * Основная служба метода ввода «Ижица».
 *
 * Служба связывает редактор Android через [InputConnection] с интерфейсом
 * Compose, управляет раскладками и регистром, формирует подсказки, предоставляет
 * панели эмодзи, стикеров и буфера обмена и воспроизводит обратную связь.
 */
class SoftKeyboard : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    // Состояние редактора хранится отдельно от наблюдаемого состояния интерфейса:
    // первое описывает протокол InputConnection, второе вызывает перерисовку Compose.
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
    private var isKeyboardSoundFeedbackEnabled = false

    private val currentLayout = mutableStateOf(KeyboardLayouts.Russian)
    private val currentPanel = mutableStateOf(KeyboardPanel.KEYS)
    private val currentSuggestions = mutableStateOf<List<String>>(emptyList())
    private val currentClipboardText = mutableStateOf<String?>(null)
    private val isKeyboardHapticFeedbackEnabled = mutableStateOf(false)
    private lateinit var preferences: AppPreferences
    private lateinit var audioManager: AudioManager
    private lateinit var vibrator: Vibrator
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

    /** Создаёт зависимости службы и переводит владельцев Compose в состояние `CREATED`. */
    override fun onCreate() {
        super.onCreate()

        wordSeparators = resources.getString(R.string.word_separators)
        preferences = AppPreferences(context = applicationContext)
        audioManager = getSystemService(AudioManager::class.java)
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        refreshFeedbackPreferences()
        russianDictionary = RussianDictionary(context = applicationContext)
        russianDictionary.prepare()
        stickerContentSender = StickerContentSender(
            repository = StickerRepository(context = applicationContext)
        )

        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    /** Завершает жизненный цикл Compose и освобождает созданные им модели представления. */
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

    /** Синхронизирует жизненный цикл Compose с появлением окна клавиатуры. */
    override fun onWindowShown() {
        super.onWindowShown()

        if (lifecycleRegistry.currentState == Lifecycle.State.CREATED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }

        if (lifecycleRegistry.currentState == Lifecycle.State.STARTED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
    }

    /** Приостанавливает Compose, когда окно метода ввода скрывается. */
    override fun onWindowHidden() {
        if (lifecycleRegistry.currentState == Lifecycle.State.RESUMED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        }

        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }

        super.onWindowHidden()
    }

    /** Создаёт корневое представление Compose и связывает его события со службой. */
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
                    isHapticFeedbackEnabled =
                        isKeyboardHapticFeedbackEnabled.value,
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

    /**
     * Сбрасывает состояние предыдущего поля и выбирает раскладку по типу нового поля.
     *
     * @param attribute сведения о новом поле ввода.
     * @param restarting повторно ли запускается ввод в том же поле.
     */
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

    /**
     * Отмечает фактическое использование клавиатуры и перечитывает настройки отклика.
     *
     * @param info сведения о поле, для которого показывается клавиатура.
     * @param restarting повторно ли показывается клавиатура для того же поля.
     */
    override fun onStartInputView(
        info: EditorInfo,
        restarting: Boolean
    ) {
        super.onStartInputView(info, restarting)

        preferences.hasUsedKeyboard = true
        refreshFeedbackPreferences()
        updateAutomaticShift()
        refreshTextState()
    }

    /** Завершает составной ввод и очищает панели при уходе из поля. */
    override fun onFinishInput() {
        composingText.clear()
        completionSuggestions = emptyList()
        currentSuggestions.value = emptyList()
        currentPanel.value = KeyboardPanel.KEYS
        setCandidatesViewShown(false)

        super.onFinishInput()
    }

    /**
     * Отслеживает внешнее перемещение курсора и отменяет устаревшую составную область.
     *
     * @param oldSelStart прежнее начало выделения.
     * @param oldSelEnd прежний конец выделения.
     * @param newSelStart новое начало выделения.
     * @param newSelEnd новый конец выделения.
     * @param candidatesStart начало составной области.
     * @param candidatesEnd конец составной области.
     */
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

    /**
     * Принимает варианты автозаполнения, предоставленные полноэкранным редактором.
     *
     * @param newCompletions новые варианты либо `null`, если редактор их очистил.
     */
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

    /**
     * Настраивает подсказки и приватность для конкретной разновидности текстового поля.
     *
     * @param attribute сведения о поле ввода.
     */
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

    /**
     * Направляет событие клавиши подходящему обработчику и обновляет подсказки.
     *
     * @param key нажатая клавиша текущей раскладки.
     * @return `true`, если событие обработано службой.
     */
    private fun onKey(key: KeyInfo): Boolean {
        performKeyFeedback()

        val handled = when {
            isWordSeparator(code = key.code) -> {
                alternativeTap = null
                currentInputConnection?.let(::commitTyped)
                sendKey(keyCode = key.code)
                updateAutomaticShift()
                true
            }

            key.code == KeyboardKeyCodes.DELETE -> handleBackspace()
            key.code == KeyboardKeyCodes.SHIFT -> {
                handleShift()
                true
            }

            key.code == KeyboardKeyCodes.MODE_ALPHA -> {
                showAlphabetLayout()
                true
            }

            key.code == KeyboardKeyCodes.SYMBOLS -> {
                showSymbolsLayout()
                true
            }

            key.code == KeyboardKeyCodes.MORE_SYMBOLS -> {
                showMoreSymbolsLayout()
                true
            }

            key.code == KeyboardKeyCodes.NUMBER_PAD -> {
                showNumberPadLayout()
                true
            }

            key.code == KeyboardKeyCodes.LANGUAGE -> {
                switchLanguage()
                true
            }

            else -> {
                handleCharacter(key = key)
                true
            }
        }

        refreshTextState()

        return handled
    }

    /**
     * Выполняет отдельное действие, выбранное в меню долгого нажатия.
     *
     * @param action выбранное действие.
     */
    private fun handleKeyLongPressAction(action: KeyLongPressAction) {
        when (action) {
            KeyLongPressAction.SHOW_EMOJI -> currentPanel.value = KeyboardPanel.EMOJI
        }
    }

    /**
     * Вставляет явно выбранный альтернативный символ.
     *
     * @param key исходная клавиша.
     * @param alternative выбранный символ.
     */
    private fun onAlternativeSelected(
        key: KeyInfo,
        alternative: String
    ) {
        performKeyFeedback()
        alternativeTap = null

        insertText(
            text = alternative,
            useComposingRegion = shouldUseComposingRegion(
                sdkInt = Build.VERSION.SDK_INT,
                predictionEnabled = predictionEnabled,
                inputClass = inputClass
            )
        )

        if (shiftState == ShiftState.ONESHOT) {
            shiftState = ShiftState.OFF
            publishCurrentMode()
        }

        refreshTextState()
    }

    /**
     * Вставляет печатаемую клавишу и поддерживает последовательный перебор её вариантов.
     *
     * @param key нажатая печатаемая клавиша.
     */
    private fun handleCharacter(key: KeyInfo) {
        val label = key.label ?: key.code.toChar().toString()
        val now = System.currentTimeMillis()
        val previousTap = alternativeTap
        val canCycleAlternative = key.tapAlternatives.size > 1 &&
            previousTap != null &&
            previousTap.keyCode == key.code &&
            now - previousTap.timestampMillis <= AlternativeTapTimeoutMillis

        if (canCycleAlternative) {
            val nextIndex = (previousTap.alternativeIndex + 1) % key.tapAlternatives.size
            val previousAlternative = key.tapAlternatives[previousTap.alternativeIndex]
            val nextAlternative = key.tapAlternatives[nextIndex]

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
                useComposingRegion = shouldUseComposingRegion(
                    sdkInt = Build.VERSION.SDK_INT,
                    predictionEnabled = predictionEnabled,
                    inputClass = inputClass
                )
            )

            alternativeTap = if (key.tapAlternatives.size > 1) {
                AlternativeTap(
                    keyCode = key.code,
                    alternativeIndex = key.tapAlternatives.indexOf(label).coerceAtLeast(0),
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

    /**
     * Передаёт [text] редактору как составной либо окончательно закреплённый текст.
     *
     * @param text вставляемый текст.
     * @param useComposingRegion передавать ли текст как незавершённое слово.
     */
    private fun insertText(
        text: String,
        useComposingRegion: Boolean
    ) {
        val inputConnection = currentInputConnection ?: return

        if (useComposingRegion) {
            composingText.append(text)

            /*
             * InputConnection принимает CharSequence, но конкретный редактор может
             * обработать его после возвращения из вызова. Передаём неизменяемый
             * снимок, чтобы последующее изменение общего буфера не меняло уже
             * отправленное состояние составного текста.
             */
            inputConnection.setComposingText(composingText.toString(), 1)
        } else {
            inputConnection.commitText(text, 1)
        }
    }

    /**
     * Заменяет последний введённый вариант при повторном коротком нажатии клавиши.
     *
     * @param previousText ранее вставленный вариант.
     * @param replacementText следующий вариант.
     */
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
            inputConnection.setComposingText(composingText.toString(), 1)
        } else {
            inputConnection.deleteSurroundingTextInCodePoints(
                previousText.codePointCount(0, previousText.length),
                0
            )
            inputConnection.commitText(replacementText, 1)
        }
    }

    /**
     * Закрепляет составное слово и вставляет готовую строку из вспомогательной панели.
     *
     * @param text строка эмодзи, реакции или буфера обмена.
     */
    private fun commitDirectText(text: String) {
        val inputConnection = currentInputConnection ?: return

        performKeyFeedback()
        commitTyped(inputConnection = inputConnection)
        inputConnection.commitText(text, 1)
        alternativeTap = null
        refreshTextState()
    }

    /**
     * Передаёт выбранный стикер, если поле объявило поддержку изображений.
     *
     * @param sticker выбранный элемент каталога.
     */
    private fun commitSticker(sticker: Sticker) {
        if (!supportsStickerContent) {
            return
        }

        val inputConnection = currentInputConnection ?: return

        performKeyFeedback()
        commitTyped(inputConnection = inputConnection)
        stickerContentSender.commit(
            inputConnection = inputConnection,
            sticker = sticker
        )
    }

    /**
     * Заменяет текущее слово выбранной словарной подсказкой и добавляет пробел.
     *
     * @param suggestion выбранный словарный вариант.
     */
    private fun commitSuggestion(suggestion: String) {
        val inputConnection = currentInputConnection ?: return
        val completionIndex = currentSuggestions.value.indexOf(suggestion)
        val completion = completions?.getOrNull(completionIndex)

        performKeyFeedback()

        if (
            completionEnabled &&
            completion != null
        ) {
            /*
             * Приложение передаёт CompletionInfo до преобразования регистра. Создаём
             * равнозначный вариант с отображаемым текстом, сохраняя его идентификатор
             * и положение, чтобы выбранное слово вставлялось именно в показанном виде.
             */
            inputConnection.commitCompletion(
                CompletionInfo(
                    completion.id,
                    completion.position,
                    suggestion,
                    completion.label
                )
            )
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

    /**
     * Закрепляет накопленную составную область в редакторе.
     *
     * @param inputConnection соединение с текущим полем.
     */
    private fun commitTyped(inputConnection: InputConnection) {
        if (composingText.isEmpty()) {
            return
        }

        inputConnection.commitText(composingText.toString(), 1)
        composingText.clear()
    }

    /**
     * Удаляет выделение, последнюю кодовую точку составного текста или символ перед курсором.
     *
     * @return `true`, если редактор действительно был изменён.
     */
    private fun handleBackspace(): Boolean {
        val inputConnection = currentInputConnection ?: return false
        alternativeTap = null
        val selectedText = inputConnection.getSelectedText(0)

        val deleted = when {
            composingText.isNotEmpty() -> {
                val lastCodePointStart = composingText.offsetByCodePoints(
                    composingText.length,
                    -1
                )
                composingText.delete(lastCodePointStart, composingText.length)

                if (composingText.isEmpty()) {
                    inputConnection.finishComposingText()
                } else {
                    inputConnection.setComposingText(composingText.toString(), 1)
                }

                true
            }

            !selectedText.isNullOrEmpty() -> {
                inputConnection.commitText("", 1)
            }

            !inputConnection.getTextBeforeCursor(1, 0).isNullOrEmpty() -> {
                inputConnection.deleteSurroundingTextInCodePoints(1, 0)
            }

            else -> false
        }

        if (deleted) {
            updateAutomaticShift()
        }

        return deleted
    }

    /** Переключает обычный, одноразовый верхний регистр и Caps Lock. */
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

    /** Согласует одноразовый верхний регистр с правилами текущего редактора. */
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

    /** Возвращает выбранную буквенную раскладку. */
    private fun showAlphabetLayout() {
        layoutMode = LayoutMode.ALPHA
        shiftState = ShiftState.OFF
        currentPanel.value = KeyboardPanel.KEYS
        publishLayout(baseLayout)
        updateAutomaticShift()
    }

    /** Открывает первую страницу символов. */
    private fun showSymbolsLayout() {
        layoutMode = LayoutMode.SYMBOLS1
        shiftState = ShiftState.OFF
        currentPanel.value = KeyboardPanel.KEYS
        publishLayout(KeyboardLayouts.Symbols)
    }

    /** Открывает вторую страницу символов только с первой страницы. */
    private fun showMoreSymbolsLayout() {
        if (layoutMode != LayoutMode.SYMBOLS1) {
            return
        }

        layoutMode = LayoutMode.SYMBOLS2
        shiftState = ShiftState.OFF
        publishLayout(KeyboardLayouts.Symbols2)
    }

    /** Открывает цифровую раскладку. */
    private fun showNumberPadLayout() {
        layoutMode = LayoutMode.NUMBERS
        shiftState = ShiftState.OFF
        currentPanel.value = KeyboardPanel.KEYS
        publishLayout(KeyboardLayouts.Numbers)
    }

    /** Переключает русскую и английскую буквенные раскладки. */
    private fun switchLanguage() {
        baseLayout = if (baseLayout == KeyboardLayouts.Russian) {
            KeyboardLayouts.English
        } else {
            KeyboardLayouts.Russian
        }

        layoutMode = LayoutMode.ALPHA
        shiftState = ShiftState.OFF
        currentPanel.value = KeyboardPanel.KEYS
        publishCurrentMode()
        updateAutomaticShift()
    }

    /** Публикует раскладку, соответствующую текущему режиму. */
    private fun publishCurrentMode() {
        val layout = when (layoutMode) {
            LayoutMode.ALPHA -> baseLayout
            LayoutMode.SYMBOLS1 -> KeyboardLayouts.Symbols
            LayoutMode.SYMBOLS2 -> KeyboardLayouts.Symbols2
            LayoutMode.NUMBERS -> KeyboardLayouts.Numbers
        }

        publishLayout(layout)
    }

    /**
     * Применяет регистр и действие редактора перед передачей раскладки в Compose.
     *
     * @param layout исходная раскладка выбранного режима.
     */
    private fun publishLayout(layout: List<List<KeyInfo>>) {
        val withCaps = applyCaps(
            layout = layout,
            enabled = shiftState.isCapsEnabled
        )
        currentLayout.value = applyEditorAction(layout = withCaps)
    }

    /**
     * Преобразует буквы и их альтернативы в требуемый регистр.
     *
     * @param layout исходные ряды клавиш.
     * @param enabled использовать ли верхний регистр.
     */
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
                        val transformedTapAlternatives =
                            key.tapAlternatives.map { alternative ->
                                if (enabled) {
                                    alternative.uppercase(Locale.getDefault())
                                } else {
                                    alternative.lowercase(Locale.getDefault())
                                }
                            }

                        key.copy(
                            code = transformedLabel.first().code,
                            label = transformedLabel,
                            alternatives = transformedAlternatives,
                            tapAlternatives = transformedTapAlternatives
                        )
                    }

                    else -> key
                }
            }
        }
    }

    /**
     * Настраивает Enter по действию, которое запросило текущее поле ввода.
     *
     * @param layout раскладка с исходным Enter.
     */
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

    /**
     * Вставляет печатаемый код либо передаёт Enter специальному обработчику.
     *
     * @param keyCode код печатаемого символа или Enter.
     */
    private fun sendKey(keyCode: Int) {
        if (keyCode == KeyboardKeyCodes.ENTER) {
            sendEditorAction()
            return
        }

        currentInputConnection?.commitText(keyCode.toChar().toString(), 1)
    }

    /** Выполняет заявленное действие редактора или отправляет обычный Enter. */
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

    /**
     * Отправляет редактору законченную пару событий нажатия аппаратной клавиши.
     *
     * @param keyEventCode код аппаратной клавиши Android.
     */
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

    /** Перечитывает контекст перед курсором и публикует подходящие подсказки. */
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
        val currentWord = SuggestionEngine.resolveCurrentWord(
            textBeforeCursor = textBeforeCursor,
            composingText = composingText
        )

        val suggestions = when {
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

        currentSuggestions.value = suggestions.withSuggestionLetterCase(
            letterCase = shiftState.suggestionLetterCase,
            currentWord = currentWord,
            locale = Locale.getDefault()
        )
    }

    /**
     * Переключает панели или выполняет действие верхней строки клавиатуры.
     *
     * @param action выбранное действие.
     */
    private fun handleToolbarAction(action: KeyboardToolbarAction) {
        when (action) {
            KeyboardToolbarAction.OPEN_ACTIONS -> {
                currentPanel.value = KeyboardPanel.ACTIONS
            }

            KeyboardToolbarAction.SHARE_APP -> shareApplication()

            KeyboardToolbarAction.NEXT_LANGUAGE -> switchLanguage()

            KeyboardToolbarAction.EMOJI -> {
                currentPanel.value = KeyboardPanel.EMOJI
            }

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

    /** Открывает системное меню отправки ссылки на страницу приложения. */
    private fun shareApplication() {
        currentPanel.value = KeyboardPanel.KEYS
        startActivity(
            createShareApplicationIntent(packageName = packageName)
        )
    }

    /** Читает первый текстовый элемент буфера обмена с ограничением безопасной длины. */
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

    /** Открывает системный список клавиатур из контекста службы. */
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

    /** Закрывает вспомогательную панель и возвращается к клавишам. */
    private fun showLettersPanel() {
        currentPanel.value = KeyboardPanel.KEYS
    }

    /** Перечитывает параметры звука и вибрации, общие с главным окном. */
    private fun refreshFeedbackPreferences() {
        isKeyboardSoundFeedbackEnabled =
            preferences.isKeyboardSoundFeedbackEnabled
        isKeyboardHapticFeedbackEnabled.value =
            preferences.isKeyboardHapticFeedbackEnabled
    }

    /** Воспроизводит разрешённые пользователем звук и вибрацию нажатия. */
    private fun performKeyFeedback() {
        if (isKeyboardSoundFeedbackEnabled) {
            audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD)
        }

        if (
            !isKeyboardHapticFeedbackEnabled.value ||
            !vibrator.hasVibrator()
        ) {
            return
        }

        vibrator.vibrate(createKeyVibrationEffect())
    }

    /** Создаёт наиболее точный доступный на устройстве короткий вибрационный отклик. */
    private fun createKeyVibrationEffect(): VibrationEffect {
        /*
         * На Android 11 и новее композиция позволяет масштабировать системный
         * короткий эффект без потери его чёткости. На остальных устройствах
         * уменьшаем амплитуду, а при отсутствии аппаратного управления ею —
         * длительность импульса. Все три варианта дают примерно треть прежней силы.
         */
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            vibrator.areAllPrimitivesSupported(
                VibrationEffect.Composition.PRIMITIVE_TICK
            )
        ) {
            return VibrationEffect.startComposition()
                .addPrimitive(
                    VibrationEffect.Composition.PRIMITIVE_TICK,
                    KeyVibrationScale
                )
                .compose()
        }

        return if (vibrator.hasAmplitudeControl()) {
            VibrationEffect.createOneShot(
                KeyVibrationDurationMillis,
                KeyVibrationAmplitude
            )
        } else {
            VibrationEffect.createOneShot(
                KeyVibrationFallbackDurationMillis,
                VibrationEffect.DEFAULT_AMPLITUDE
            )
        }
    }

    /**
     * Проверяет, завершает ли код текущее слово.
     *
     * @param code код клавиши.
     */
    private fun isWordSeparator(code: Int): Boolean {
        return code > 0 && wordSeparators.contains(code.toChar())
    }

    /**
     * Состояние перебора вариантов повторными короткими нажатиями.
     *
     * @property keyCode код клавиши, варианты которой перебираются.
     * @property alternativeIndex индекс последнего вставленного варианта.
     * @property timestampMillis время последнего нажатия.
     */
    private data class AlternativeTap(
        val keyCode: Int,
        val alternativeIndex: Int,
        val timestampMillis: Long
    )

    /** Состояния регистра буквенной раскладки. */
    private enum class ShiftState {
        OFF,
        ONESHOT,
        CAPS_LOCK;

        /** Нужно ли сейчас показывать и вводить прописные буквы. */
        val isCapsEnabled: Boolean
            get() = this != OFF

        /** Регистр, который следует применить к словарным подсказкам. */
        val suggestionLetterCase: SuggestionLetterCase
            get() = when (this) {
                OFF -> SuggestionLetterCase.UNCHANGED
                ONESHOT -> SuggestionLetterCase.INITIAL_UPPERCASE
                CAPS_LOCK -> SuggestionLetterCase.UPPERCASE
            }
    }

    /** Логические страницы клавиатуры. */
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
        const val KeyVibrationDurationMillis = 10L
        const val KeyVibrationFallbackDurationMillis = 3L
        const val KeyVibrationAmplitude = 85
        const val KeyVibrationScale = 0.33f
    }
}

/**
 * Решает, безопасно ли хранить незавершённое слово в составной области редактора.
 *
 * @param sdkInt версия Android, передаваемая отдельно для воспроизводимой проверки.
 * @param predictionEnabled разрешены ли подсказки для текущего поля.
 * @param inputClass основной класс поля ввода.
 */
internal fun shouldUseComposingRegion(
    sdkInt: Int,
    predictionEnabled: Boolean,
    inputClass: Int
): Boolean {
    /*
     * На Android 9 и старше поле Compose может завершить составную область,
     * не уведомив метод ввода. Следующий разделитель тогда повторно вставляет
     * уже отображённое слово. На этих версиях закрепляем каждый символ сразу;
     * начиная с Android 10 сохраняем обычное составное выделение слова.
     */
    return sdkInt >= Build.VERSION_CODES.Q &&
        predictionEnabled &&
        inputClass == InputType.TYPE_CLASS_TEXT
}

/**
 * Создаёт системное меню отправки ссылки на приложение в Google Play.
 *
 * Результат уже содержит флаг новой задачи, обязательный для запуска из службы.
 *
 * @param packageName имя пакета приложения.
 */
internal fun createShareApplicationIntent(packageName: String): Intent {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = PlainTextMimeType
        putExtra(
            Intent.EXTRA_TEXT,
            "$GooglePlayWebUrl$packageName"
        )
    }

    /*
     * Метод ввода работает как служба, поэтому системное меню отправки должно
     * открываться в новой задаче, а не ожидать Activity-контекст.
     */
    return Intent.createChooser(shareIntent, ShareApplicationTitle).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

private const val GooglePlayWebUrl =
    "https://play.google.com/store/apps/details?id="
private const val PlainTextMimeType = "text/plain"
private const val ShareApplicationTitle = "Рассказать об Ижице"
