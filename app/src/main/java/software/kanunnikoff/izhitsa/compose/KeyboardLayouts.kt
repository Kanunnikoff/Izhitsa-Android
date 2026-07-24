package software.kanunnikoff.izhitsa.compose

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Коды служебных клавиш.
 *
 * Отрицательные значения не пересекаются с кодовыми точками печатаемых символов.
 */
object KeyboardKeyCodes {
    /** Переключение регистра. */
    const val SHIFT = -1

    /** Возврат к буквенной раскладке. */
    const val MODE_ALPHA = -2

    /** Переход ко второй странице символов. */
    const val MORE_SYMBOLS = -4

    /** Удаление символа перед курсором. */
    const val DELETE = -5

    /** Переход к первой странице символов. */
    const val SYMBOLS = -6

    /** Переход к цифровой раскладке. */
    const val NUMBER_PAD = -7

    /** Смена русского и английского языков. */
    const val LANGUAGE = -101

    /** Перевод строки либо действие редактора. */
    const val ENTER = 10

    /** Обычный пробел. */
    const val SPACE = 32
}

/** Имена значков, которые представление сопоставляет ресурсам Compose или приложения. */
enum class KeyIcon {
    BACKSPACE,
    DONE,
    EMOJI,
    ENTER,
    LANGUAGE,
    SEARCH,
    SEND,
    SHIFT,
    NUMBER_PAD
}

/** Готовые раскладки и фабрики их клавиш. */
object KeyboardLayouts {
    private val ModifierKeyLabelFontSize = 20.sp
    private val SmallSymbolLabelFontSize = 22.sp
    private val NumberOperatorLabelFontSize = 22.sp
    private val NumberSpaceLabelFontSize = 28.sp
    private val PeriodAlternatives = listOf(
        "&", "%", "+", "\"", "-", ":", "'", "@",
        ";", "/", "(", ")", "#", "!", ",", "?"
    )

    /** Первая страница цифр и наиболее употребительных символов. */
    val Symbols = listOf(
        listOf(
            key("1"), key("2"), key("3"), key("4"), key("5"),
            key("6"), key("7"), key("8"), key("9"), key("0")
        ),
        listOf(
            key("@"),
            key("#"),
            key(
                label = "₽",
                alternatives = listOf("₹", "¥", "₽", "€", "$", "¢", "£"),
                alternativeRowLengths = listOf(3, 4),
                preferredAlternativeIndex = 4
            ),
            key("_"),
            key("&"),
            key(
                label = "-",
                alternatives = listOf("—", "_", "–", "•"),
                preferredAlternativeIndex = 1
            ),
            key(label = "+", alternatives = listOf("±")),
            key("("),
            key(")"),
            key("/")
        ),
        listOf(
            special(
                KeyboardKeyCodes.MORE_SYMBOLS,
                "=\\<",
                weight = SymbolModifierKeyWeight,
                fontSize = ModifierKeyLabelFontSize
            ),
            key("*"),
            key(
                label = "\"",
                alternatives = listOf("“", "„", "”", "«", "»"),
                preferredAlternativeIndex = 2
            ),
            key(
                label = "'",
                alternatives = listOf("‘", "‚", "’", "‹", "›"),
                preferredAlternativeIndex = 2
            ),
            key(":"),
            key(";"),
            key(label = "!", alternatives = listOf("¡")),
            key(label = "?", alternatives = listOf("¿", "‽")),
            iconKey(
                KeyboardKeyCodes.DELETE,
                KeyIcon.BACKSPACE,
                weight = SymbolModifierKeyWeight,
                repeatOnLongPress = true
            )
        ),
        listOf(
            special(
                KeyboardKeyCodes.MODE_ALPHA,
                "АБВ",
                weight = BottomPillKeyWeight,
                fontSize = ModifierKeyLabelFontSize
            ),
            special(
                code = ",".first().code,
                label = ","
            ),
            KeyInfo(
                code = KeyboardKeyCodes.NUMBER_PAD,
                icon = KeyIcon.NUMBER_PAD,
                weight = BottomSquareKeyWeight,
                isModifier = false
            ),
            KeyInfo(
                code = KeyboardKeyCodes.SPACE,
                label = "Русский",
                weight = SpacebarKeyWeight
            ),
            periodKey(),
            enterKey()
        )
    )

