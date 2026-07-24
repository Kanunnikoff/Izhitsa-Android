package software.kanunnikoff.izhitsa

import java.util.Locale

/**
 * Формирует подсказки для незавершённого слова, набираемого пользователем.
 *
 * Объект не хранит состояние: источник слов передаётся через [SuggestionDictionary],
 * что позволяет одинаково использовать алгоритм в службе и модульных проверках.
 */
internal object SuggestionEngine {
    /**
     * Возвращает подсказки для слова непосредственно перед курсором.
     *
     * @param textBeforeCursor текст редактора перед курсором.
     * @param composingText слово, которое метод ввода ещё не закрепил.
     * @param dictionary источник словарных вариантов.
     */
    fun suggestionsFor(
        textBeforeCursor: CharSequence?,
        composingText: CharSequence?,
        dictionary: SuggestionDictionary? = null
    ): List<String> {
        val currentWord = resolveCurrentWord(
            textBeforeCursor = textBeforeCursor,
            composingText = composingText
        )

        if (currentWord.isBlank()) {
            return emptyList()
        }

        val normalizedWord = currentWord.lowercase(RussianLocale)

        return dictionary
            ?.suggestions(
                prefix = normalizedWord,
                limit = SuggestionCount
            )
            .orEmpty()
            .take(SuggestionCount)
    }

    /**
     * Определяет текущее слово с учётом активной составной области метода ввода.
     *
     * @param textBeforeCursor текст редактора перед курсором.
     * @param composingText незавершённое слово метода ввода.
     */
    fun resolveCurrentWord(
        textBeforeCursor: CharSequence?,
        composingText: CharSequence?
    ): String {
        return composingText
            ?.toString()
            ?.takeIf(String::isNotBlank)
            ?: extractCurrentWord(textBeforeCursor)
    }

    /**
     * Извлекает последнюю непрерывную последовательность букв из текста перед курсором.
     *
     * @param textBeforeCursor текст редактора перед курсором.
     */
    fun extractCurrentWord(textBeforeCursor: CharSequence?): String {
        return textBeforeCursor
            ?.toString()
            ?.takeLastWhile { character ->
                character.isLetter() || character == '\'' || character == '’'
            }
            .orEmpty()
    }

    private const val SuggestionCount = 3

    private val RussianLocale = Locale.forLanguageTag("ru-RU")
}

/**
 * Применяет требуемый регистр к подсказкам, не меняя их порядок.
 *
 * @receiver исходные словарные варианты.
 * @param letterCase режим регистра клавиатуры.
 * @param currentWord уже набранная часть слова, по которой сохраняется начальная прописная.
 * @param locale языковые правила преобразования регистра.
 */
internal fun List<String>.withSuggestionLetterCase(
    letterCase: SuggestionLetterCase,
    currentWord: CharSequence?,
    locale: Locale
): List<String> {
    /*
     * Одноразовый Shift выключается сразу после первой буквы. Если введённое
     * слово уже начинается с заглавной, сохраняем этот регистр у всех вариантов,
     * даже когда текущее состояние Shift успело вернуться в обычное.
     */
    val effectiveLetterCase = when {
        letterCase != SuggestionLetterCase.UNCHANGED -> letterCase
        currentWord?.firstOrNull()?.isUpperCase() == true -> {
            SuggestionLetterCase.INITIAL_UPPERCASE
        }

        else -> SuggestionLetterCase.UNCHANGED
    }

    return map { suggestion ->
        when (effectiveLetterCase) {
            SuggestionLetterCase.UNCHANGED -> suggestion

            SuggestionLetterCase.INITIAL_UPPERCASE -> {
                suggestion.replaceFirstChar { character ->
                    character.uppercase(locale)
                }
            }

            SuggestionLetterCase.UPPERCASE -> suggestion.uppercase(locale)
        }
    }
}

/** Режим преобразования регистра словарных подсказок. */
internal enum class SuggestionLetterCase {
    /** Словарное написание не изменяется. */
    UNCHANGED,

    /** Прописной становится только первая буква. */
    INITIAL_UPPERCASE,

    /** Все буквы преобразуются в прописные. */
    UPPERCASE
}
