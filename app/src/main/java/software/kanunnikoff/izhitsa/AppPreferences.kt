package software.kanunnikoff.izhitsa

import android.content.Context
import androidx.core.content.edit

class AppPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(
        PreferencesFileName,
        Context.MODE_PRIVATE
    )

    var usesSystemFont: Boolean
        get() = preferences.getBoolean(SystemFontKey, true)
        set(value) {
            preferences.edit {
                putBoolean(SystemFontKey, value)
            }
        }

    var usesPreRevolutionaryOrthography: Boolean
        get() = preferences.getBoolean(PreRevolutionaryOrthographyKey, false)
        set(value) {
            preferences.edit {
                putBoolean(PreRevolutionaryOrthographyKey, value)
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
        private const val SystemFontKey = "app_uses_system_font"
        private const val PreRevolutionaryOrthographyKey = "app_uses_pre_revolutionary_orthography"
        private const val HasUsedKeyboardKey = "keyboard_has_been_used"
        private const val LaunchesCountKey = "launches_count"
        private const val LastReviewVersionKey = "last_review_version"
    }
}
