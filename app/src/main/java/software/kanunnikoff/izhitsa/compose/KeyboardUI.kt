package software.kanunnikoff.izhitsa.compose

import android.view.ContextThemeWrapper
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.KeyboardReturn
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.KeyboardCapslock
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.emoji2.emojipicker.EmojiPickerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import software.kanunnikoff.izhitsa.R
import software.kanunnikoff.izhitsa.stickers.Sticker
import software.kanunnikoff.izhitsa.stickers.StickerCatalog
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt
import androidx.emoji2.emojipicker.R as EmojiPickerResources

data class KeyInfo(
    val code: Int,
    val label: String? = null,
    val icon: KeyIcon? = null,
    val hint: String? = null,
    val hintIcon: KeyIcon? = null,
    val alternatives: List<String> = emptyList(),
    val tapAlternatives: List<String> = alternatives,
    val alternativeRowLengths: List<Int> = emptyList(),
    val preferredAlternativeIndex: Int? = null,
    val longPressAction: KeyLongPressAction? = null,
    val weight: Float = 1f,
    val fontSize: TextUnit? = null,
    val isModifier: Boolean = false,
    val isActive: Boolean = false,
    val repeatOnLongPress: Boolean = false
)

enum class KeyLongPressAction {
    SHOW_EMOJI
}

enum class KeyboardPanel {
    KEYS,
    ACTIONS,
    EMOJI,
    STICKERS,
    CLIPBOARD
}

enum class KeyboardToolbarAction {
    OPEN_ACTIONS,
    SHARE_APP,
    NEXT_LANGUAGE,
    EMOJI,
    STICKERS,
    CLIPBOARD,
    SETTINGS
}

