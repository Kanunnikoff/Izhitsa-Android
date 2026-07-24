package software.kanunnikoff.izhitsa.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import software.kanunnikoff.izhitsa.R

/**
 * Показывает версию, ссылки, обратную связь, политику и поддержку автора.
 *
 * @param usesPreRevolutionaryOrthography используется ли дореформенное написание.
 * @param versionName отображаемое имя версии.
 * @param versionCode внутренний номер сборки.
 * @param onSupportAuthor обработчик покупки чаевых.
 */
@Composable
internal fun AboutScreen(
    usesPreRevolutionaryOrthography: Boolean,
    versionName: String,
    versionCode: Long,
    onSupportAuthor: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer),
        contentPadding = PaddingValues(
            horizontal = ScreenPadding,
            vertical = 12.dp
        )
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(SettingsSectionCornerRadius),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AboutApplicationIcon()

                    Column(
                        modifier = Modifier.padding(start = 12.dp)
                    ) {
                        Text(
                            text = "Ижица",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${if (usesPreRevolutionaryOrthography) "Версія" else "Версия"} $versionName, сборка $versionCode",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "© 2026 Дмитрiй Канунниковъ",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        item {
            AboutSection(
                title = "Google Play",
                footer = if (usesPreRevolutionaryOrthography) {
                    "Ваше мнѣніе очень важно для меня. Пожалуйста, не полѣнитесь поставить оцѣнку и написать отзывъ."
                } else {
                    "Ваше мнение очень важно для меня. Пожалуйста, не поленитесь поставить оценку и написать отзыв."
                },
                actions = listOf(
                    AboutActionItem(
                        title = if (usesPreRevolutionaryOrthography) {
                            "Оцѣнить"
                        } else {
                            "Оценить"
                        },
                        onClick = {
                            context.openStorePage(
                                packageName = context.packageName
                            )
                        }
                    ),
                    AboutActionItem(
                        title = if (usesPreRevolutionaryOrthography) {
                            "Подѣлиться"
                        } else {
                            "Поделиться"
                        },
                        onClick = {
                            context.shareText(
                                text = "$GooglePlayWebUrl${context.packageName}"
                            )
                        }
                    ),
                    AboutActionItem(
                        title = if (usesPreRevolutionaryOrthography) {
                            "Другія приложенія"
                        } else {
                            "Другие приложения"
                        },
                        onClick = {
                            context.openUrl(GooglePlayDeveloperUrl)
                        }
                    ),
                    AboutActionItem(
                        title = if (usesPreRevolutionaryOrthography) {
                            "Переводчикъ «Ять»"
                        } else {
                            "Переводчик «Ять»"
                        },
                        onClick = {
                            context.openStorePage(
                                packageName = YatPackageName
                            )
                        }
                    ),
                    AboutActionItem(
                        title = if (usesPreRevolutionaryOrthography) {
                            "Приложеніе «Русскія мѣры»"
                        } else {
                            "Приложение «Русские меры»"
                        },
                        onClick = {
                            context.openStorePage(
                                packageName = RussianMeasuresPackageName
                            )
                        }
                    )
                )
            )
        }

        item {
            AboutSection(
                title = "Написать письмо",
                footer = if (usesPreRevolutionaryOrthography) {
                    "Въ случаѣ вопросовъ или предложеній, я къ Вашимъ услугамъ. Будемъ на связи!"
                } else {
                    "В случае вопросов или предложений, я к Вашим услугам. Будем на связи!"
                },
                actions = listOf(
                    AboutActionItem(
                        title = "Написать письмо",
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_SENDTO,
                                    Uri.parse(FeedbackEmailUri)
                                )
                            )
                        }
                    )
                ),
                sectionTitle = "Обратная связь"
            )
        }

        item {
            AboutSection(
                title = "Читать",
                footer = if (usesPreRevolutionaryOrthography) {
                    "Подробная информація о томъ, какъ приложеніе используетъ Ваши данные."
                } else {
                    "Подробная информация о том, как приложение использует Ваши данные."
                },
                actions = listOf(
                    AboutActionItem(
                        title = "Читать",
                        onClick = {
                            context.openUrl(PrivacyPolicyUrl)
                        }
                    )
                ),
                sectionTitle = if (usesPreRevolutionaryOrthography) {
                    "Политика конфиденціальности"
                } else {
                    "Политика конфиденциальности"
                }
            )
        }

        item {
            AboutSection(
                title = "Чаевые",
                footer = if (usesPreRevolutionaryOrthography) {
                    "Если Вамъ нравится результатъ моего труда, то Вы можете, при желаніи, поддержать меня чаевыми."
                } else {
                    "Если Вам нравится результат моего труда, то Вы можете, при желании, поддержать меня чаевыми."
                },
                actions = listOf(
                    AboutActionItem(
                        title = "Чаевые",
                        onClick = onSupportAuthor
                    )
                ),
                sectionTitle = "Поддержка"
            )
        }
    }
}

/** Отображает передний слой адаптивного значка внутри компактной карточки. */
@Composable
private fun AboutApplicationIcon() {
    /*
     * На Android 8 и новее ic_launcher разрешается в XML adaptive-icon,
     * который painterResource не поддерживает. Передний слой хранится как
     * обычный PNG и содержит безопасные поля адаптивного значка. Масштаб
     * убирает эти поля, чтобы внутри карточки осталось исходное изображение.
     */
    Box(
        modifier = Modifier
            .size(AboutApplicationIconSize)
            .clip(RoundedCornerShape(AboutApplicationIconCornerRadius))
    ) {
        Image(
            painter = painterResource(R.mipmap.ic_launcher_adaptive_fore),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .scale(AdaptiveIconArtworkScale)
        )
    }
}

/**
 * Название и обработчик одной строки на экране сведений о программе.
 *
 * @property title видимая подпись действия.
 * @property onClick обработчик выбора действия.
 */
private data class AboutActionItem(
    val title: String,
    val onClick: () -> Unit
)

/**
 * Группа действий с заголовком, карточкой и пояснением.
 *
 * @param title заголовок по умолчанию и подпись единственного действия.
 * @param footer поясняющий текст под карточкой.
 * @param actions строки действий в порядке отображения.
 * @param sectionTitle отдельный заголовок группы.
 */
@Composable
private fun AboutSection(
    title: String,
    footer: String,
    actions: List<AboutActionItem>,
    sectionTitle: String = title
) {
    Text(
        text = sectionTitle,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(
            start = ListTextPadding,
            top = 16.dp,
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
        actions.forEachIndexed { index, action ->
            AboutAction(
                title = action.title,
                onClick = action.onClick
            )

            if (index < actions.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = ListTextPadding)
                )
            }
        }
    }

    Text(
        text = footer,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(
            horizontal = ListTextPadding,
            vertical = 10.dp
        )
    )
}

/**
 * Одна нажимаемая строка действия на экране сведений о программе.
 *
 * @param title видимая подпись.
 * @param onClick обработчик нажатия.
 */
@Composable
private fun AboutAction(
    title: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.primary
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier.clickable(onClick = onClick)
    )
}

/** Сторона значка приложения на экране сведений. */
private val AboutApplicationIconSize = 65.dp

/** Радиус углов карточки значка приложения. */
private val AboutApplicationIconCornerRadius = 8.dp

/** Масштаб переднего слоя, убирающий безопасные поля адаптивного значка. */
private const val AdaptiveIconArtworkScale = 1.5f
