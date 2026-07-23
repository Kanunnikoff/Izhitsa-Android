package software.kanunnikoff.izhitsa.compose

object KeyboardKeyCodes {
    const val SHIFT = -1
    const val MODE_ALPHA = -2
    const val MORE_SYMBOLS = -4
    const val DELETE = -5
    const val SYMBOLS = -6
    const val NUMBER_PAD = -7
    const val LANGUAGE = -101
    const val ENTER = 10
    const val SPACE = 32
}

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

object KeyboardLayouts {
    val Symbols = listOf(
        listOf(
            key("1"), key("2"), key("3"), key("4"), key("5"),
            key("6"), key("7"), key("8"), key("9"), key("0")
        ),
        listOf(
            key("@"), key("#"), key("₽"), key("_"), key("&"),
            key("-"), key("+"), key("("), key(")"), key("/")
        ),
        listOf(
            special(
                KeyboardKeyCodes.MORE_SYMBOLS,
                "=\\<",
                weight = SymbolModifierKeyWeight
            ),
            key("*"), key("\""), key("'"), key(":"), key(";"), key("!"), key("?"),
            iconKey(
                KeyboardKeyCodes.DELETE,
                KeyIcon.BACKSPACE,
                weight = SymbolModifierKeyWeight
            )
        ),
        listOf(
            special(KeyboardKeyCodes.MODE_ALPHA, "АБВ", weight = BottomPillKeyWeight),
            emojiCommaKey(),
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
            key("."),
            enterKey()
        )
    )

    val Symbols2 = listOf(
        listOf(
            key("~"), key("`"), key("|"), key("•"), key("√"),
            key("π"), key("÷"), key("×"), key("§"), key("Δ")
        ),
        listOf(
            key("£"), key("€"), key("$"), key("¢"), key("^"),
            key("°"), key("="), key("{"), key("}"), key("\\")
        ),
        listOf(
            special(
                KeyboardKeyCodes.SYMBOLS,
                "?123",
                weight = SymbolModifierKeyWeight
            ),
            key("%"), key("©"), key("®"), key("™"), key("✓"), key("["), key("]"),
            iconKey(
                KeyboardKeyCodes.DELETE,
                KeyIcon.BACKSPACE,
                weight = SymbolModifierKeyWeight
            )
        ),
        listOf(
            special(KeyboardKeyCodes.MODE_ALPHA, "АБВ", weight = BottomPillKeyWeight),
            key("<"),
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
            key(">"),
            enterKey()
        )
    )

    val Russian = listOf(
        listOf(
            key("й", hint = "1"),
            key("ц", hint = "2"),
            key("у", hint = "3"),
            key("к", hint = "4"),
            key(
                label = "е",
                hint = "5",
                alternatives = listOf("е", "ѣ", "ё")
            ),
            key("н", hint = "6"),
            key("г", hint = "7"),
            key("ш", hint = "8"),
            key("щ", hint = "9"),
            key("з", hint = "0"),
            key("х")
        ),
        listOf(
            key(
                label = "ф",
                alternatives = listOf("ф", "ѳ")
            ),
            key("ы"), key("в"), key("а"), key("п"), key("р"),
            key("о"), key("л"), key("д"), key("ж"), key("э")
        ),
        listOf(
            iconKey(KeyboardKeyCodes.SHIFT, KeyIcon.SHIFT, weight = ModifierKeyWeight),
            key("я"), key("ч"), key("с"), key("м"),
            key(
                label = "и",
                alternatives = listOf("и", "i", "ѵ")
            ),
            key("т"),
            key(
                label = "ь",
                alternatives = listOf("ь", "ъ")
            ),
            key("б"), key("ю"),
            iconKey(KeyboardKeyCodes.DELETE, KeyIcon.BACKSPACE, weight = ModifierKeyWeight)
        ),
        alphabetBottomRow(language = "Русский")
    )

    val English = listOf(
        listOf(
            key("q", hint = "1"),
            key("w", hint = "2"),
            key(
                label = "e",
                hint = "3",
                alternatives = listOf("e", "é", "è", "ê", "ë")
            ),
            key("r", hint = "4"),
            key("t", hint = "5"),
            key("y", hint = "6"),
            key(
                label = "u",
                hint = "7",
                alternatives = listOf("u", "ú", "ù", "û", "ü")
            ),
            key(
                label = "i",
                hint = "8",
                alternatives = listOf("i", "í", "ì", "î", "ï")
            ),
            key(
                label = "o",
                hint = "9",
                alternatives = listOf("o", "ó", "ò", "ô", "ö")
            ),
            key("p", hint = "0")
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
            iconKey(KeyboardKeyCodes.DELETE, KeyIcon.BACKSPACE, weight = ModifierKeyWeight)
        ),
        alphabetBottomRow(language = "English")
    )

    val Numbers = listOf(
        listOf(
            key("+", weight = 0.8f),
            key("1", weight = 1.25f),
            key("2", weight = 1.25f),
            key("3", weight = 1.25f),
            special(code = "%".first().code, label = "%", weight = 0.8f)
        ),
        listOf(
            key("-", weight = 0.8f),
            key("4", weight = 1.25f),
            key("5", weight = 1.25f),
            key("6", weight = 1.25f),
            special(code = KeyboardKeyCodes.SPACE, label = "␣", weight = 0.8f)
        ),
        listOf(
            key("*", weight = 0.8f),
            key("7", weight = 1.25f),
            key("8", weight = 1.25f),
            key("9", weight = 1.25f),
            iconKey(KeyboardKeyCodes.DELETE, KeyIcon.BACKSPACE, weight = 0.8f)
        ),
        listOf(
            key("/", weight = 0.8f),
            special(KeyboardKeyCodes.MODE_ALPHA, "АБВ", weight = 1.1f),
            key(",", weight = 0.72f),
            special(KeyboardKeyCodes.SYMBOLS, "!?#"),
            key("0", weight = 1.8f),
            key("=", weight = 0.95f),
            key(".", weight = 0.72f),
            enterKey(weight = 1.1f)
        )
    )

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
            special(
                code = ".".first().code,
                label = ".",
                weight = BottomSquareKeyWeight
            ),
            enterKey()
        )
    }

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

    private fun key(
        label: String,
        hint: String? = null,
        alternatives: List<String> = emptyList(),
        weight: Float = 1f
    ): KeyInfo {
        return KeyInfo(
            code = label.first().code,
            label = label,
            hint = hint,
            alternatives = alternatives,
            weight = weight
        )
    }

    private fun special(
        code: Int,
        label: String? = null,
        icon: KeyIcon? = null,
        weight: Float = 1f
    ): KeyInfo {
        return KeyInfo(
            code = code,
            label = label,
            icon = icon,
            weight = weight,
            isModifier = true
        )
    }

    private fun iconKey(
        code: Int,
        icon: KeyIcon,
        weight: Float
    ): KeyInfo {
        return special(
            code = code,
            icon = icon,
            weight = weight
        )
    }

    private fun enterKey(weight: Float = BottomPillKeyWeight): KeyInfo {
        return KeyInfo(
            code = KeyboardKeyCodes.ENTER,
            icon = KeyIcon.ENTER,
            weight = weight,
            isModifier = true
        )
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
}
