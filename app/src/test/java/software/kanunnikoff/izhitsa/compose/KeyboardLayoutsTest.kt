package software.kanunnikoff.izhitsa.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class KeyboardLayoutsTest {
    @Test
    fun `раскладки с эмодзи открывают их долгим удержанием запятой`() {
        val layouts = listOf(
            KeyboardLayouts.Russian,
            KeyboardLayouts.English,
            KeyboardLayouts.Symbols
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
}