@Composable
fun KeyboardScreen(
    rows: List<List<KeyInfo>>,
    isNumberLayout: Boolean,
    panel: KeyboardPanel,
    suggestions: List<String>,
    clipboardText: String?,
    supportsStickerContent: Boolean,
    isHapticFeedbackEnabled: Boolean,
    onKeyClick: (KeyInfo) -> Boolean,
    onKeyLongPressAction: (KeyLongPressAction) -> Unit,
    onAlternativeSelected: (KeyInfo, String) -> Unit,
    onSuggestionClick: (String) -> Unit,
    onToolbarAction: (KeyboardToolbarAction) -> Unit,
    onEmojiPicked: (String) -> Unit,
    onReactionPicked: (String) -> Unit,
    onStickerPicked: (Sticker) -> Unit,
    onClosePanel: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val palette = keyboardPalette(isDark = isDark)
    val bottomInset = keyboardBottomInset()
    var alternativeMenuVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = palette.background)
            .padding(bottom = bottomInset)
    ) {
        Column(
            modifier = Modifier.graphicsLayer(
                alpha = if (alternativeMenuVisible) {
                    AlternativeMenuBackgroundAlpha
                } else {
                    1f
                }
            )
        ) {
            when (panel) {
                KeyboardPanel.KEYS -> {
                    KeyboardToolbar(
                        suggestions = suggestions,
                        palette = palette,
                        onSuggestionClick = onSuggestionClick,
                        onToolbarAction = onToolbarAction
                    )

                    if (isNumberLayout) {
                        NumberKeyboardRows(
                            rows = rows,
                            palette = palette,
                            isHapticFeedbackEnabled =
                                isHapticFeedbackEnabled,
                            onKeyClick = onKeyClick,
                            onKeyLongPressAction = onKeyLongPressAction,
                            onAlternativeSelected = onAlternativeSelected,
                            onAlternativeMenuVisibilityChanged = { isVisible ->
                                alternativeMenuVisible = isVisible
                            }
                        )
                    } else {
                        KeyboardRows(
                            rows = rows,
                            palette = palette,
                            isHapticFeedbackEnabled =
                                isHapticFeedbackEnabled,
                            onKeyClick = onKeyClick,
                            onKeyLongPressAction = onKeyLongPressAction,
                            onAlternativeSelected = onAlternativeSelected,
                            onAlternativeMenuVisibilityChanged = { isVisible ->
                                alternativeMenuVisible = isVisible
                            }
                        )
                    }
                }

                KeyboardPanel.ACTIONS -> {
                    ActionsPanelToolbar(
                        palette = palette,
                        onBack = onClosePanel,
                        onToolbarAction = onToolbarAction
                    )

                    ActionsPanel(
                        palette = palette,
                        onToolbarAction = onToolbarAction
                    )
                }

                KeyboardPanel.EMOJI -> {
                    EmojiPanel(
                        isDark = isDark,
                        palette = palette,
                        onEmojiPicked = onEmojiPicked
                    )

                    PanelNavigation(
                        palette = palette,
                        onBack = onClosePanel,
                        onBackspace = {
                            onKeyClick(
                                KeyInfo(
                                    code = KeyboardKeyCodes.DELETE,
                                    icon = KeyIcon.BACKSPACE,
                                    isModifier = true
                                )
                            )
                        }
                    )
                }

                KeyboardPanel.STICKERS -> {
                    PanelToolbar(
                        title = "Стикеры",
                        palette = palette,
                        onBack = onClosePanel
                    )

                    StickersPanel(
                        palette = palette,
                        supportsStickerContent = supportsStickerContent,
                        onStickerPicked = onStickerPicked
                    )

                    PanelNavigation(
                        palette = palette,
                        onBack = onClosePanel,
                        onBackspace = {
                            onKeyClick(
                                KeyInfo(
                                    code = KeyboardKeyCodes.DELETE,
                                    icon = KeyIcon.BACKSPACE,
                                    isModifier = true
                                )
                            )
                        }
                    )
                }

                KeyboardPanel.CLIPBOARD -> {
                    PanelToolbar(
                        title = "Буфер обмена",
                        palette = palette,
                        onBack = onClosePanel
                    )

                    ClipboardPanel(
                        clipboardText = clipboardText,
                        palette = palette,
                        onPaste = onReactionPicked
                    )

                    PanelNavigation(
                        palette = palette,
                        onBack = onClosePanel,
                        onBackspace = {
                            onKeyClick(
                                KeyInfo(
                                    code = KeyboardKeyCodes.DELETE,
                                    icon = KeyIcon.BACKSPACE,
                                    isModifier = true
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

@Suppress("DiscouragedApi")
@Composable
private fun keyboardBottomInset(): Dp {
    val navigationBarInset = WindowInsets.navigationBars
        .only(WindowInsetsSides.Bottom)
        .asPaddingValues()
        .calculateBottomPadding()
    val resources = LocalContext.current.resources
    val density = LocalDensity.current
    val navigationAreaHeightResourceId = remember(resources) {
        resources.getIdentifier(
            InputMethodNavigationBarHeightResourceName,
            DimensionResourceType,
            AndroidResourcePackage
        ).takeIf { resourceId -> resourceId != 0 }
            ?: resources.getIdentifier(
                NavigationBarFrameHeightResourceName,
                DimensionResourceType,
                AndroidResourcePackage
            )
    }
    val navigationAreaHeight = if (navigationAreaHeightResourceId == 0) {
        0.dp
    } else {
        with(density) {
            resources.getDimensionPixelSize(navigationAreaHeightResourceId).toDp()
        }
    }

    // При жестовом управлении navigationBars сообщает только высоту области распознавания
    // жеста, тогда как системная клавиатура оставляет всю область навигационной панели.
    // Специальный системный размер даёт ту же высоту, а максимум с фактическим отступом
    // не удваивает пространство при кнопочном управлении. На старых версиях Android,
    // где специального размера ещё нет, используется совместимый размер рамки панели.
    return maxOf(navigationBarInset, navigationAreaHeight) + KeyboardNavigationContentGap
}

@Composable
private fun KeyboardToolbar(
    suggestions: List<String>,
    palette: KeyboardPalette,
    onSuggestionClick: (String) -> Unit,
    onToolbarAction: (KeyboardToolbarAction) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ToolbarHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (suggestions.isNotEmpty()) {
            suggestions.take(MaxSuggestionCount).forEachIndexed { index, suggestion ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(shape = RoundedCornerShape(size = 12.dp))
                        .combinedClickable(
                            onClick = { onSuggestionClick(suggestion) },
                            onLongClick = {}
                        )
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = suggestion,
                        color = palette.text,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }

                if (index != minOf(suggestions.size, MaxSuggestionCount) - 1) {
                    Box(
                        modifier = Modifier
                            .width(SuggestionDividerWidth)
                            .height(SuggestionDividerHeight)
                            .background(color = palette.divider)
                    )
                }
            }
        } else {
            ToolbarIcon(
                imageVector = Icons.Rounded.GridView,
                contentDescription = "Открыть действия",
                palette = palette,
                onClick = { onToolbarAction(KeyboardToolbarAction.OPEN_ACTIONS) }
            )

            ToolbarIcon(
                imageVector = Icons.Rounded.Collections,
                contentDescription = "Стикеры",
                palette = palette,
                onClick = { onToolbarAction(KeyboardToolbarAction.STICKERS) }
            )

            ToolbarIcon(
                imageVector = Icons.Rounded.ContentPaste,
                contentDescription = "Буфер обмена",
                palette = palette,
                onClick = { onToolbarAction(KeyboardToolbarAction.CLIPBOARD) }
            )

            ToolbarIcon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = "Настройки клавиатур",
                palette = palette,
                onClick = { onToolbarAction(KeyboardToolbarAction.SETTINGS) }
            )
        }
    }
}

@Composable
private fun ActionsPanelToolbar(
    palette: KeyboardPalette,
    onBack: () -> Unit,
    onToolbarAction: (KeyboardToolbarAction) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ToolbarHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        ToolbarIcon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "Вернуться к клавишам",
            palette = palette,
            onClick = onBack
        )

        ToolbarIcon(
            imageVector = Icons.Rounded.Collections,
            contentDescription = "Стикеры",
            palette = palette,
            onClick = { onToolbarAction(KeyboardToolbarAction.STICKERS) }
        )

        ToolbarIcon(
            imageVector = Icons.Rounded.ContentPaste,
            contentDescription = "Буфер обмена",
            palette = palette,
            onClick = { onToolbarAction(KeyboardToolbarAction.CLIPBOARD) }
        )

        ToolbarIcon(
            imageVector = Icons.Rounded.Settings,
            contentDescription = "Настройки клавиатур",
            palette = palette,
            onClick = { onToolbarAction(KeyboardToolbarAction.SETTINGS) }
        )
    }
}

@Composable
private fun ActionsPanel(
    palette: KeyboardPalette,
    onToolbarAction: (KeyboardToolbarAction) -> Unit
) {
    val actions = listOf(
        KeyboardActionPanelItem(
            action = KeyboardToolbarAction.SHARE_APP,
            title = "Рассказать об Ижице",
            icon = Icons.Rounded.Share
        ),
        KeyboardActionPanelItem(
            action = KeyboardToolbarAction.NEXT_LANGUAGE,
            title = "Следующий язык",
            icon = Icons.Rounded.Language
        ),
        KeyboardActionPanelItem(
            action = KeyboardToolbarAction.EMOJI,
            title = "Эмодзи",
            icon = Icons.Outlined.EmojiEmotions
        )
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ActionsPanelBodyHeight)
            .padding(
                horizontal = ActionsPanelHorizontalPadding,
                vertical = ActionsPanelVerticalPadding
            ),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Top
    ) {
        actions.forEach { item ->
            Column(
                modifier = Modifier.width(ActionPanelItemWidth),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    onClick = { onToolbarAction(item.action) },
                    modifier = Modifier
                        .width(ActionPanelCardWidth)
                        .height(ActionPanelCardHeight),
                    shape = RoundedCornerShape(size = ActionPanelCardCornerRadius),
                    color = palette.key
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = palette.text,
                            modifier = Modifier.size(ActionPanelIconSize)
                        )
                    }
                }

                Text(
                    text = item.title,
                    modifier = Modifier.padding(top = ActionPanelLabelTopPadding),
                    color = palette.text,
                    fontSize = ActionPanelLabelFontSize,
                    lineHeight = ActionPanelLabelLineHeight,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ToolbarIcon(
    imageVector: ImageVector,
    contentDescription: String,
    palette: KeyboardPalette,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(ToolbarButtonSize)
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = palette.textSecondary,
            modifier = Modifier.size(ToolbarIconSize)
        )
    }
}

private data class KeyboardActionPanelItem(
    val action: KeyboardToolbarAction,
    val title: String,
    val icon: ImageVector
)

@Composable
private fun PanelToolbar(
    title: String,
    palette: KeyboardPalette,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ToolbarHeight)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Вернуться к буквам",
                tint = palette.text
            )
        }

        Text(
            text = title,
            color = palette.text,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun KeyboardRows(
    rows: List<List<KeyInfo>>,
    palette: KeyboardPalette,
    isHapticFeedbackEnabled: Boolean,
    onKeyClick: (KeyInfo) -> Boolean,
    onKeyLongPressAction: (KeyLongPressAction) -> Unit,
    onAlternativeSelected: (KeyInfo, String) -> Unit,
    onAlternativeMenuVisibilityChanged: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KeyboardHorizontalPadding)
            .padding(bottom = KeyboardVerticalPadding)
    ) {
        rows.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(KeyHeight),
                horizontalArrangement = Arrangement.spacedBy(KeyHorizontalGap)
            ) {
                row.forEach { key ->
                    KeyButton(
                        key = key,
                        modifier = Modifier.weight(key.weight),
                        palette = palette,
                        isBottomRow = rowIndex == rows.lastIndex,
                        isHapticFeedbackEnabled =
                            isHapticFeedbackEnabled,
                        onClick = { onKeyClick(key) },
                        onLongPressAction = onKeyLongPressAction,
                        onAlternativeMenuVisibilityChanged =
                            onAlternativeMenuVisibilityChanged,
                        onAlternativeSelected = { alternative ->
                            onAlternativeSelected(key, alternative)
                        }
                    )
                }
            }

            if (rowIndex != rows.lastIndex) {
                Spacer(modifier = Modifier.height(KeyVerticalGap))
            }
        }
    }
}