    /** Вторая страница специальных символов. */
    val Symbols2 = listOf(
        listOf(
            key("~"),
            key("`"),
            key("|"),
            key(
                label = "•",
                alternatives = listOf("♣", "♠", "♪", "♥", "♦"),
                preferredAlternativeIndex = 2
            ),
            key("√"),
            key(
                label = "π",
                alternatives = listOf("Ω", "Π", "μ"),
                preferredAlternativeIndex = 1
            ),
            key("÷"),
            key("×"),
            key(label = "§", alternatives = listOf("¶")),
            key("Δ")
        ),
        listOf(
            key("£"),
            key("€"),
            key(
                label = "$",
                alternatives = listOf("₹", "¥", "₽", "€", "¢", "£"),
                alternativeRowLengths = listOf(3, 3),
                preferredAlternativeIndex = 4
            ),
            key("¢"),
            key(
                label = "^",
                alternatives = listOf("←", "↑", "↓", "→"),
                preferredAlternativeIndex = 1
            ),
            key(
                label = "°",
                alternatives = listOf("′", "″")
            ),
            key(
                label = "=",
                alternatives = listOf("∞", "≠", "≈"),
                preferredAlternativeIndex = 1
            ),
            key("{"),
            key("}"),
            key("\\")
        ),
        listOf(
            special(
                KeyboardKeyCodes.SYMBOLS,
                "?123",
                weight = SymbolModifierKeyWeight
            ),
            key(
                label = "%",
                alternatives = listOf("‰", "‱")
            ),
            key(label = "©", fontSize = SmallSymbolLabelFontSize),
            key(label = "®", fontSize = SmallSymbolLabelFontSize),
            key("™"),
            key("✓"),
            key("["),
            key("]"),
            iconKey(
                KeyboardKeyCodes.DELETE,
                KeyIcon.BACKSPACE,
                weight = SymbolModifierKeyWeight,
                repeatOnLongPress = true
            )
        ),
        listOf(
            special(
                KeyboardKeyCodes.MODE_ALPHA,
                "АБВ",
                weight = BottomPillKeyWeight,
                fontSize = ModifierKeyLabelFontSize
            ),
            key(label = "<", fontSize = SmallSymbolLabelFontSize),
            KeyInfo(
                code = KeyboardKeyCodes.NUMBER_PAD,
                icon = KeyIcon.NUMBER_PAD,
                weight = BottomSquareKeyWeight,
                isModifier = false
            ),
            KeyInfo(
                code = KeyboardKeyCodes.SPACE,
                label = "Русский",
                weight = SpacebarKeyWeight
            ),
            key(label = ">", fontSize = SmallSymbolLabelFontSize),
            enterKey()
        )
    )

