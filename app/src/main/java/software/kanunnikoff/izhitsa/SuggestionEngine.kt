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
            return NextWordSuggestions
        }

        val normalizedWord = currentWord.lowercase(RussianLocale)
        val suggestions = linkedSetOf(currentWord)

        dictionary
            ?.suggestions(
                prefix = normalizedWord,
                limit = SuggestionCount
            )
            ?.forEach { candidate ->
                if (!candidate.equals(currentWord, ignoreCase = true)) {
                    suggestions += candidate
                }
            }

        RussianLexicon
            .asSequence()
            .filter { candidate ->
                suggestions.size < SuggestionCount &&
                    candidate.lowercase(RussianLocale).startsWith(normalizedWord) &&
                    !candidate.equals(currentWord, ignoreCase = true)
            }
            .forEach { candidate ->
                suggestions += candidate
            }

        if (suggestions.size < SuggestionCount) {
            suggestions += currentWord.replaceFirstChar { character ->
                character.titlecase(RussianLocale)
            }
        }

        NextWordSuggestions.forEach { suggestion ->
            if (suggestions.size < SuggestionCount) {
                suggestions += suggestion
            }
        }

        return suggestions.take(SuggestionCount)
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

    private val NextWordSuggestions = listOf("и", "в", "на")

    /*
     * Небольшой встроенный словарь служит надёжным запасным вариантом, когда
     * приложение-получатель не передаёт собственные варианты автодополнения.
     * Первым всегда остаётся введённое пользователем слово.
     */
    private val RussianLexicon = listOf(
        "благодарю",
        "больше",
        "будет",
        "быть",
        "вас",
        "всего",
        "где",
        "да",
        "для",
        "доброе",
        "есть",
        "ещё",
        "здравствуйте",
        "как",
        "карп",
        "карпов",
        "Карпаты",
        "когда",
        "который",
        "можно",
        "мы",
        "написать",
        "нет",
        "нужно",
        "пожалуйста",
        "почему",
        "привет",
        "спасибо",
        "так",
        "текст",
        "хорошо",
        "чтобы",
        "это"
    )
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
