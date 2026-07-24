package software.kanunnikoff.izhitsa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.nio.channels.FileChannel
import java.util.Locale

/** Проверяет извлечение текущего слова, поиск и преобразование регистра подсказок. */
class SuggestionEngineTest {
    private val russianLocale = Locale.forLanguageTag("ru-RU")
    private val testDictionary = SuggestionDictionary { prefix, limit ->
        listOf("карп", "карпов", "Карпаты")
            .filter { word ->
                word.lowercase(russianLocale).startsWith(prefix)
            }
            .take(limit)
    }

    /** Словарный порядок вариантов сохраняется для известного начала слова. */
    @Test
    fun suggestionsMatchDictionaryOrderForKnownPrefix() {
        val suggestions = SuggestionEngine.suggestionsFor(
            textBeforeCursor = "карп",
            composingText = "карп",
            dictionary = testDictionary
        )

        assertEquals(
            listOf("карп", "карпов", "Карпаты"),
            suggestions
        )
    }

    /** После разделителя текущего слова нет и подсказки не создаются. */
    @Test
    fun suggestionsAreEmptyAfterSeparator() {
        val suggestions = SuggestionEngine.suggestionsFor(
            textBeforeCursor = "готово ",
            composingText = ""
        )

        assertEquals(
            emptyList<String>(),
            suggestions
        )
    }

    /** Движок не придумывает варианты, отсутствующие в словаре. */
    @Test
    fun unknownWordDoesNotProduceArtificialSuggestions() {
        val suggestions = SuggestionEngine.suggestionsFor(
            textBeforeCursor = "karp",
            composingText = null,
            dictionary = testDictionary
        )

        assertEquals(
            emptyList<String>(),
            suggestions
        )
    }

    /** Знаки препинания перед словом не входят в искомое начало. */
    @Test
    fun currentWordIsExtractedAfterPunctuation() {
        assertEquals(
            "при",
            SuggestionEngine.extractCurrentWord("Здравствуйте, при")
        )
    }

    /** Встроенный двоичный словарь находится, разбирается и возвращает совпадения. */
    @Test
    fun bundledDictionaryFindsWordsByRussianPrefix() {
        val dictionaryFile = File("src/main/assets/russian.bin")
        val storage = FileInputStream(dictionaryFile).channel.use { channel ->
            RussianDictionaryStorage.parse(
                buffer = channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    0,
                    channel.size()
                )
            )
        }
        val suggestions = storage.suggestions(
            prefix = "карп",
            limit = 5
        )

        assertEquals(5, suggestions.size)
        assertTrue(
            suggestions.all { suggestion ->
                suggestion.lowercase().startsWith("карп")
            }
        )
    }

    /** Переданный источник может вернуть несколько продолжений уже набранного слова. */
    @Test
    fun externalDictionarySupplementsTypedWord() {
        val dictionary = SuggestionDictionary { prefix, limit ->
            listOf("карп", "карпа", "карпами")
                .filter { word -> word.startsWith(prefix) }
                .take(limit)
        }

        val suggestions = SuggestionEngine.suggestionsFor(
            textBeforeCursor = "карп",
            composingText = "карп",
            dictionary = dictionary
        )

        assertEquals(
            listOf("карп", "карпа", "карпами"),
            suggestions
        )
    }

    /** Одноразовый верхний регистр делает прописной только первую букву вариантов. */
    @Test
    fun oneShotShiftCapitalizesSuggestionInitials() {
        val suggestions = SuggestionEngine.suggestionsFor(
            textBeforeCursor = "карп",
            composingText = "карп",
            dictionary = testDictionary
        ).withSuggestionLetterCase(
            letterCase = SuggestionLetterCase.INITIAL_UPPERCASE,
            currentWord = "карп",
            locale = russianLocale
        )

        assertEquals(
            listOf("Карп", "Карпов", "Карпаты"),
            suggestions
        )
    }

    /** Caps Lock преобразует варианты полностью в верхний регистр. */
    @Test
    fun capsLockUppercasesEntireSuggestions() {
        val suggestions = SuggestionEngine.suggestionsFor(
            textBeforeCursor = "карп",
            composingText = "карп",
            dictionary = testDictionary
        ).withSuggestionLetterCase(
            letterCase = SuggestionLetterCase.UPPERCASE,
            currentWord = "карп",
            locale = russianLocale
        )

        assertEquals(
            listOf("КАРП", "КАРПОВ", "КАРПАТЫ"),
            suggestions
        )
    }

    /** Начальная прописная сохраняется после автоматического выключения Shift. */
    @Test
    fun typedInitialUppercaseIsPreservedAfterOneShotShiftTurnsOff() {
        val currentWord = "Карп"
        val suggestions = SuggestionEngine.suggestionsFor(
            textBeforeCursor = currentWord,
            composingText = currentWord,
            dictionary = testDictionary
        ).withSuggestionLetterCase(
            letterCase = SuggestionLetterCase.UNCHANGED,
            currentWord = currentWord,
            locale = russianLocale
        )

        assertEquals(
            listOf("Карп", "Карпов", "Карпаты"),
            suggestions
        )
    }
}