    /** Русская буквенная раскладка с дореформенными вариантами по удержанию. */
    val Russian = listOf(
        listOf(
            digitHintKey(label = "й", digit = "1"),
            digitHintKey(label = "ц", digit = "2"),
            digitHintKey(
                label = "у",
                digit = "3",
                alternatives = listOf(acute("у"))
            ),
            digitHintKey(label = "к", digit = "4"),
            key(
                label = "е",
                hint = "5",
                alternatives = listOf("е", "ѣ", "ё", "5", acute("е")),
                tapAlternatives = listOf("е", "ѣ", "ё"),
                preferredAlternativeIndex = 3
            ),
            digitHintKey(label = "н", digit = "6"),
            digitHintKey(label = "г", digit = "7"),
            digitHintKey(label = "ш", digit = "8"),
            digitHintKey(label = "щ", digit = "9"),
            digitHintKey(label = "з", digit = "0"),
            key("х")
        ),
        listOf(
            key(
                label = "ф",
                alternatives = listOf("ф", "ѳ")
            ),
            vowelKey("ы"),
            key("в"),
            vowelKey("а"),
            key("п"),
            key("р"),
            vowelKey("о"),
            key("л"),
            key("д"),
            key("ж"),
            vowelKey("э")
        ),
        listOf(
            iconKey(KeyboardKeyCodes.SHIFT, KeyIcon.SHIFT, weight = ModifierKeyWeight),
            vowelKey("я"),
            key("ч"),
            key("с"),
            key("м"),
            key(
                label = "и",
                alternatives = listOf("и", "і", "ѵ", acute("и")),
                tapAlternatives = listOf("и", "і", "ѵ")
            ),
            key("т"),
            key(
                label = "ь",
                alternatives = listOf("ь", "ъ")
            ),
            key("б"),
            vowelKey("ю"),
            iconKey(
                KeyboardKeyCodes.DELETE,
                KeyIcon.BACKSPACE,
                weight = ModifierKeyWeight,
                repeatOnLongPress = true
            )
        ),
        alphabetBottomRow(language = "Русскій")
    )

    /** Английская буквенная раскладка. */
    val English = listOf(
        listOf(
            digitHintKey(label = "q", digit = "1"),
            digitHintKey(label = "w", digit = "2"),
            digitHintKey(
                label = "e",
                digit = "3",
                alternatives = listOf("e", "é", "è", "ê", "ë")
            ),
            digitHintKey(label = "r", digit = "4"),
            digitHintKey(label = "t", digit = "5"),
            digitHintKey(label = "y", digit = "6"),
            digitHintKey(
                label = "u",
                digit = "7",
                alternatives = listOf("u", "ú", "ù", "û", "ü")
            ),
            digitHintKey(
                label = "i",
                digit = "8",
                alternatives = listOf("i", "í", "ì", "î", "ï")
            ),
            digitHintKey(
                label = "o",
                digit = "9",
                alternatives = listOf("o", "ó", "ò", "ô", "ö")
            ),
            digitHintKey(label = "p", digit = "0")
        ),
        listOf(
            key(
                label = "a",
                alternatives = listOf("a", "á", "à", "â", "ä")
            ),
            key("s"), key("d"), key("f"), key("g"), key("h"),
            key("j"), key("k"), key("l")
        ),
        listOf(
            iconKey(KeyboardKeyCodes.SHIFT, KeyIcon.SHIFT, weight = ModifierKeyWeight),
            key("z"), key("x"),
            key(
                label = "c",
                alternatives = listOf("c", "ç")
            ),
            key("v"), key("b"),
            key(
                label = "n",
                alternatives = listOf("n", "ñ")
            ),
            key("m"),
            iconKey(
                KeyboardKeyCodes.DELETE,
                KeyIcon.BACKSPACE,
                weight = ModifierKeyWeight,
                repeatOnLongPress = true
            )
        ),
        alphabetBottomRow(language = "English")
    )

    /** Цифровая раскладка для полей, запрашивающих набор чисел. */
    val Numbers = listOf(
        listOf(
            key("+", weight = 0.8f, fontSize = NumberOperatorLabelFontSize),
            key("1", weight = 1.25f),
            key("2", weight = 1.25f),
            key("3", weight = 1.25f),
            special(code = "%".first().code, label = "%", weight = 0.8f)
        ),
        listOf(
            key("-", weight = 0.8f, fontSize = NumberOperatorLabelFontSize),
            key("4", weight = 1.25f),
            key("5", weight = 1.25f),
            key("6", weight = 1.25f),
            special(
                code = KeyboardKeyCodes.SPACE,
                label = "␣",
                weight = 0.8f,
                fontSize = NumberSpaceLabelFontSize
            )
        ),
        listOf(
            key("*", weight = 0.8f, fontSize = NumberOperatorLabelFontSize),
            key("7", weight = 1.25f),
            key("8", weight = 1.25f),
            key("9", weight = 1.25f),
            iconKey(
                KeyboardKeyCodes.DELETE,
                KeyIcon.BACKSPACE,
                weight = 0.8f,
                repeatOnLongPress = true
            )
        ),
        listOf(
            key("/", weight = 0.8f, fontSize = NumberOperatorLabelFontSize),
            special(
                KeyboardKeyCodes.MODE_ALPHA,
                "АБВ",
                weight = 1.1f,
                fontSize = ModifierKeyLabelFontSize
            ),
            key(",", weight = 0.72f),
            special(
                KeyboardKeyCodes.SYMBOLS,
                "!?#",
                fontSize = ModifierKeyLabelFontSize
            ),
            key("0", weight = 1.8f),
            key("=", weight = 0.95f),
            key(".", weight = 0.72f),
            enterKey(weight = 1.1f)
        )
    )

