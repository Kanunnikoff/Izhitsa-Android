package software.kanunnikoff.izhitsa

import java.util.Locale

internal object SuggestionEngine {
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

    fun resolveCurrentWord(
        textBeforeCursor: CharSequence?,
        composingText: CharSequence?
    ): String {
        return composingText
            ?.toString()
            ?.takeIf(String::isNotBlank)
            ?: extractCurrentWord(textBeforeCursor)
    }

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

internal enum class SuggestionLetterCase {
    UNCHANGED,
    INITIAL_UPPERCASE,
    UPPERCASE
}
