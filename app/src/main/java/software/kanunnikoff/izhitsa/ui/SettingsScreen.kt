package software.kanunnikoff.izhitsa.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Позволяет изменить орфографию интерфейса, звук и вибрацию клавиатуры.
 *
 * @param usesPreRevolutionaryOrthography используется ли дореформенное написание.
 * @param isKeyboardSoundFeedbackEnabled включён ли звук клавиш.
 * @param isKeyboardHapticFeedbackEnabled включена ли вибрация клавиш.
 * @param onUsesPreRevolutionaryOrthographyChanged обработчик изменения орфографии.
 * @param onKeyboardSoundFeedbackChanged обработчик изменения звука.
 * @param onKeyboardHapticFeedbackChanged обработчик изменения вибрации.
 */
@Composable
internal fun SettingsScreen(
    usesPreRevolutionaryOrthography: Boolean,
    isKeyboardSoundFeedbackEnabled: Boolean,
    isKeyboardHapticFeedbackEnabled: Boolean,
    onUsesPreRevolutionaryOrthographyChanged: (Boolean) -> Unit,
    onKeyboardSoundFeedbackChanged: (Boolean) -> Unit,
    onKeyboardHapticFeedbackChanged: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .verticalScroll(rememberScrollState())
            .padding(ScreenPadding)
    ) {
        Text(
            text = if (usesPreRevolutionaryOrthography) {
                "Основныя"
            } else {
                "Основные"
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                start = ListTextPadding,
                bottom = 4.dp
            )
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(SettingsSectionCornerRadius),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            )
        ) {
            SettingsToggle(
                title = if (usesPreRevolutionaryOrthography) {
                    "Дореволюціонная орѳографія"
                } else {
                    "Дореволюционная орфография"
                },
                checked = usesPreRevolutionaryOrthography,
                onCheckedChange =
                    onUsesPreRevolutionaryOrthographyChanged
            )
        }

        Text(
            text = if (usesPreRevolutionaryOrthography) {
                "Отображеніе текста въ орѳографіи, употреблявшейся до реформы 1918 года."
            } else {
                "Отображение текста в орфографии, употреблявшейся до реформы 1918 года."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                horizontal = ListTextPadding,
                vertical = 10.dp
            )
        )

        Spacer(modifier = Modifier.height(SectionSpacing))

        Text(
            text = if (usesPreRevolutionaryOrthography) {
                "Клавіатура"
            } else {
                "Клавиатура"
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                start = ListTextPadding,
                bottom = 4.dp
            )
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(SettingsSectionCornerRadius),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            )
        ) {
            SettingsToggle(
                title = if (usesPreRevolutionaryOrthography) {
                    "Звуковой откликъ"
                } else {
                    "Звуковой отклик"
                },
                checked = isKeyboardSoundFeedbackEnabled,
                onCheckedChange = onKeyboardSoundFeedbackChanged
            )

            HorizontalDivider(
                modifier = Modifier.padding(start = ListTextPadding)
            )

            SettingsToggle(
                title = if (usesPreRevolutionaryOrthography) {
                    "Виброоткликъ"
                } else {
                    "Виброотклик"
                },
                checked = isKeyboardHapticFeedbackEnabled,
                onCheckedChange = onKeyboardHapticFeedbackChanged
            )
        }

        Text(
            text = if (usesPreRevolutionaryOrthography) {
                "Звукъ и вибрація при нажатіи клавишъ. Оба отклика по умолчанію выключены."
            } else {
                "Звук и вибрация при нажатии клавиш. Оба отклика по умолчанию выключены."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                horizontal = ListTextPadding,
                vertical = 10.dp
            )
        )
    }
}

/**
 * Строка настройки, одинаково переключаемая касанием текста и переключателя.
 *
 * @param title название настройки.
 * @param checked текущее состояние.
 * @param onCheckedChange обработчик нового состояния.
 */
@Composable
private fun SettingsToggle(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = {
            Text(title)
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier.clickable {
            onCheckedChange(!checked)
        }
    )
}