    /**
     * Собирает общий нижний ряд буквенных раскладок.
     *
     * @param language название языка на пробеле.
     */
    private fun alphabetBottomRow(language: String): List<KeyInfo> {
        return listOf(
            special(KeyboardKeyCodes.SYMBOLS, "?123", weight = BottomPillKeyWeight),
            emojiCommaKey(weight = BottomSquareKeyWeight),
            KeyInfo(
                code = KeyboardKeyCodes.LANGUAGE,
                icon = KeyIcon.LANGUAGE,
                weight = BottomSquareKeyWeight,
                isModifier = false
            ),
            KeyInfo(
                code = KeyboardKeyCodes.SPACE,
                label = language,
                weight = SpacebarKeyWeight
            ),
            periodKey(),
            enterKey()
        )
    }

    /**
     * Создаёт запятую, которая долгим нажатием открывает панель эмодзи.
     *
     * @param weight доля ширины ряда.
     */
    private fun emojiCommaKey(weight: Float = 1f): KeyInfo {
        /*
         * Клавиша сохраняет код обычной запятой, поэтому короткое нажатие продолжает
         * вводить знак препинания. Отдельное действие долгого удержания открывает
         * панель эмодзи, а значок лишь подсказывает об этой возможности.
         */
        return KeyInfo(
            code = ",".first().code,
            label = ",",
            hintIcon = KeyIcon.EMOJI,
            longPressAction = KeyLongPressAction.SHOW_EMOJI,
            weight = weight,
            isModifier = true
        )
    }

    /** Создаёт точку с полным набором знаков быстрого выбора. */
    private fun periodKey(): KeyInfo {
        return special(
            code = ".".first().code,
            label = ".",
            weight = BottomSquareKeyWeight,
            alternatives = PeriodAlternatives,
            alternativeRowLengths = listOf(8, 8),
            preferredAlternativeIndex = 14
        )
    }

    /**
     * Создаёт обычную печатаемую клавишу.
     *
     * @param label основная надпись и источник кода символа.
     * @param hint дополнительная надпись в углу.
     * @param alternatives пункты меню долгого нажатия.
     * @param tapAlternatives варианты повторных коротких нажатий.
     * @param alternativeRowLengths распределение вариантов по строкам.
     * @param preferredAlternativeIndex вариант, совмещённый с клавишей.
     * @param weight доля ширины ряда.
     * @param fontSize явно заданный размер надписи.
     */
    private fun key(
        label: String,
        hint: String? = null,
        alternatives: List<String> = emptyList(),
        tapAlternatives: List<String> = alternatives,
        alternativeRowLengths: List<Int> = emptyList(),
        preferredAlternativeIndex: Int? = null,
        weight: Float = 1f,
        fontSize: TextUnit? = null
    ): KeyInfo {
        return KeyInfo(
            code = label.first().code,
            label = label,
            hint = hint,
            alternatives = alternatives,
            tapAlternatives = tapAlternatives,
            alternativeRowLengths = alternativeRowLengths,
            preferredAlternativeIndex = preferredAlternativeIndex,
            weight = weight,
            fontSize = fontSize
        )
    }