@Composable
private fun NumberKeyboardRows(
    rows: List<List<KeyInfo>>,
    palette: KeyboardPalette,
    isHapticFeedbackEnabled: Boolean,
    onKeyClick: (KeyInfo) -> Boolean,
    onKeyLongPressAction: (KeyLongPressAction) -> Unit,
    onAlternativeSelected: (KeyInfo, String) -> Unit,
    onAlternativeMenuVisibilityChanged: (Boolean) -> Unit
) {
    /*
     * Цифровая раскладка повторяет геометрию системной клавиатуры: отдельная
     * вертикальная колонка арифметических знаков, сетка 3 × 3 и служебная
     * колонка справа. Исходные клавиши остаются в общей модели, поэтому обработка
     * ввода и адаптивная клавиша действия не дублируются в интерфейсном слое.
     */
    val operatorKeys = rows.map { row -> row.first() }
    val digitRows = rows.take(3).map { row -> row.slice(indices = 1..3) }
    val sideKeys = rows.take(3).map { row -> row.last() }
    val bottomKeys = rows.last().drop(n = 1)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KeyboardHorizontalPadding)
            .padding(bottom = KeyboardVerticalPadding)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(NumberGridHeight),
            horizontalArrangement = Arrangement.spacedBy(KeyHorizontalGap)
        ) {
            NumberOperatorColumn(
                keys = operatorKeys,
                modifier = Modifier.weight(NumberSideColumnWeight),
                palette = palette,
                onKeyClick = onKeyClick
            )

            Column(
                modifier = Modifier.weight(NumberDigitGridWeight),
                verticalArrangement = Arrangement.spacedBy(KeyVerticalGap)
            ) {
                digitRows.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(KeyHorizontalGap)
                    ) {
                        row.forEach { key ->
                            KeyButton(
                                key = key,
                                modifier = Modifier.weight(1f),
                                palette = palette,
                                isBottomRow = false,
                                isHapticFeedbackEnabled =
                                    isHapticFeedbackEnabled,
                                onClick = { onKeyClick(key) },
                                onLongPressAction = onKeyLongPressAction,
                                onAlternativeMenuVisibilityChanged =
                                    onAlternativeMenuVisibilityChanged,
                                onAlternativeSelected = { alternative ->
                                    onAlternativeSelected(key, alternative)
                                }
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.weight(NumberSideColumnWeight),
                verticalArrangement = Arrangement.spacedBy(KeyVerticalGap)
            ) {
                sideKeys.forEach { key ->
                    KeyButton(
                        key = key,
                        modifier = Modifier.weight(1f),
                        palette = palette,
                        isBottomRow = false,
                        isHapticFeedbackEnabled =
                            isHapticFeedbackEnabled,
                        onClick = { onKeyClick(key) },
                        onLongPressAction = onKeyLongPressAction,
                        onAlternativeMenuVisibilityChanged =
                            onAlternativeMenuVisibilityChanged,
                        onAlternativeSelected = { alternative ->
                            onAlternativeSelected(key, alternative)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(KeyVerticalGap))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(KeyHeight),
            horizontalArrangement = Arrangement.spacedBy(KeyHorizontalGap)
        ) {
            bottomKeys.forEach { key ->
                KeyButton(
                    key = key,
                    modifier = Modifier.weight(key.weight),
                    palette = palette,
                    isBottomRow = true,
                    isHapticFeedbackEnabled =
                        isHapticFeedbackEnabled,
                    onClick = { onKeyClick(key) },
                    onLongPressAction = onKeyLongPressAction,
                    onAlternativeMenuVisibilityChanged =
                        onAlternativeMenuVisibilityChanged,
                    onAlternativeSelected = { alternative ->
                        onAlternativeSelected(key, alternative)
                    }
                )
            }
        }
    }
}

@Composable
private fun NumberOperatorColumn(
    keys: List<KeyInfo>,
    modifier: Modifier,
    palette: KeyboardPalette,
    onKeyClick: (KeyInfo) -> Boolean
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(shape = RoundedCornerShape(size = KeyCornerRadius))
            .background(color = palette.keySecondary)
    ) {
        keys.forEach { key ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .combinedClickable(
                        onClick = { onKeyClick(key) },
                        onLongClick = {}
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = key.label.orEmpty(),
                    color = palette.text,
                    fontSize = key.fontSize ?: KeyLabelFontSize,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeyButton(
    key: KeyInfo,
    modifier: Modifier = Modifier,
    palette: KeyboardPalette,
    isBottomRow: Boolean,
    isHapticFeedbackEnabled: Boolean,
    onClick: () -> Boolean,
    onLongPressAction: (KeyLongPressAction) -> Unit,
    onAlternativeMenuVisibilityChanged: (Boolean) -> Unit,
    onAlternativeSelected: (String) -> Unit
) {
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val longPressTimeoutMillis = LocalViewConfiguration.current.longPressTimeoutMillis
    val screenWidthPixels = LocalWindowInfo.current.containerSize.width
    var isPressed by remember(key) { mutableStateOf(false) }
    var alternativesVisible by remember(key) { mutableStateOf(false) }
    var selectedAlternativeIndex by remember(key) {
        mutableStateOf(key.defaultMenuItemIndex())
    }
    var keyBounds by remember { mutableStateOf(Rect.Zero) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) PressedKeyScale else 1f,
        animationSpec = tween(durationMillis = PressAnimationDurationMillis),
        label = "масштаб_нажатой_клавиши"
    )

    val keyColor = when {
        key.isActive -> palette.keyAccent
        key.isModifier || key.code == KeyboardKeyCodes.ENTER -> palette.keySecondary
        else -> palette.key
    }
    val keyShape = key.shape(isBottomRow = isBottomRow)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .onGloballyPositioned { coordinates ->
                keyBounds = coordinates.boundsInWindow()
            }
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale
            )
            .clip(shape = keyShape)
            .background(color = keyColor)
            .semantics {
                onClick(label = key.accessibilityDescription()) {
                    onClick()
                    true
                }

                if (key.menuItemCount() > 0 || key.repeatOnLongPress) {
                    onLongClick(label = key.longPressDescription()) {
                        when {
                            key.alternatives.isNotEmpty() -> {
                                onAlternativeSelected(
                                    key.alternatives[
                                        key.defaultMenuItemIndex()
                                            .coerceAtMost(key.alternatives.lastIndex)
                                    ]
                                )
                            }

                            key.longPressAction != null -> {
                                onLongPressAction(key.longPressAction)
                            }

                            key.repeatOnLongPress -> onClick()
                        }

                        true
                    }
                }
            }
            .pointerInput(
                key,
                longPressTimeoutMillis,
                isHapticFeedbackEnabled
            ) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var releasedBeforeLongPress = false
                    val holdDelayMillis = if (key.repeatOnLongPress) {
                        RepeatDeleteStartDelayMillis
                    } else {
                        longPressTimeoutMillis
                    }

                    isPressed = true

                    withTimeoutOrNull(timeMillis = holdDelayMillis) {
                        while (!releasedBeforeLongPress) {
                            val change = awaitPointerEvent()
                                .changes
                                .firstOrNull { pointerChange ->
                                    pointerChange.id == down.id
                                }

                            if (change == null || !change.pressed) {
                                releasedBeforeLongPress = true
                            }
                        }
                    }

                    if (releasedBeforeLongPress) {
                        isPressed = false
                        onClick()
                        return@awaitEachGesture
                    }

                    if (key.repeatOnLongPress) {
                        var canContinueDeleting = onClick()
                        var released = false

                        while (!released) {
                            val event = withTimeoutOrNull(
                                timeMillis = RepeatDeleteIntervalMillis
                            ) {
                                awaitPointerEvent()
                            }

                            if (event == null) {
                                if (canContinueDeleting) {
                                    canContinueDeleting = onClick()
                                }
                            } else {
                                val change = event.changes.firstOrNull { pointerChange ->
                                    pointerChange.id == down.id
                                }
                                released = change == null || !change.pressed
                            }
                        }

                        isPressed = false
                        return@awaitEachGesture
                    }

                    if (key.menuItemCount() == 0) {
                        var released = false

                        while (!released) {
                            val change = awaitPointerEvent()
                                .changes
                                .firstOrNull { pointerChange ->
                                    pointerChange.id == down.id
                                }
                            released = change == null || !change.pressed
                        }

                        isPressed = false
                        onClick()
                        return@awaitEachGesture
                    }

                    if (isHapticFeedbackEnabled) {
                        hapticFeedback.performHapticFeedback(
                            HapticFeedbackType.LongPress
                        )
                    }

                    selectedAlternativeIndex = key.defaultMenuItemIndex()
                    alternativesVisible = true
                    onAlternativeMenuVisibilityChanged(true)

                    var released = false

                    try {
                        while (!released) {
                            val change = awaitPointerEvent()
                                .changes
                                .firstOrNull { pointerChange ->
                                    pointerChange.id == down.id
                                }

                            if (change == null || !change.pressed) {
                                released = true
                            } else {
                                selectedAlternativeIndex = alternativeIndexAt(
                                    key = key,
                                    keyBounds = keyBounds,
                                    pointerX = keyBounds.left + change.position.x,
                                    pointerY = keyBounds.top + change.position.y,
                                    density = density,
                                    screenWidthPixels = screenWidthPixels
                                )
                            }
                        }
                    } finally {
                        alternativesVisible = false
                        onAlternativeMenuVisibilityChanged(false)
                        isPressed = false
                    }

                    when {
                        selectedAlternativeIndex < key.alternatives.size -> {
                            key.alternatives
                                .getOrNull(selectedAlternativeIndex)
                                ?.let(onAlternativeSelected)
                        }

                        selectedAlternativeIndex == key.alternatives.size -> {
                            key.longPressAction?.let(onLongPressAction)
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        KeyContent(
            key = key,
            palette = palette
        )
    }

    if (alternativesVisible) {
        AlternativesPopup(
            key = key,
            keyBounds = keyBounds,
            palette = palette,
            selectedIndex = selectedAlternativeIndex
        )
    }
}

@Composable
private fun KeyContent(
    key: KeyInfo,
    palette: KeyboardPalette
) {
    val contentColor = if (key.isModifier) {
        palette.textSecondary
    } else {
        palette.text
    }

    Box(modifier = Modifier.fillMaxSize()) {
        key.icon?.let { icon ->
            if (icon == KeyIcon.NUMBER_PAD) {
                NumberPadIcon(
                    color = contentColor,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                val drawableResource = icon.drawableResource()

                if (drawableResource != null) {
                    Icon(
                        painter = painterResource(id = drawableResource),
                        contentDescription = icon.contentDescription(),
                        tint = contentColor,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(icon.size())
                    )
                } else {
                    Icon(
                        imageVector = icon.imageVector(),
                        contentDescription = icon.contentDescription(),
                        tint = contentColor,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(icon.size())
                    )
                }
            }
        }

        key.label?.let { label ->
            Text(
                text = label,
                color = contentColor,
                fontSize = key.fontSize ?: key.labelFontSize(label = label),
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                modifier = if (key.hintIcon == null) {
                    Modifier.align(Alignment.Center)
                } else {
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = EmojiCommaLabelBottomPadding)
                }
            )
        }

        key.hint?.let { hint ->
            Text(
                text = hint,
                color = palette.textSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 3.dp, end = 5.dp)
            )
        }

        key.hintIcon?.let { icon ->
            Icon(
                imageVector = icon.imageVector(),
                contentDescription = null,
                tint = palette.textSecondary,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = EmojiHintIconTopPadding)
                    .size(EmojiHintIconSize)
            )
        }
    }
}

@Composable
private fun NumberPadIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    /*
     * Gboard обозначает цифровую раскладку двумя строками «12» и «34».
     * Отдельный составной знак сохраняет это начертание при любом масштабе
     * шрифта и не зависит от наличия похожей пиктограммы в наборе Material.
     */
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "12",
            color = color,
            fontSize = NumberPadIconFontSize,
            lineHeight = NumberPadIconLineHeight,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium
        )

        Text(
            text = "34",
            color = color,
            fontSize = NumberPadIconFontSize,
            lineHeight = NumberPadIconLineHeight,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun KeyInfo.labelFontSize(label: String): TextUnit {
    return when {
        hintIcon != null -> EmojiCommaLabelFontSize
        code == KeyboardKeyCodes.SPACE -> SpacebarLabelFontSize
        label.length > LongKeyLabelCharacterCount -> LongKeyLabelFontSize
        else -> KeyLabelFontSize
    }
}

private fun KeyInfo.menuItemCount(): Int {
    return alternatives.size + if (longPressAction == null) 0 else 1
}

private fun KeyInfo.menuRowLengths(): List<Int> {
    val itemCount = menuItemCount()
    val hasValidRows = alternativeRowLengths.isNotEmpty() &&
        alternativeRowLengths.all { rowLength -> rowLength > 0 } &&
        alternativeRowLengths.sum() == itemCount

    return if (hasValidRows) {
        alternativeRowLengths
    } else {
        listOf(itemCount)
    }
}

private fun KeyInfo.defaultMenuItemIndex(): Int {
    val itemCount = menuItemCount()

    if (itemCount == 0) {
        return 0
    }

    val inferredIndex = alternatives.indexOfFirst { alternative ->
        alternative != label
    }.coerceAtLeast(0)

    return (preferredAlternativeIndex ?: inferredIndex).coerceIn(
        minimumValue = 0,
        maximumValue = itemCount - 1
    )
}

private fun KeyInfo.shape(isBottomRow: Boolean): RoundedCornerShape {
    val usesPillShape = isBottomRow && (
        code == KeyboardKeyCodes.SYMBOLS ||
            code == KeyboardKeyCodes.MODE_ALPHA ||
            code == KeyboardKeyCodes.ENTER
        )

    return RoundedCornerShape(
        size = if (usesPillShape) {
            BottomPillKeyCornerRadius
        } else {
            KeyCornerRadius
        }
    )
}

private fun KeyInfo.accessibilityDescription(): String {
    return label ?: icon?.contentDescription() ?: "Клавиша"
}

private fun KeyInfo.longPressDescription(): String {
    return when {
        alternatives.isNotEmpty() -> "Показать альтернативные символы"
        longPressAction != null -> longPressAction.contentDescription()
        repeatOnLongPress -> "Удалять непрерывно"
        else -> "Удержать клавишу"
    }
}

private fun alternativeIndexAt(
    key: KeyInfo,
    keyBounds: Rect,
    pointerX: Float,
    pointerY: Float,
    density: Density,
    screenWidthPixels: Int
): Int {
    val geometry = alternativePopupGeometry(
        key = key,
        keyBounds = keyBounds,
        density = density,
        screenWidthPixels = screenWidthPixels
    )
    val defaultLocation = menuLocation(
        index = key.defaultMenuItemIndex(),
        rowLengths = geometry.rowLengths
    )
    val popupBottom = geometry.popupTopPixels + geometry.popupHeightPixels
    val pointerInsidePopupVertically = pointerY >= geometry.popupTopPixels &&
        pointerY < popupBottom
    val row = if (pointerInsidePopupVertically) {
        (
            (pointerY - geometry.popupTopPixels - geometry.verticalPaddingPixels) /
                geometry.rowHeightPixels
            ).toInt().coerceIn(
            minimumValue = 0,
            maximumValue = geometry.rowLengths.lastIndex
        )
    } else {
        defaultLocation.row
    }
    val rowLength = geometry.rowLengths[row]
    val rowHorizontalInset = (
        geometry.maxColumnCount - rowLength
        ) * geometry.itemWidthPixels / 2f
    val contentX = pointerX -
        geometry.popupLeftPixels -
        geometry.horizontalPaddingPixels -
        rowHorizontalInset
    val column = (contentX / geometry.itemWidthPixels)
        .toInt()
        .coerceIn(
            minimumValue = 0,
            maximumValue = rowLength - 1
        )
    val rowStartIndex = geometry.rowLengths
        .take(row)
        .sum()

    return rowStartIndex + column
}

@Composable
private fun AlternativesPopup(
    key: KeyInfo,
    keyBounds: Rect,
    palette: KeyboardPalette,
    selectedIndex: Int
) {
    val density = LocalDensity.current
    val popupMarginPixels = with(density) { PopupWindowMargin.roundToPx() }
    val preferredItemCenterPixels = key.preferredItemCenterPixels(density = density)
    val anchorCenterPixels = keyBounds.center.x.roundToInt()
    val anchorTopPixels = keyBounds.top.roundToInt()
    val positionProvider = remember(
        popupMarginPixels,
        preferredItemCenterPixels,
        anchorCenterPixels,
        anchorTopPixels
    ) {
        AlternativePopupPositionProvider(
            marginPixels = popupMarginPixels,
            preferredItemCenterPixels = preferredItemCenterPixels,
            anchorCenterPixels = anchorCenterPixels,
            anchorTopPixels = anchorTopPixels
        )
    }
    val rowLengths = key.menuRowLengths()
    val maximumColumnCount = rowLengths.max()

    Popup(
        popupPositionProvider = positionProvider,
        properties = PopupProperties(
            focusable = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            clippingEnabled = false
        )
    ) {
        Surface(
            modifier = Modifier
                .width(
                    AlternativeItemWidth * maximumColumnCount +
                        PopupHorizontalPadding * 2
                )
                .height(
                    AlternativeRowHeight * rowLengths.size +
                        PopupVerticalPadding * 2
                ),
            color = palette.popup,
            shape = RoundedCornerShape(size = PopupCornerRadius),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = PopupHorizontalPadding,
                        vertical = PopupVerticalPadding
                    ),
                verticalArrangement = Arrangement.Center
            ) {
                var firstIndexInRow = 0

                rowLengths.forEach { rowLength ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(AlternativeRowHeight),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(times = rowLength) { column ->
                            val index = firstIndexInRow + column

                            AlternativeMenuItem(
                                key = key,
                                index = index,
                                isSelected = index == selectedIndex,
                                palette = palette
                            )
                        }
                    }

                    firstIndexInRow += rowLength
                }
            }
        }
    }
}

