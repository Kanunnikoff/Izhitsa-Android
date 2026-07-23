package software.kanunnikoff.izhitsa.compose

import android.view.ContextThemeWrapper
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.KeyboardReturn
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.emoji2.emojipicker.EmojiPickerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import software.kanunnikoff.izhitsa.R
import software.kanunnikoff.izhitsa.stickers.Sticker
import software.kanunnikoff.izhitsa.stickers.StickerCatalog
import kotlin.math.roundToInt
import androidx.emoji2.emojipicker.R as EmojiPickerResources

data class KeyInfo(
    val code: Int,
    val label: String? = null,
    val icon: KeyIcon? = null,
    val hint: String? = null,
    val hintIcon: KeyIcon? = null,
    val alternatives: List<String> = emptyList(),
    val longPressAction: KeyLongPressAction? = null,
    val weight: Float = 1f,
    val isModifier: Boolean = false,
    val isActive: Boolean = false
)

enum class KeyLongPressAction {
    SHOW_EMOJI
}

enum class KeyboardPanel {
    KEYS,
    EMOJI,
    STICKERS,
    CLIPBOARD
}

enum class KeyboardToolbarAction {
    SWITCH_INPUT_METHOD,
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
    onKeyClick: (KeyInfo) -> Unit,
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = palette.background)
            .padding(bottom = bottomInset)
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
                        onKeyClick = onKeyClick,
                        onKeyLongPressAction = onKeyLongPressAction,
                        onAlternativeSelected = onAlternativeSelected
                    )
                } else {
                    KeyboardRows(
                        rows = rows,
                        palette = palette,
                        onKeyClick = onKeyClick,
                        onKeyLongPressAction = onKeyLongPressAction,
                        onAlternativeSelected = onAlternativeSelected
                    )
                }
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
            .height(ToolbarHeight)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (suggestions.isNotEmpty()) {
            suggestions.take(MaxSuggestionCount).forEach { suggestion ->
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
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            ToolbarIcon(
                imageVector = Icons.Rounded.Apps,
                contentDescription = "Сменить клавиатуру",
                palette = palette,
                onClick = { onToolbarAction(KeyboardToolbarAction.SWITCH_INPUT_METHOD) }
            )

            Spacer(modifier = Modifier.weight(1f))

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
private fun ToolbarIcon(
    imageVector: ImageVector,
    contentDescription: String,
    palette: KeyboardPalette,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = palette.textSecondary,
            modifier = Modifier.size(23.dp)
        )
    }
}

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
    onKeyClick: (KeyInfo) -> Unit,
    onKeyLongPressAction: (KeyLongPressAction) -> Unit,
    onAlternativeSelected: (KeyInfo, String) -> Unit
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
                        onClick = { onKeyClick(key) },
                        onLongPressAction = onKeyLongPressAction,
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
    onKeyClick: (KeyInfo) -> Unit,
    onKeyLongPressAction: (KeyLongPressAction) -> Unit,
    onAlternativeSelected: (KeyInfo, String) -> Unit
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
                                onClick = { onKeyClick(key) },
                                onLongPressAction = onKeyLongPressAction,
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
                        onClick = { onKeyClick(key) },
                        onLongPressAction = onKeyLongPressAction,
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
                    onClick = { onKeyClick(key) },
                    onLongPressAction = onKeyLongPressAction,
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
    onKeyClick: (KeyInfo) -> Unit
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
                    fontSize = KeyLabelFontSize,
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
    onClick: () -> Unit,
    onLongPressAction: (KeyLongPressAction) -> Unit,
    onAlternativeSelected: (String) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var alternativesVisible by remember(key) { mutableStateOf(false) }
    var keyBounds by remember { mutableStateOf(Rect.Zero) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) PressedKeyScale else 1f,
        animationSpec = tween(durationMillis = PressAnimationDurationMillis),
        label = "масштаб_нажатой_клавиши"
    )

    val keyColor = when {
        key.isActive -> palette.keyAccent
        key.isModifier || key.code < 0 || key.code == KeyboardKeyCodes.ENTER -> palette.keySecondary
        else -> palette.key
    }

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
            .clip(shape = RoundedCornerShape(size = KeyCornerRadius))
            .background(color = keyColor)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClickLabel = key.longPressAction?.contentDescription(),
                onLongClick = {
                    when {
                        key.alternatives.isNotEmpty() -> alternativesVisible = true
                        key.longPressAction != null -> onLongPressAction(key.longPressAction)
                    }
                }
            ),
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
            onDismiss = { alternativesVisible = false },
            onSelected = { alternative ->
                alternativesVisible = false
                onAlternativeSelected(alternative)
            }
        )
    }
}

