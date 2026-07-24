package software.kanunnikoff.izhitsa

import android.content.Context
import androidx.core.content.edit

class AppPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(
        PreferencesFileName,
        Context.MODE_PRIVATE
    )

    var usesPreRevolutionaryOrthography: Boolean
        get() = preferences.getBoolean(PreRevolutionaryOrthographyKey, false)
        set(value) {
            preferences.edit {
                putBoolean(PreRevolutionaryOrthographyKey, value)
            }
        }

    var isKeyboardSoundFeedbackEnabled: Boolean
        get() = preferences.getBoolean(KeyboardSoundFeedbackKey, false)
        set(value) {
            preferences.edit {
                putBoolean(KeyboardSoundFeedbackKey, value)
            }
        }

    var isKeyboardHapticFeedbackEnabled: Boolean
        get() = preferences.getBoolean(KeyboardHapticFeedbackKey, false)
        set(value) {
            preferences.edit {
                putBoolean(KeyboardHapticFeedbackKey, value)
            }
        }

    var hasUsedKeyboard: Boolean
        get() = preferences.getBoolean(HasUsedKeyboardKey, false)
        set(value) {
            preferences.edit {
                putBoolean(HasUsedKeyboardKey, value)
            }
        }

    var launchesCount: Int
        get() = preferences.getInt(LaunchesCountKey, 0)
        set(value) {
            preferences.edit {
                putInt(LaunchesCountKey, value)
            }
        }

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