@Composable
private fun AlternativeMenuItem(
    key: KeyInfo,
    index: Int,
    isSelected: Boolean,
    palette: KeyboardPalette
) {
    Box(
        modifier = Modifier
            .width(AlternativeItemWidth)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(AlternativeSelectedItemSize)
                .clip(shape = CircleShape)
                .background(
                    color = if (isSelected) {
                        palette.popupSelected
                    } else {
                        Color.Transparent
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            val contentColor = if (isSelected) {
                palette.popupSelectedText
            } else {
                palette.text
            }

            if (index < key.alternatives.size) {
                Text(
                    text = key.alternatives[index],
                    color = contentColor,
                    fontSize = AlternativeLabelFontSize,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal
                )
            } else {
                key.longPressAction?.let { action ->
                    Icon(
                        imageVector = action.icon().imageVector(),
                        contentDescription = action.contentDescription(),
                        tint = contentColor,
                        modifier = Modifier.size(AlternativeActionIconSize)
                    )
                }
            }
        }
    }
}

private fun alternativePopupGeometry(
    key: KeyInfo,
    keyBounds: Rect,
    density: Density,
    screenWidthPixels: Int
): AlternativePopupGeometry {
    val rowLengths = key.menuRowLengths()
    val maximumColumnCount = rowLengths.max()
    val itemWidthPixels = with(density) { AlternativeItemWidth.toPx() }
    val rowHeightPixels = with(density) { AlternativeRowHeight.toPx() }
    val horizontalPaddingPixels = with(density) { PopupHorizontalPadding.toPx() }
    val verticalPaddingPixels = with(density) { PopupVerticalPadding.toPx() }
    val popupMarginPixels = with(density) { PopupWindowMargin.roundToPx() }
    val popupWidthPixels = (
        itemWidthPixels * maximumColumnCount +
            horizontalPaddingPixels * 2
        ).roundToInt()
    val popupHeightPixels = (
        rowHeightPixels * rowLengths.size +
            verticalPaddingPixels * 2
        ).roundToInt()
    val preferredItemCenterPixels = key.preferredItemCenterPixels(density = density)
    val popupLeftPixels = (
        keyBounds.center.x - preferredItemCenterPixels
        ).roundToInt().coerceIn(
        minimumValue = popupMarginPixels,
        maximumValue = (
            screenWidthPixels - popupWidthPixels - popupMarginPixels
            ).coerceAtLeast(popupMarginPixels)
    )

    return AlternativePopupGeometry(
        rowLengths = rowLengths,
        maxColumnCount = maximumColumnCount,
        itemWidthPixels = itemWidthPixels,
        rowHeightPixels = rowHeightPixels,
        horizontalPaddingPixels = horizontalPaddingPixels,
        verticalPaddingPixels = verticalPaddingPixels,
        popupLeftPixels = popupLeftPixels,
        popupTopPixels = keyBounds.top -
            popupHeightPixels -
            popupMarginPixels,
        popupHeightPixels = popupHeightPixels
    )
}

private fun KeyInfo.preferredItemCenterPixels(density: Density): Int {
    val rowLengths = menuRowLengths()
    val maximumColumnCount = rowLengths.max()
    val location = menuLocation(
        index = defaultMenuItemIndex(),
        rowLengths = rowLengths
    )
    val itemWidthPixels = with(density) { AlternativeItemWidth.toPx() }
    val horizontalPaddingPixels = with(density) { PopupHorizontalPadding.toPx() }
    val rowHorizontalInset = (
        maximumColumnCount - rowLengths[location.row]
        ) * itemWidthPixels / 2f

    return (
        horizontalPaddingPixels +
            rowHorizontalInset +
            (location.column + 0.5f) * itemWidthPixels
        ).roundToInt()
}

private fun menuLocation(
    index: Int,
    rowLengths: List<Int>
): AlternativeMenuLocation {
    var rowStartIndex = 0

    rowLengths.forEachIndexed { row, rowLength ->
        val rowEndIndex = rowStartIndex + rowLength

        if (index < rowEndIndex) {
            return AlternativeMenuLocation(
                row = row,
                column = index - rowStartIndex
            )
        }

        rowStartIndex = rowEndIndex
    }

    return AlternativeMenuLocation(
        row = rowLengths.lastIndex,
        column = rowLengths.last() - 1
    )
}

private data class AlternativeMenuLocation(
    val row: Int,
    val column: Int
)

private data class AlternativePopupGeometry(
    val rowLengths: List<Int>,
    val maxColumnCount: Int,
    val itemWidthPixels: Float,
    val rowHeightPixels: Float,
    val horizontalPaddingPixels: Float,
    val verticalPaddingPixels: Float,
    val popupLeftPixels: Int,
    val popupTopPixels: Float,
    val popupHeightPixels: Int
)

private class AlternativePopupPositionProvider(
    private val marginPixels: Int,
    private val preferredItemCenterPixels: Int,
    private val anchorCenterPixels: Int,
    private val anchorTopPixels: Int
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        /*
         * Плашка совмещает с удерживаемой клавишей выбранный по умолчанию
         * символ, как Gboard. Ограничение по краям экрана не позволяет панели
         * выйти за доступную ширину даже для двух рядов пунктуации.
         */
        val x = (anchorCenterPixels - preferredItemCenterPixels)
            .coerceIn(
                minimumValue = marginPixels,
                maximumValue = (
                    windowSize.width - popupContentSize.width - marginPixels
                    ).coerceAtLeast(marginPixels)
            )
        val y = anchorTopPixels - popupContentSize.height - marginPixels

        return IntOffset(x = x, y = y)
    }
}