@Composable
private fun KeyContent(
    key: KeyInfo,
    palette: KeyboardPalette
) {
    Box(modifier = Modifier.fillMaxSize()) {
        key.icon?.let { icon ->
            Icon(
                imageVector = icon.imageVector(),
                contentDescription = icon.contentDescription(),
                tint = palette.text,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(24.dp)
            )
        }

        key.label?.let { label ->
            Text(
                text = label,
                color = palette.text,
                fontSize = key.labelFontSize(label = label),
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

private fun KeyInfo.labelFontSize(label: String): TextUnit {
    return when {
        hintIcon != null -> EmojiCommaLabelFontSize
        code == KeyboardKeyCodes.SPACE -> SpacebarLabelFontSize
        label.length > LongKeyLabelCharacterCount -> LongKeyLabelFontSize
        else -> KeyLabelFontSize
    }
}

@Composable
private fun AlternativesPopup(
    key: KeyInfo,
    keyBounds: Rect,
    palette: KeyboardPalette,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit
) {
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val popupWidth = AlternativeItemWidth * key.alternatives.size + PopupHorizontalPadding * 2
    val popupWidthPixels = with(density) { popupWidth.roundToPx() }
    val popupHeightPixels = with(density) { PopupHeight.roundToPx() }
    val popupMarginPixels = with(density) { PopupWindowMargin.roundToPx() }
    val screenWidthPixels = windowInfo.containerSize.width

    /*
     * Popup позиционируется в координатах окна метода ввода. Ограничение по краям
     * нужно для крайних букв, иначе часть исторических вариантов окажется за экраном.
     */
    val popupX = (keyBounds.center.x - popupWidthPixels / 2f)
        .roundToInt()
        .coerceIn(
            minimumValue = popupMarginPixels,
            maximumValue = (screenWidthPixels - popupWidthPixels - popupMarginPixels)
                .coerceAtLeast(popupMarginPixels)
        )
    val popupY = (keyBounds.top - popupHeightPixels - popupMarginPixels)
        .roundToInt()
        .coerceAtLeast(popupMarginPixels)

    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(
            x = popupX,
            y = popupY
        ),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Surface(
            color = palette.popup,
            shape = RoundedCornerShape(size = PopupCornerRadius),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .height(PopupHeight)
                    .padding(horizontal = PopupHorizontalPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                key.alternatives.forEachIndexed { index, alternative ->
                    Box(
                        modifier = Modifier
                            .width(AlternativeItemWidth)
                            .fillMaxHeight()
                            .padding(vertical = 5.dp)
                            .clip(shape = RoundedCornerShape(size = 10.dp))
                            .background(
                                color = if (index == 0) {
                                    palette.keyAccent
                                } else {
                                    Color.Transparent
                                }
                            )
                            .combinedClickable(
                                onClick = { onSelected(alternative) },
                                onLongClick = {}
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = alternative,
                            color = palette.text,
                            fontSize = KeyLabelFontSize,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }
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
        KeyIcon.SHIFT -> Icons.Rounded.ArrowUpward
    }
}

private fun KeyLongPressAction.contentDescription(): String {
    return when (this) {
        KeyLongPressAction.SHOW_EMOJI -> "Открыть эмодзи"
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
    }
}

private data class KeyboardPalette(
    val background: Color,
    val key: Color,
    val keySecondary: Color,
    val keyAccent: Color,
    val popup: Color,
    val text: Color,
    val textSecondary: Color
)

private fun keyboardPalette(isDark: Boolean): KeyboardPalette {
    return if (isDark) {
        KeyboardPalette(
            background = Color(0xFF202124),
            key = Color(0xFF303134),
            keySecondary = Color(0xFF3C4043),
            keyAccent = Color(0xFF3F5F86),
            popup = Color(0xFF35373B),
            text = Color(0xFFF1F3F4),
            textSecondary = Color(0xFFBDC1C6)
        )
    } else {
        KeyboardPalette(
            background = Color(0xFFF7F7FF),
            key = Color(0xFFFFFFFF),
            keySecondary = Color(0xFFDDE6FF),
            keyAccent = Color(0xFFC2E7FF),
            popup = Color(0xFFFFFFFF),
            text = Color(0xFF202124),
            textSecondary = Color(0xFF5F6368)
        )
    }
}

private const val MaxSuggestionCount = 3
private const val EmojiColumnCount = 9
private const val FirstEmojiCategoryPosition = 2
private const val StickerColumnCount = 2
private const val PressAnimationDurationMillis = 70
private const val PressedKeyScale = 0.96f
private const val AndroidResourcePackage = "android"
private const val DimensionResourceType = "dimen"
private const val InputMethodNavigationBarHeightResourceName =
    "input_method_navigation_bar_height"
private const val NavigationBarFrameHeightResourceName = "navigation_bar_frame_height"
private const val LongKeyLabelCharacterCount = 3

private val ToolbarHeight = 50.dp
private val KeyHeight = 47.dp
private val KeyHorizontalGap = 5.dp
private val KeyVerticalGap = 12.dp
private val PanelItemGap = 5.dp
private val KeyCornerRadius = 10.dp
private val KeyboardHorizontalPadding = 6.dp
private val KeyboardVerticalPadding = 6.dp
private val KeyboardNavigationContentGap = 6.dp
private val PanelBodyHeight = 160.dp
private val EmojiPanelBodyHeight = 270.dp
private val StickerPanelBodyHeight = 270.dp
private val StickerItemHeight = 122.dp
private val PanelNavigationHeight = 49.dp
private val NumberGridHeight = KeyHeight * 3 + KeyVerticalGap * 2
private const val NumberSideColumnWeight = 0.85f
private const val NumberDigitGridWeight = 4.25f
private val AlternativeItemWidth = 48.dp
private val PopupHeight = 58.dp
private val PopupHorizontalPadding = 4.dp
private val PopupCornerRadius = 16.dp
private val PopupWindowMargin = 6.dp
private val KeyLabelFontSize = 28.sp
private val LongKeyLabelFontSize = 18.sp
private val SpacebarLabelFontSize = 14.sp
private val EmojiCommaLabelFontSize = 18.sp
private val EmojiHintIconSize = 14.dp
private val EmojiHintIconTopPadding = 4.dp
private val EmojiCommaLabelBottomPadding = 3.dp
