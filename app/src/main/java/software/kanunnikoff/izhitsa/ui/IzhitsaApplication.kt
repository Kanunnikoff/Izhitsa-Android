package software.kanunnikoff.izhitsa.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Корневой интерфейс приложения-компаньона.
 *
 * Он хранит изменяемые настройки в состоянии Compose, а переданные обработчики
 * сразу сохраняют каждое изменение для службы клавиатуры.
 *
 * @param initialUsesPreRevolutionaryOrthography начальное оформление текстов приложения.
 * @param initialIsKeyboardSoundFeedbackEnabled начальное состояние звука клавиш.
 * @param initialIsKeyboardHapticFeedbackEnabled начальное состояние вибрации клавиш.
 * @param hasUsedKeyboard пользовался ли владелец клавиатурой хотя бы один раз.
 * @param versionName отображаемое имя версии.
 * @param versionCode внутренний номер сборки.
 * @param onUsesPreRevolutionaryOrthographyChanged обработчик изменения орфографии.
 * @param onKeyboardSoundFeedbackChanged обработчик изменения звука клавиш.
 * @param onKeyboardHapticFeedbackChanged обработчик изменения вибрации клавиш.
 * @param onSupportAuthor обработчик покупки чаевых.
 */
@Composable
fun IzhitsaApplication(
    initialUsesPreRevolutionaryOrthography: Boolean,
    initialIsKeyboardSoundFeedbackEnabled: Boolean,
    initialIsKeyboardHapticFeedbackEnabled: Boolean,
    hasUsedKeyboard: Boolean,
    versionName: String,
    versionCode: Long,
    onUsesPreRevolutionaryOrthographyChanged: (Boolean) -> Unit,
    onKeyboardSoundFeedbackChanged: (Boolean) -> Unit,
    onKeyboardHapticFeedbackChanged: (Boolean) -> Unit,
    onSupportAuthor: () -> Unit
) {
    var usesPreRevolutionaryOrthography by remember {
        mutableStateOf(initialUsesPreRevolutionaryOrthography)
    }
    var isKeyboardSoundFeedbackEnabled by remember {
        mutableStateOf(initialIsKeyboardSoundFeedbackEnabled)
    }
    var isKeyboardHapticFeedbackEnabled by remember {
        mutableStateOf(initialIsKeyboardHapticFeedbackEnabled)
    }
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val colorScheme = if (android.os.Build.VERSION.SDK_INT >= 31) {
        if (isDark) {
            dynamicDarkColorScheme(context)
        } else {
            dynamicLightColorScheme(context)
        }
    } else if (isDark) {
        darkColorScheme()
    } else {
        lightColorScheme()
    }
    MaterialTheme(colorScheme = colorScheme) {
        AppNavigation(
            usesPreRevolutionaryOrthography =
                usesPreRevolutionaryOrthography,
            isKeyboardSoundFeedbackEnabled =
                isKeyboardSoundFeedbackEnabled,
            isKeyboardHapticFeedbackEnabled =
                isKeyboardHapticFeedbackEnabled,
            hasUsedKeyboard = hasUsedKeyboard,
            versionName = versionName,
            versionCode = versionCode,
            onUsesPreRevolutionaryOrthographyChanged = { value ->
                usesPreRevolutionaryOrthography = value
                onUsesPreRevolutionaryOrthographyChanged(value)
            },
            onKeyboardSoundFeedbackChanged = { value ->
                isKeyboardSoundFeedbackEnabled = value
                onKeyboardSoundFeedbackChanged(value)
            },
            onKeyboardHapticFeedbackChanged = { value ->
                isKeyboardHapticFeedbackEnabled = value
                onKeyboardHapticFeedbackChanged(value)
            },
            onSupportAuthor = onSupportAuthor
        )
    }
}
