package software.kanunnikoff.izhitsa.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Abc
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Основные разделы приложения и их значки навигации.
 *
 * @property icon значок раздела.
 */
private enum class AppSection(
    val icon: ImageVector
) {
    HOME(Icons.Rounded.Home),
    ALPHABET(Icons.Rounded.Abc),
    SETTINGS(Icons.Rounded.Settings),
    ABOUT(Icons.Rounded.Info)
}

/**
 * Адаптивная оболочка с нижней навигацией на телефоне и боковой на планшете.
 *
 * @param usesPreRevolutionaryOrthography используется ли дореформенное написание.
 * @param isKeyboardSoundFeedbackEnabled включён ли звук клавиш.
 * @param isKeyboardHapticFeedbackEnabled включена ли вибрация клавиш.
 * @param hasUsedKeyboard пользовался ли владелец клавиатурой.
 * @param versionName отображаемое имя версии.
 * @param versionCode внутренний номер сборки.
 * @param onUsesPreRevolutionaryOrthographyChanged обработчик изменения орфографии.
 * @param onKeyboardSoundFeedbackChanged обработчик изменения звука.
 * @param onKeyboardHapticFeedbackChanged обработчик изменения вибрации.
 * @param onSupportAuthor обработчик покупки чаевых.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppNavigation(
    usesPreRevolutionaryOrthography: Boolean,
    isKeyboardSoundFeedbackEnabled: Boolean,
    isKeyboardHapticFeedbackEnabled: Boolean,
    hasUsedKeyboard: Boolean,
    versionName: String,
    versionCode: Long,
    onUsesPreRevolutionaryOrthographyChanged: (Boolean) -> Unit,
    onKeyboardSoundFeedbackChanged: (Boolean) -> Unit,
    onKeyboardHapticFeedbackChanged: (Boolean) -> Unit,
    onSupportAuthor: () -> Unit
) {
    var selectedSection by remember { mutableStateOf(AppSection.HOME) }
    val title = selectedSection.title(
        usesPreRevolutionaryOrthography =
            usesPreRevolutionaryOrthography
    )
    val navigationItems = AppSection.entries

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val usesNavigationRail = maxWidth >= TabletBreakpoint

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing.exclude(WindowInsets.ime),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            if (selectedSection == AppSection.HOME) {
                                Text(
                                    text = "Спасибо за чаевые!",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        if (selectedSection == AppSection.HOME) {
                            IconButton(
                                onClick = onSupportAuthor,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceContainerLow
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocalCafe,
                                    contentDescription = "Поддержать автора"
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (!usesNavigationRail) {
                    NavigationBar {
                        navigationItems.forEach { item ->
                            NavigationBarItem(
                                selected = selectedSection == item,
                                onClick = { selectedSection = item },
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.shortTitle(
                                            usesPreRevolutionaryOrthography =
                                                usesPreRevolutionaryOrthography
                                        ),
                                        maxLines = 1
                                    )
                                }
                            )
                        }
                    }
                }
            }
        ) { contentPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .consumeWindowInsets(contentPadding)
            ) {
                if (usesNavigationRail) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))

                        navigationItems.forEach { item ->
                            NavigationRailItem(
                                selected = selectedSection == item,
                                onClick = { selectedSection = item },
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.shortTitle(
                                            usesPreRevolutionaryOrthography =
                                                usesPreRevolutionaryOrthography
                                        )
                                    )
                                }
                            )
                        }
                    }
                }

                when (selectedSection) {
                    AppSection.HOME -> HomeScreen(
                        usesPreRevolutionaryOrthography =
                            usesPreRevolutionaryOrthography,
                        hasUsedKeyboard = hasUsedKeyboard
                    )

                    AppSection.ALPHABET -> AlphabetScreen(
                        usesPreRevolutionaryOrthography =
                            usesPreRevolutionaryOrthography
                    )

                    AppSection.SETTINGS -> SettingsScreen(
                        usesPreRevolutionaryOrthography =
                            usesPreRevolutionaryOrthography,
                        isKeyboardSoundFeedbackEnabled =
                            isKeyboardSoundFeedbackEnabled,
                        isKeyboardHapticFeedbackEnabled =
                            isKeyboardHapticFeedbackEnabled,
                        onUsesPreRevolutionaryOrthographyChanged =
                            onUsesPreRevolutionaryOrthographyChanged,
                        onKeyboardSoundFeedbackChanged =
                            onKeyboardSoundFeedbackChanged,
                        onKeyboardHapticFeedbackChanged =
                            onKeyboardHapticFeedbackChanged
                    )

                    AppSection.ABOUT -> AboutScreen(
                        usesPreRevolutionaryOrthography =
                            usesPreRevolutionaryOrthography,
                        versionName = versionName,
                        versionCode = versionCode,
                        onSupportAuthor = onSupportAuthor
                    )
                }
            }
        }
    }
}



/**
 * Возвращает полный заголовок раздела с учётом выбранной орфографии.
 *
 * @receiver раздел приложения.
 * @param usesPreRevolutionaryOrthography используется ли дореформенное написание.
 */
private fun AppSection.title(
    usesPreRevolutionaryOrthography: Boolean
): String {
    return when (this) {
        AppSection.HOME -> "Ижица"
        AppSection.ALPHABET -> {
            if (usesPreRevolutionaryOrthography) "Алфавитъ" else "Алфавит"
        }
        AppSection.SETTINGS -> "Настройки"
        AppSection.ABOUT -> {
            if (usesPreRevolutionaryOrthography) "О программѣ" else "О программе"
        }
    }
}

/**
 * Возвращает компактную подпись элемента навигации.
 *
 * @receiver раздел приложения.
 * @param usesPreRevolutionaryOrthography используется ли дореформенное написание.
 */
private fun AppSection.shortTitle(
    usesPreRevolutionaryOrthography: Boolean
): String {
    return when (this) {
        AppSection.HOME -> "Главная"
        AppSection.ALPHABET -> {
            if (usesPreRevolutionaryOrthography) "Алфавитъ" else "Алфавит"
        }
        AppSection.SETTINGS -> "Настройки"
        AppSection.ABOUT -> {
            if (usesPreRevolutionaryOrthography) "О программѣ" else "О программе"
        }
    }
}

/** Ширина, с которой нижняя навигация заменяется боковой. */
private val TabletBreakpoint = 600.dp