@Composable
private fun EmojiPanel(
    isDark: Boolean,
    palette: KeyboardPalette,
    onEmojiPicked: (String) -> Unit
) {
    /*
     * AndroidX поставляет актуальный список эмодзи, категории, недавние элементы
     * и варианты оттенков кожи. AndroidView оставляет эту логику библиотеке,
     * а Compose отвечает за общую геометрию клавиатуры.
     */
    key(isDark) {
        AndroidView(
            factory = { context ->
                val theme = if (isDark) {
                    R.style.KeyboardEmojiPickerDark
                } else {
                    R.style.KeyboardEmojiPickerLight
                }
                val themedContext = ContextThemeWrapper(context, theme)

                EmojiPickerView(themedContext).apply {
                    emojiGridColumns = EmojiColumnCount
                    setBackgroundColor(palette.background.toArgb())
                    setOnEmojiPickedListener { item ->
                        onEmojiPicked(item.emoji)
                    }

                    /*
                     * При пустой истории штатный выбор эмодзи занимает начало списка
                     * сообщением-заглушкой. Начальная прокрутка сразу раскрывает
                     * первую полезную категорию; к недавним элементам по-прежнему
                     * можно вернуться кнопкой с часами.
                     */
                    post {
                        val body = findViewById<RecyclerView>(
                            EmojiPickerResources.id.emoji_picker_body
                        )

                        (body?.layoutManager as? GridLayoutManager)
                            ?.scrollToPositionWithOffset(
                                FirstEmojiCategoryPosition,
                                0
                            )
                    }
                }
            },
            update = { picker ->
                picker.setBackgroundColor(palette.background.toArgb())
                picker.setOnEmojiPickedListener { item ->
                    onEmojiPicked(item.emoji)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(EmojiPanelBodyHeight)
        )
    }
}

@Composable
private fun StickersPanel(
    palette: KeyboardPalette,
    supportsStickerContent: Boolean,
    onStickerPicked: (Sticker) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(StickerColumnCount),
        modifier = Modifier
            .fillMaxWidth()
            .height(StickerPanelBodyHeight)
            .padding(
                horizontal = KeyboardHorizontalPadding,
                vertical = KeyboardVerticalPadding
            ),
        horizontalArrangement = Arrangement.spacedBy(PanelItemGap),
        verticalArrangement = Arrangement.spacedBy(PanelItemGap)
    ) {
        if (!supportsStickerContent) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Это поле не принимает изображения",
                    color = palette.textSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                )
            }
        }

        items(
            items = StickerCatalog.items,
            key = Sticker::identifier
        ) { sticker ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(StickerItemHeight)
                    .clip(shape = RoundedCornerShape(size = KeyCornerRadius))
                    .background(color = palette.key)
                    .combinedClickable(
                        enabled = supportsStickerContent,
                        onClick = {
                            onStickerPicked(sticker)
                        },
                        onLongClick = {}
                    )
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(sticker.drawableResource),
                    contentDescription = sticker.description,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun ClipboardPanel(
    clipboardText: String?,
    palette: KeyboardPalette,
    onPaste: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(PanelBodyHeight)
            .padding(
                horizontal = 12.dp,
                vertical = 10.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        if (clipboardText.isNullOrBlank()) {
            Text(
                text = "Буфер обмена пуст",
                color = palette.textSecondary,
                fontSize = 16.sp
            )
        } else {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .combinedClickable(
                        onClick = { onPaste(clipboardText) },
                        onLongClick = {}
                    ),
                color = palette.key,
                shape = RoundedCornerShape(size = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Нажмите, чтобы вставить",
                        color = palette.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = clipboardText,
                        color = palette.text,
                        fontSize = 16.sp,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun PanelNavigation(
    palette: KeyboardPalette,
    onBack: () -> Unit,
    onBackspace: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(PanelNavigationHeight)
            .padding(
                start = KeyboardHorizontalPadding,
                end = KeyboardHorizontalPadding,
                bottom = KeyboardVerticalPadding
            ),
        horizontalArrangement = Arrangement.spacedBy(KeyHorizontalGap)
    ) {
        Box(
            modifier = Modifier
                .width(76.dp)
                .fillMaxHeight()
                .clip(shape = RoundedCornerShape(size = KeyCornerRadius))
                .background(color = palette.keySecondary)
                .combinedClickable(
                    onClick = onBack,
                    onLongClick = {}
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "АБВ",
                color = palette.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .width(76.dp)
                .fillMaxHeight()
                .clip(shape = RoundedCornerShape(size = KeyCornerRadius))
                .background(color = palette.keySecondary)
                .combinedClickable(
                    onClick = onBackspace,
                    onLongClick = {}
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Backspace,
                contentDescription = "Удалить",
                tint = palette.text
            )
        }
    }
}

private fun KeyIcon.imageVector(): ImageVector {
    return when (this) {
        KeyIcon.BACKSPACE -> Icons.AutoMirrored.Rounded.Backspace
        KeyIcon.DONE -> Icons.Rounded.Done
        KeyIcon.EMOJI -> Icons.Outlined.EmojiEmotions
        KeyIcon.ENTER -> Icons.AutoMirrored.Rounded.KeyboardReturn
        KeyIcon.LANGUAGE -> Icons.Rounded.Language
        KeyIcon.SEARCH -> Icons.Rounded.Search
        KeyIcon.SEND -> Icons.AutoMirrored.Rounded.Send
        KeyIcon.SHIFT -> Icons.Rounded.KeyboardCapslock
        KeyIcon.NUMBER_PAD -> Icons.Rounded.Dialpad
    }
}

private fun KeyIcon.drawableResource(): Int? {
    return when (this) {
        KeyIcon.BACKSPACE -> R.drawable.ic_outline_backspace_24px
        KeyIcon.ENTER -> R.drawable.ic_outline_keyboard_return_24px
        KeyIcon.SHIFT -> R.drawable.ic_outline_shift_24px
        else -> null
    }
}

private fun KeyIcon.size(): Dp {
    return when (this) {
        KeyIcon.BACKSPACE -> 28.dp
        KeyIcon.ENTER -> 30.dp
        KeyIcon.SHIFT -> 27.dp
        else -> 24.dp
    }
}

private fun KeyLongPressAction.contentDescription(): String {
    return when (this) {
        KeyLongPressAction.SHOW_EMOJI -> "Открыть эмодзи"
    }
}

private fun KeyLongPressAction.icon(): KeyIcon {
    return when (this) {
        KeyLongPressAction.SHOW_EMOJI -> KeyIcon.EMOJI
    }
}

private fun KeyIcon.contentDescription(): String {
    return when (this) {
        KeyIcon.BACKSPACE -> "Удалить"
        KeyIcon.DONE -> "Готово"
        KeyIcon.EMOJI -> "Эмодзи"
        KeyIcon.ENTER -> "Ввод"
        KeyIcon.LANGUAGE -> "Сменить язык"
        KeyIcon.SEARCH -> "Искать"
        KeyIcon.SEND -> "Отправить"
        KeyIcon.SHIFT -> "Регистр"
        KeyIcon.NUMBER_PAD -> "Цифровая раскладка"
    }
}

private data class KeyboardPalette(
    val background: Color,
    val key: Color,
    val keySecondary: Color,
    val keyAccent: Color,
    val popup: Color,
    val popupSelected: Color,
    val popupSelectedText: Color,
    val text: Color,
    val textSecondary: Color,
    val divider: Color
)

private fun keyboardPalette(isDark: Boolean): KeyboardPalette {
    return if (isDark) {
        KeyboardPalette(
            background = Color(0xFF1E1F25),
            key = Color(0xFF33343A),
            keySecondary = Color(0xFF404659),
            keyAccent = Color(0xFF404659),
            popup = Color(0xFF33343A),
            popupSelected = Color(0xFFAFC6FF),
            popupSelectedText = Color(0xFF152E60),
            text = Color(0xFFE2E2E9),
            textSecondary = Color(0xFFC4C6D0),
            divider = Color(0xFF51525A)
        )
    } else {
        KeyboardPalette(
            background = Color(0xFFEEEDF4),
            key = Color(0xFFFFFFFF),
            keySecondary = Color(0xFFDCE2F9),
            keyAccent = Color(0xFFDCE2F9),
            popup = Color(0xFFFFFFFF),
            popupSelected = Color(0xFF475D92),
            popupSelectedText = Color(0xFFFFFFFF),
            text = Color(0xFF1B1B1F),
            textSecondary = Color(0xFF45464F),
            divider = Color(0xFFC5C6CE)
        )
    }
}

private const val MaxSuggestionCount = 3
private const val EmojiColumnCount = 9
private const val FirstEmojiCategoryPosition = 2
private const val StickerColumnCount = 2
private const val PressAnimationDurationMillis = 70
private const val PressedKeyScale = 0.96f
private const val AlternativeMenuBackgroundAlpha = 0.4f
private const val RepeatDeleteStartDelayMillis = 1_000L
private const val RepeatDeleteIntervalMillis = 70L
private const val AndroidResourcePackage = "android"
private const val DimensionResourceType = "dimen"
private const val InputMethodNavigationBarHeightResourceName =
    "input_method_navigation_bar_height"
private const val NavigationBarFrameHeightResourceName = "navigation_bar_frame_height"
private const val LongKeyLabelCharacterCount = 3

private val ToolbarHeight = 50.dp
private val ToolbarButtonSize = 44.dp
private val ToolbarIconSize = 20.dp
private val SuggestionDividerWidth = 1.dp
private val SuggestionDividerHeight = 24.dp
private val KeyHeight = 47.dp
private val KeyHorizontalGap = 5.dp
private val KeyVerticalGap = 12.dp
private val PanelItemGap = 5.dp
private val KeyCornerRadius = 8.dp
private val BottomPillKeyCornerRadius = 24.dp
private val KeyboardHorizontalPadding = 4.5.dp
private val KeyboardVerticalPadding = 6.dp
private val KeyboardNavigationContentGap = 6.dp
private val PanelBodyHeight = 160.dp
private val ActionsPanelBodyHeight =
    KeyHeight * 4 + KeyVerticalGap * 3 + KeyboardVerticalPadding * 2
private val EmojiPanelBodyHeight = 270.dp
private val StickerPanelBodyHeight = 270.dp
private val StickerItemHeight = 122.dp
private val PanelNavigationHeight = 49.dp
private val NumberGridHeight = KeyHeight * 3 + KeyVerticalGap * 2
private const val NumberSideColumnWeight = 0.85f
private const val NumberDigitGridWeight = 4.25f
private val AlternativeItemWidth = 40.dp
private val AlternativeRowHeight = 46.dp
private val AlternativeSelectedItemSize = 44.dp
private val PopupHorizontalPadding = 12.dp
private val PopupVerticalPadding = 4.dp
private val PopupCornerRadius = 35.dp
private val PopupWindowMargin = 6.dp
private val KeyLabelFontSize = 28.sp
private val AlternativeLabelFontSize = 26.sp
private val LongKeyLabelFontSize = 18.sp
private val SpacebarLabelFontSize = 14.sp
private val EmojiCommaLabelFontSize = 18.sp
private val NumberPadIconFontSize = 13.sp
private val NumberPadIconLineHeight = 12.sp
private val EmojiHintIconSize = 14.dp
private val AlternativeActionIconSize = 28.dp
private val EmojiHintIconTopPadding = 4.dp
private val EmojiCommaLabelBottomPadding = 3.dp
private val ActionsPanelHorizontalPadding = 12.dp
private val ActionsPanelVerticalPadding = 24.dp
private val ActionPanelItemWidth = 104.dp
private val ActionPanelCardWidth = 80.dp
private val ActionPanelCardHeight = 48.dp
private val ActionPanelCardCornerRadius = 10.dp
private val ActionPanelIconSize = 26.dp
private val ActionPanelLabelTopPadding = 6.dp
private val ActionPanelLabelFontSize = 14.sp
private val ActionPanelLabelLineHeight = 17.sp
