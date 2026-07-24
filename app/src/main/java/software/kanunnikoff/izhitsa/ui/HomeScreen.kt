package software.kanunnikoff.izhitsa.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

/**
 * Главный экран с инструкцией по включению, описанием и полем проверки клавиатуры.
 *
 * @param usesPreRevolutionaryOrthography используется ли дореформенное написание.
 * @param hasUsedKeyboard пользовался ли владелец клавиатурой.
 */
@Composable
internal fun HomeScreen(
    usesPreRevolutionaryOrthography: Boolean,
    hasUsedKeyboard: Boolean
) {
    var testText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenPadding)
            .padding(bottom = ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(SectionSpacing)
    ) {
        ActivationGuide(
            usesPreRevolutionaryOrthography =
                usesPreRevolutionaryOrthography,
            hasUsedKeyboard = hasUsedKeyboard
        )

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = DividerPadding)
        )

        KeyboardDescription(
            usesPreRevolutionaryOrthography =
                usesPreRevolutionaryOrthography
        )

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = DividerPadding)
        )

        OutlinedTextField(
            value = testText,
            onValueChange = { value ->
                testText = value.take(MaximumTestTextLength)
            },
            modifier = Modifier.fillMaxWidth(),
            minLines = 1,
            maxLines = 5,
            placeholder = {
                Text(
                    if (usesPreRevolutionaryOrthography) {
                        "Провѣрьте вводъ"
                    } else {
                        "Проверьте ввод"
                    }
                )
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Default
            )
        )
    }
}

/**
 * Пошагово объясняет включение и выбор метода ввода.
 *
 * @param usesPreRevolutionaryOrthography используется ли дореформенное написание.
 * @param hasUsedKeyboard завершён ли шаг пробного ввода.
 */
@Composable
private fun ActivationGuide(
    usesPreRevolutionaryOrthography: Boolean,
    hasUsedKeyboard: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(CardPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Keyboard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = if (usesPreRevolutionaryOrthography) {
                        "Какъ включить клавіатуру"
                    } else {
                        "Как включить клавиатуру"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            ActivationStep(
                number = 1,
                text = if (usesPreRevolutionaryOrthography) {
                    "Откройте «Настройки» → «Система»."
                } else {
                    "Откройте «Настройки» → «Система»."
                }
            )
            ActivationStep(
                number = 2,
                text = if (usesPreRevolutionaryOrthography) {
                    "Клавіатура → Экранная клавіатура → Управленіе клавіатурами"
                } else {
                    "Клавиатура → Экранная клавиатура → Управление клавиатурами"
                }
            )
            ActivationStep(
                number = 3,
                text = if (usesPreRevolutionaryOrthography) {
                    "Включите клавіатуру «Ижица»."
                } else {
                    "Включите клавиатуру «Ижица»."
                },
                isCompleted = hasUsedKeyboard
            )

            HorizontalDivider()

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = if (hasUsedKeyboard) {
                        Icons.Rounded.CheckCircle
                    } else {
                        Icons.Rounded.Language
                    },
                    contentDescription = null,
                    tint = if (hasUsedKeyboard) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                Text(
                    text = if (hasUsedKeyboard) {
                        if (usesPreRevolutionaryOrthography) {
                            "Клавіатура уже была успѣшно открыта на этомъ устройствѣ."
                        } else {
                            "Клавиатура уже была успешно открыта на этом устройстве."
                        }
                    } else {
                        if (usesPreRevolutionaryOrthography) {
                            "Послѣ включенія выберите «Ижицу» кнопкой переключенія способовъ ввода."
                        } else {
                            "После включения выберите «Ижицу» кнопкой переключения способов ввода."
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (hasUsedKeyboard) {
                        SuccessGreen
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Text(
                text = if (usesPreRevolutionaryOrthography) {
                    "Въ поляхъ пароля и номера телефона Android можетъ показывать системную клавіатуру. Нѣкоторыя приложенія также могутъ запрещать стороннія клавіатуры."
                } else {
                    "В полях пароля и номера телефона Android может показывать системную клавиатуру. Некоторые приложения также могут запрещать сторонние клавиатуры."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Одна строка инструкции с номером либо отметкой выполнения.
 *
 * @param number порядковый номер шага.
 * @param text пояснение шага.
 * @param isCompleted заменять ли номер отметкой выполнения.
 */
@Composable
private fun ActivationStep(
    number: Int,
    text: String,
    isCompleted: Boolean = false
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isCompleted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isCompleted) "✓" else number.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = if (isCompleted) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                },
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Кратко объясняет расположение дореформенных букв на клавиатуре.
 *
 * @param usesPreRevolutionaryOrthography используется ли дореформенное написание.
 */
@Composable
private fun KeyboardDescription(
    usesPreRevolutionaryOrthography: Boolean
) {
    Card(
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(CardPadding),
            verticalArrangement = Arrangement.spacedBy(SectionSpacing)
        ) {
            Text(
                text = if (usesPreRevolutionaryOrthography) {
                    "Клавіатура содержитъ четыре дополнительныя буквы дореформенной орѳографіи: ять (ѣ), фиту (ѳ), и десятеричное (і) и ижицу (ѵ)."
                } else {
                    "Клавиатура содержит четыре дополнительные буквы дореформенной орфографии: ять (ѣ), фиту (ѳ), и десятеричное (і) и ижицу (ѵ)."
                },
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = if (usesPreRevolutionaryOrthography) {
                    "Дореформенныя буквы доступны по долгому удержанію соотвѣтствующихъ буквъ е, ф и и. Твёрдый знакъ доступенъ по долгому удержанію мягкаго знака."
                } else {
                    "Дореформенные буквы доступны по долгому удержанию соответствующих букв е, ф и и. Твёрдый знак доступен по долгому удержанию мягкого знака."
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/** Предельное число знаков в поле проверки клавиатуры. */
private const val MaximumTestTextLength = 2_000

/** Внутренний отступ информационных карточек. */
private val CardPadding = 16.dp

/** Боковой отступ коротких разделителей между блоками. */
private val DividerPadding = 80.dp

/** Цвет сообщения об успешном использовании клавиатуры. */
private val SuccessGreen = Color(0xFF34C759)
