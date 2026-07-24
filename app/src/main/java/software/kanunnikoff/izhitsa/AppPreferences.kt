package software.kanunnikoff.izhitsa

import android.content.Context
import androidx.core.content.edit

/**
 * Предоставляет типизированный доступ к пользовательским настройкам приложения.
 *
 * Все значения хранятся в одном закрытом наборе [android.content.SharedPreferences],
 * чтобы служба клавиатуры и основное окно использовали одинаковое состояние.
 *
 * @param context контекст приложения или компонента, из которого доступны настройки.
 */
class AppPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(
        PreferencesFileName,
        Context.MODE_PRIVATE
    )

    /** Включено ли отображение текстов интерфейса в дореформенной орфографии. */
    var usesPreRevolutionaryOrthography: Boolean
        get() = preferences.getBoolean(PreRevolutionaryOrthographyKey, false)
        set(value) {
            preferences.edit {
                putBoolean(PreRevolutionaryOrthographyKey, value)
            }
        }

    /** Включён ли системный звук при нажатии клавиш. */
    var isKeyboardSoundFeedbackEnabled: Boolean
        get() = preferences.getBoolean(KeyboardSoundFeedbackKey, false)
        set(value) {
            preferences.edit {
                putBoolean(KeyboardSoundFeedbackKey, value)
            }
        }

    /** Включена ли вибрация при нажатии клавиш. */
    var isKeyboardHapticFeedbackEnabled: Boolean
        get() = preferences.getBoolean(KeyboardHapticFeedbackKey, false)
        set(value) {
            preferences.edit {
                putBoolean(KeyboardHapticFeedbackKey, value)
            }
        }

    /** Пользовался ли владелец устройства клавиатурой хотя бы один раз. */
    var hasUsedKeyboard: Boolean
        get() = preferences.getBoolean(HasUsedKeyboardKey, false)
        set(value) {
            preferences.edit {
                putBoolean(HasUsedKeyboardKey, value)
            }
        }

    /** Число запусков приложения, используемое для выбора момента запроса оценки. */
    var launchesCount: Int
        get() = preferences.getInt(LaunchesCountKey, 0)
        set(value) {
            preferences.edit {
                putInt(LaunchesCountKey, value)
            }
        }

    /** Версия приложения, для которой уже выполнялась попытка запросить оценку. */
    var lastReviewVersion: String
        get() = preferences.getString(LastReviewVersionKey, "").orEmpty()
        set(value) {
            preferences.edit {
                putString(LastReviewVersionKey, value)
            }
        }

    companion object {
        private const val PreferencesFileName = "izhitsa_settings"
        private const val PreRevolutionaryOrthographyKey = "app_uses_pre_revolutionary_orthography"
        private const val KeyboardSoundFeedbackKey = "keyboard_sound_feedback_enabled"
        private const val KeyboardHapticFeedbackKey = "keyboard_haptic_feedback_enabled"
        private const val HasUsedKeyboardKey = "keyboard_has_been_used"
        private const val LaunchesCountKey = "launches_count"
        private const val LastReviewVersionKey = "last_review_version"
    }
}
