package software.kanunnikoff.izhitsa.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Проверяет содержимое раскладок и доступность вариантов долгого нажатия. */
class KeyboardLayoutsTest {
    /** Буквенные раскладки открывают эмодзи удержанием запятой. */
    @Test
    fun `буквенные раскладки открывают эмодзи долгим удержанием запятой`() {
        val layouts = listOf(
            KeyboardLayouts.Russian,
            KeyboardLayouts.English
        )

        layouts.forEach { layout ->
            val commaKey = layout
                .flatten()
                .single { key -> key.label == "," }

            assertEquals(",".first().code, commaKey.code)
            assertEquals(KeyIcon.EMOJI, commaKey.hintIcon)
            assertEquals(KeyLongPressAction.SHOW_EMOJI, commaKey.longPressAction)
        }
    }

    /** Символьная запятая остаётся обычным знаком без действия эмодзи. */
    @Test
    fun `на символьной раскладке запятая не содержит эмодзи`() {
        val commaKey = KeyboardLayouts.Symbols
            .flatten()
            .single { key -> key.label == "," }

        assertNull(commaKey.hintIcon)
        assertNull(commaKey.longPressAction)
    }

    /** Эмодзи не занимают отдельную клавишу ни в одной основной раскладке. */
    @Test
    fun `в раскладках нет отдельной клавиши эмодзи`() {
        val layouts = listOf(
            KeyboardLayouts.Russian,
            KeyboardLayouts.English,
            KeyboardLayouts.Symbols,
            KeyboardLayouts.Symbols2
        )

        layouts.forEach { layout ->
            assertFalse(
                layout
                    .flatten()
                    .any { key -> key.icon == KeyIcon.EMOJI }
            )
        }
    }

    /** Русский пробел использует историческое написание названия языка. */
    @Test
    fun `русская клавиша пробела подписана в дореформенной орфографии`() {
        val spaceKey = KeyboardLayouts.Russian
            .flatten()
            .single { key -> key.code == KeyboardKeyCodes.SPACE }

        assertEquals("Русскій", spaceKey.label)
    }

    /** Цифры верхнего ряда доступны удержанием соответствующих букв. */
    @Test
    fun `цифровые подсказки верхнего ряда доступны по удержанию в буквенных раскладках`() {
        val alphabeticLayouts = listOf(
            KeyboardLayouts.Russian,
            KeyboardLayouts.English
        )

        alphabeticLayouts.forEach { layout ->
            val hintedKeys = layout
                .first()
                .filter { key -> key.hint != null }

            assertEquals(10, hintedKeys.size)

            hintedKeys.forEach { key ->
                val hint = requireNotNull(key.hint)

                assertTrue(key.alternatives.contains(hint))
                assertEquals(
                    hint,
                    key.alternatives[requireNotNull(key.preferredAlternativeIndex)]
                )
            }
        }
    }

    /** Каждая русская гласная содержит вариант с комбинируемым ударением. */
    @Test
    fun `все русские гласные содержат вариант под ударением`() {
        val combiningAcuteAccent = "\u0301"
        val vowels = listOf("а", "е", "и", "о", "у", "ы", "э", "ю", "я")
        val keysByLabel = KeyboardLayouts.Russian
            .flatten()
            .associateBy(KeyInfo::label)

        vowels.forEach { vowel ->
            val key = requireNotNull(keysByLabel[vowel])

            assertTrue(key.alternatives.contains(vowel + combiningAcuteAccent))
        }
    }

    /** Вариант клавиши «и» содержит кириллическую, а не латинскую букву. */
    @Test
    fun `клавиша и содержит кириллическую і десятеричную`() {
        val iKey = KeyboardLayouts.Russian
            .flatten()
            .single { key -> key.label == "и" }

        assertTrue(iKey.alternatives.contains("і"))
        assertTrue(iKey.tapAlternatives.contains("і"))
        assertFalse(iKey.alternatives.contains("i"))
        assertFalse(iKey.tapAlternatives.contains("i"))
    }

    /** Удержание удаления включено во всех раскладках. */
    @Test
    fun `удержание Backspace повторяется во всех раскладках`() {
        val layouts = listOf(
            KeyboardLayouts.Russian,
            KeyboardLayouts.English,
            KeyboardLayouts.Symbols,
            KeyboardLayouts.Symbols2,
            KeyboardLayouts.Numbers
        )

        layouts.forEach { layout ->
            val backspaceKey = layout
                .flatten()
                .single { key -> key.code == KeyboardKeyCodes.DELETE }

            assertTrue(backspaceKey.repeatOnLongPress)
        }
    }

    /** Меню точки совпадает с принятой двухрядной геометрией и порядком знаков. */
    @Test
    fun `точка содержит двухрядную панель знаков Gboard`() {
        val expectedAlternatives = listOf(
            "&", "%", "+", "\"", "-", ":", "'", "@",
            ";", "/", "(", ")", "#", "!", ",", "?"
        )
        val periodKeys = listOf(
            KeyboardLayouts.Russian,
            KeyboardLayouts.English,
            KeyboardLayouts.Symbols
        ).map { layout ->
            layout
                .flatten()
                .single { key -> key.label == "." }
        }

        periodKeys.forEach { key ->
            assertEquals(expectedAlternatives, key.alternatives)
            assertEquals(listOf(8, 8), key.alternativeRowLengths)
            assertEquals(14, key.preferredAlternativeIndex)
        }
    }

    /** Обе символьные страницы содержат дополнительные валютные и типографские знаки. */
    @Test
    fun `символьные панели содержат эталонные дополнительные знаки`() {
        val firstSymbols = KeyboardLayouts.Symbols
            .flatten()
            .associateBy(KeyInfo::label)
        val secondSymbols = KeyboardLayouts.Symbols2
            .flatten()
            .associateBy(KeyInfo::label)

        assertEquals(
            listOf("₹", "¥", "₽", "€", "$", "¢", "£"),
            firstSymbols.getValue("₽").alternatives
        )
        assertEquals(
            listOf("“", "„", "”", "«", "»"),
            firstSymbols.getValue("\"").alternatives
        )
        assertEquals(
            listOf("♣", "♠", "♪", "♥", "♦"),
            secondSymbols.getValue("•").alternatives
        )
        assertEquals(
            listOf("∞", "≠", "≈"),
            secondSymbols.getValue("=").alternatives
        )
        assertEquals(
            listOf("‰", "‱"),
            secondSymbols.getValue("%").alternatives
        )
    }
}
