package software.kanunnikoff.izhitsa

import org.junit.Assert.assertEquals
import org.junit.Test

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
}
