package software.kanunnikoff.izhitsa

import android.os.Build
import android.text.InputType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardInputPolicyTest {
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
