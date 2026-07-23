package software.kanunnikoff.izhitsa.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Abc
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import software.kanunnikoff.izhitsa.R

@Composable
fun IzhitsaApplication(
    initialUsesSystemFont: Boolean,
    initialUsesPreRevolutionaryOrthography: Boolean,
    hasUsedKeyboard: Boolean,
    versionName: String,
    versionCode: Long,
    onUsesSystemFontChanged: (Boolean) -> Unit,
    onUsesPreRevolutionaryOrthographyChanged: (Boolean) -> Unit,
    onSupportAuthor: () -> Unit
) {
    var usesSystemFont by remember { mutableStateOf(initialUsesSystemFont) }
    var usesPreRevolutionaryOrthography by remember {
        mutableStateOf(initialUsesPreRevolutionaryOrthography)
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
    val appFontFamily = if (usesSystemFont) {
        FontFamily.Default
    } else {
        FontFamily(Font(R.font.monomakh_unicode))
    }
    val typography = MaterialTheme.typography.run {
        copy(
            displayLarge = displayLarge.copy(fontFamily = appFontFamily),
            displayMedium = displayMedium.copy(fontFamily = appFontFamily),
            displaySmall = displaySmall.copy(fontFamily = appFontFamily),
            headlineLarge = headlineLarge.copy(fontFamily = appFontFamily),
            headlineMedium = headlineMedium.copy(fontFamily = appFontFamily),
            headlineSmall = headlineSmall.copy(fontFamily = appFontFamily),
            titleLarge = titleLarge.copy(fontFamily = appFontFamily),
            titleMedium = titleMedium.copy(fontFamily = appFontFamily),
            titleSmall = titleSmall.copy(fontFamily = appFontFamily),
            bodyLarge = bodyLarge.copy(fontFamily = appFontFamily),
            bodyMedium = bodyMedium.copy(fontFamily = appFontFamily),
            bodySmall = bodySmall.copy(fontFamily = appFontFamily),
            labelLarge = labelLarge.copy(fontFamily = appFontFamily),
            labelMedium = labelMedium.copy(fontFamily = appFontFamily),
            labelSmall = labelSmall.copy(fontFamily = appFontFamily)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography
    ) {
        AppNavigation(
            usesSystemFont = usesSystemFont,
            usesPreRevolutionaryOrthography =
                usesPreRevolutionaryOrthography,
            hasUsedKeyboard = hasUsedKeyboard,
            versionName = versionName,
            versionCode = versionCode,
            onUsesSystemFontChanged = { value ->
                usesSystemFont = value
                onUsesSystemFontChanged(value)
            },
            onUsesPreRevolutionaryOrthographyChanged = { value ->
                usesPreRevolutionaryOrthography = value
                onUsesPreRevolutionaryOrthographyChanged(value)
            },
            onSupportAuthor = onSupportAuthor
        )
    }
}

private enum class AppSection(
    val icon: ImageVector
) {
    HOME(Icons.Rounded.Home),
    ALPHABET(Icons.Rounded.Abc),
    SETTINGS(Icons.Rounded.Settings),
    ABOUT(Icons.Rounded.Info)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppNavigation(
    usesSystemFont: Boolean,
    usesPreRevolutionaryOrthography: Boolean,
    hasUsedKeyboard: Boolean,
    versionName: String,
    versionCode: Long,
    onUsesSystemFontChanged: (Boolean) -> Unit,
    onUsesPreRevolutionaryOrthographyChanged: (Boolean) -> Unit,
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
            contentWindowInsets = WindowInsets.safeDrawing,
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
                        usesSystemFont = usesSystemFont,
                        usesPreRevolutionaryOrthography =
                            usesPreRevolutionaryOrthography,
                        onUsesSystemFontChanged =
                            onUsesSystemFontChanged,
                        onUsesPreRevolutionaryOrthographyChanged =
                            onUsesPreRevolutionaryOrthographyChanged
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

@Composable
private fun HomeScreen(
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

@Composable
private fun AlphabetScreen(
    usesPreRevolutionaryOrthography: Boolean
) {
    val context = LocalContext.current

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 72.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(GridSpacing),
        verticalArrangement = Arrangement.spacedBy(GridSpacing)
    ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (usesPreRevolutionaryOrthography) {
                        "Дореформенная русская азбука"
                    } else {
                        "Дореформенный русский алфавит"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Прописные и строчные формы 35 букв, входивших в русскую азбуку до реформы 1918 года.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(HistoricalAlphabet) { letter ->
            Card(
                modifier = Modifier.height(76.dp),
                colors = CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = letter.first,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = letter.second,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Text(
                text = "Четыре упразднённые буквы — І, Ѣ, Ѳ и Ѵ. Буквы Ё и Й формально не входили в азбуку, но употреблялись в письме.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable {
                        context.openUrl(AlphabetSourceUrl)
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Источник: «Русская дореформенная орфография»",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    usesSystemFont: Boolean,
    usesPreRevolutionaryOrthography: Boolean,
    onUsesSystemFontChanged: (Boolean) -> Unit,
    onUsesPreRevolutionaryOrthographyChanged: (Boolean) -> Unit
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
                    "Системный шрифтъ и размѣръ"
                } else {
                    "Системный шрифт и размер"
                },
                checked = usesSystemFont,
                onCheckedChange = onUsesSystemFontChanged
            )

            HorizontalDivider(
                modifier = Modifier.padding(start = ListTextPadding)
            )

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
                "Возможность отображенія текста въ старинномъ начертаніи и орѳографіи до реформы 1918-го года."
            } else {
                "Возможность отображения текста в старинном начертании и орфографии до реформы 1918-го года."
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

@Composable
private fun AboutScreen(
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
                    Image(
                        painter = painterResource(R.mipmap.ic_launcher),
                        contentDescription = null,
                        modifier = Modifier
                            .size(65.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )

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

private data class AboutActionItem(
    val title: String,
    val onClick: () -> Unit
)

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

private fun AppSection.title(
    usesPreRevolutionaryOrthography: Boolean
): String {
    return when (this) {
        AppSection.HOME -> "Ижица"
        AppSection.ALPHABET -> {
            if (usesPreRevolutionaryOrthography) "Азбука" else "Алфавит"
        }
        AppSection.SETTINGS -> "Настройки"
        AppSection.ABOUT -> {
            if (usesPreRevolutionaryOrthography) "О программѣ" else "О программе"
        }
    }
}

private fun AppSection.shortTitle(
    usesPreRevolutionaryOrthography: Boolean
): String {
    return when (this) {
        AppSection.HOME -> "Главная"
        AppSection.ALPHABET -> {
            if (usesPreRevolutionaryOrthography) "Азбука" else "Алфавит"
        }
        AppSection.SETTINGS -> "Настройки"
        AppSection.ABOUT -> {
            if (usesPreRevolutionaryOrthography) "О программѣ" else "О программе"
        }
    }
}

private fun android.content.Context.openStorePage(packageName: String) {
    val storeIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("$GooglePlayMarketUrl$packageName")
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    runCatching {
        startActivity(storeIntent)
    }.getOrElse {
        openUrl("$GooglePlayWebUrl$packageName")
    }
}

private fun android.content.Context.openUrl(url: String) {
    startActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

private fun android.content.Context.shareText(text: String) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = PlainTextMimeType
        putExtra(Intent.EXTRA_TEXT, text)
    }

    startActivity(
        Intent.createChooser(shareIntent, ShareAppTitle)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

private val HistoricalAlphabet = listOf(
    "А" to "а", "Б" to "б", "В" to "в", "Г" to "г", "Д" to "д",
    "Е" to "е", "Ж" to "ж", "З" to "з", "И" to "и", "І" to "і",
    "К" to "к", "Л" to "л", "М" to "м", "Н" to "н", "О" to "о",
    "П" to "п", "Р" to "р", "С" to "с", "Т" to "т", "У" to "у",
    "Ф" to "ф", "Х" to "х", "Ц" to "ц", "Ч" to "ч", "Ш" to "ш",
    "Щ" to "щ", "Ъ" to "ъ", "Ы" to "ы", "Ь" to "ь", "Ѣ" to "ѣ",
    "Э" to "э", "Ю" to "ю", "Я" to "я", "Ѳ" to "ѳ", "Ѵ" to "ѵ"
)

private const val MaximumTestTextLength = 2_000
private const val YatPackageName = "software.kanunnikoff.yat"
private const val RussianMeasuresPackageName =
    "software.kanunnikoff.russianmeasures"
private const val GooglePlayMarketUrl = "market://details?id="
private const val GooglePlayWebUrl =
    "https://play.google.com/store/apps/details?id="
private const val GooglePlayDeveloperUrl =
    "https://play.google.com/store/apps/dev?id=9118553902079488918"
private const val FeedbackEmailUri =
    "mailto:dmitry.kanunnikoff@gmail.com?subject=%D0%98%D0%B6%D0%B8%D1%86%D0%B0%20%28Android%29"
private const val PrivacyPolicyUrl =
    "https://docs.google.com/document/d/189iftSQQuRh8VGhFnCUDY5ujwgU5gsnnPIUjOGL5ypE/edit?usp=sharing"
private const val AlphabetSourceUrl =
    "https://ru.wikipedia.org/wiki/%D0%A0%D1%83%D1%81%D1%81%D0%BA%D0%B0%D1%8F_%D0%B4%D0%BE%D1%80%D0%B5%D1%84%D0%BE%D1%80%D0%BC%D0%B5%D0%BD%D0%BD%D0%B0%D1%8F_%D0%BE%D1%80%D1%84%D0%BE%D0%B3%D1%80%D0%B0%D1%84%D0%B8%D1%8F"
private const val PlainTextMimeType = "text/plain"
private const val ShareAppTitle = "Поделиться приложением"

private val TabletBreakpoint = 600.dp
private val ScreenPadding = 16.dp
private val CardPadding = 16.dp
private val SectionSpacing = 20.dp
private val GridSpacing = 12.dp
private val DividerPadding = 80.dp
private val ListTextPadding = 16.dp
private val SettingsSectionCornerRadius = 24.dp
private val SuccessGreen = Color(0xFF34C759)