    /**
     * Создаёт служебную клавишу, не участвующую в смене регистра букв.
     *
     * @param code отрицательный служебный код либо код печатаемого знака.
     * @param label необязательная надпись.
     * @param icon необязательный значок вместо надписи.
     * @param weight доля ширины ряда.
     * @param fontSize явно заданный размер надписи.
     * @param alternatives пункты меню долгого нажатия.
     * @param alternativeRowLengths распределение вариантов по строкам.
     * @param preferredAlternativeIndex вариант, совмещённый с клавишей.
     */
    private fun special(
        code: Int,
        label: String? = null,
        icon: KeyIcon? = null,
        weight: Float = 1f,
        fontSize: TextUnit? = null,
        alternatives: List<String> = emptyList(),
        alternativeRowLengths: List<Int> = emptyList(),
        preferredAlternativeIndex: Int? = null
    ): KeyInfo {
        return KeyInfo(
            code = code,
            label = label,
            icon = icon,
            alternatives = alternatives,
            alternativeRowLengths = alternativeRowLengths,
            preferredAlternativeIndex = preferredAlternativeIndex,
            weight = weight,
            fontSize = fontSize,
            isModifier = true
        )
    }

    /**
     * Создаёт служебную клавишу, содержимое которой представлено только значком.
     *
     * @param code служебный код.
     * @param icon отображаемый значок.
     * @param weight доля ширины ряда.
     * @param repeatOnLongPress повторять ли действие при удержании.
     */
    private fun iconKey(
        code: Int,
        icon: KeyIcon,
        weight: Float,
        repeatOnLongPress: Boolean = false
    ): KeyInfo {
        return special(
            code = code,
            icon = icon,
            weight = weight
        ).copy(repeatOnLongPress = repeatOnLongPress)
    }

    /**
     * Создаёт Enter; его действие и значок уточняются по текущему
     * [android.view.inputmethod.EditorInfo].
     *
     * @param weight доля ширины ряда.
     */
    private fun enterKey(weight: Float = BottomPillKeyWeight): KeyInfo {
        return KeyInfo(
            code = KeyboardKeyCodes.ENTER,
            icon = KeyIcon.ENTER,
            weight = weight,
            isModifier = true
        )
    }

    /**
     * Создаёт букву с цифрой-подсказкой, доступной только через долгое нажатие.
     *
     * @param label основная буква.
     * @param digit цифра в углу и меню удержания.
     * @param alternatives остальные варианты удержания.
     */
    private fun digitHintKey(
        label: String,
        digit: String,
        alternatives: List<String> = emptyList()
    ): KeyInfo {
        val menuAlternatives = alternatives + digit

        return key(
            label = label,
            hint = digit,
            alternatives = menuAlternatives,
            tapAlternatives = emptyList(),
            preferredAlternativeIndex = menuAlternatives.lastIndex
        )
    }

    /**
     * Создаёт гласную с вариантом, содержащим комбинируемое ударение.
     *
     * @param label основная буква.
     */
    private fun vowelKey(label: String): KeyInfo {
        return key(
            label = label,
            alternatives = listOf(acute(label)),
            tapAlternatives = emptyList()
        )
    }

    /**
     * Добавляет к букве комбинируемый знак острого ударения.
     *
     * @param letter исходная буква.
     */
    private fun acute(letter: String): String {
        return letter + CombiningAcuteAccent
    }

    /*
     * Весовые коэффициенты восстановлены по снимку Gboard на экране 1080 пикселей
     * при плотности 420 dpi. Они сохраняют одинаковые пропорции и на других
     * размерах, поскольку Compose распределяет доступную ширину относительно.
     */
    private const val ModifierKeyWeight = 1.35f
    private const val SymbolModifierKeyWeight = 1.58f
    private const val BottomPillKeyWeight = 1.58f
    private const val BottomSquareKeyWeight = 1f
    private const val SpacebarKeyWeight = 4.45f
    private const val CombiningAcuteAccent = "\u0301"
}
