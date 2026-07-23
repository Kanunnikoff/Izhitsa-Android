package software.kanunnikoff.izhitsa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.nio.channels.FileChannel

class SuggestionEngineTest {
    @Test
    fun suggestionsKeepTypedWordFirstAndCompleteKnownPrefix() {
        val suggestions = SuggestionEngine.suggestionsFor(
            textBeforeCursor = "карп",
            composingText = "карп"
        )

        assertEquals(
            listOf("карп", "карпов", "Карпаты"),
            suggestions
        )
    }

    @Test
    fun suggestionsOfferNextWordsAfterSeparator() {
        val suggestions = SuggestionEngine.suggestionsFor(
            textBeforeCursor = "готово ",
            composingText = ""
        )

        assertEquals(
            listOf("и", "в", "на"),
            suggestions
        )
    }

    @Test
    fun unknownWordStillProducesThreeToolbarItems() {
        val suggestions = SuggestionEngine.suggestionsFor(
            textBeforeCursor = "karp",
            composingText = null
        )

        assertEquals(
            listOf("karp", "Karp", "и"),
            suggestions
        )
    }

    @Test
    fun currentWordIsExtractedAfterPunctuation() {
        assertEquals(
            "при",
            SuggestionEngine.extractCurrentWord("Здравствуйте, при")
        )
    }

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
}
