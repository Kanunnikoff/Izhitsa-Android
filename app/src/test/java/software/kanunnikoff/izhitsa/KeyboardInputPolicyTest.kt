package software.kanunnikoff.izhitsa

import android.os.Build
import android.text.InputType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Проверяет выбор безопасного режима передачи незавершённого слова редактору. */
class KeyboardInputPolicyTest {
    /** Android 9 не должен использовать ненадёжную составную область. */
    @Test
    fun androidNineDoesNotUseUnreliableComposingRegion() {
        assertFalse(
            shouldUseComposingRegion(
                sdkInt = Build.VERSION_CODES.P,
                predictionEnabled = true,
                inputClass = InputType.TYPE_CLASS_TEXT
            )
        )
    }

    /** Начиная с Android 10 текстовое поле с подсказками использует составную область. */
    @Test
    fun androidTenUsesComposingRegionForPredictiveText() {
        assertTrue(
            shouldUseComposingRegion(
                sdkInt = Build.VERSION_CODES.Q,
                predictionEnabled = true,
                inputClass = InputType.TYPE_CLASS_TEXT
            )
        )
    }

    /** Отключённые подсказки исключают составную область независимо от версии Android. */
    @Test
    fun composingRegionIsNotUsedWhenPredictionsAreDisabled() {
        assertFalse(
            shouldUseComposingRegion(
                sdkInt = Build.VERSION_CODES.Q,
                predictionEnabled = false,
                inputClass = InputType.TYPE_CLASS_TEXT
            )
        )
    }

    /** Числовое поле не должно получать составной текст. */
    @Test
    fun composingRegionIsNotUsedForNonTextInput() {
        assertFalse(
            shouldUseComposingRegion(
                sdkInt = Build.VERSION_CODES.Q,
                predictionEnabled = true,
                inputClass = InputType.TYPE_CLASS_NUMBER
            )
        )
    }
}
