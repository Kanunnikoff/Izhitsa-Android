package software.kanunnikoff.izhitsa

object ImeKeyboardLayouts {
    fun russian(): ImeKeyboardLayout {
        val rows = mutableListOf<List<ImeKey>>()

        rows.add(
            listOf(
                ImeKey.text('й'.code, "й", 1f),
                ImeKey.text('ц'.code, "ц", 1f),
                ImeKey.text('у'.code, "у", 1f),
                ImeKey.text('к'.code, "к", 1f),
                ImeKey.text('е'.code, "е", 1f),
                ImeKey.text('н'.code, "н", 1f),
                ImeKey.text('г'.code, "г", 1f),
                ImeKey.text('ш'.code, "ш", 1f),
                ImeKey.text('щ'.code, "щ", 1f),
                ImeKey.text('з'.code, "з", 1f),
                ImeKey.text('х'.code, "х", 1f)
            )
        )

        rows.add(
            listOf(
                ImeKey.text('ф'.code, "ф", 1f),
                ImeKey.text('ы'.code, "ы", 1f),
                ImeKey.text('в'.code, "в", 1f),
                ImeKey.text('а'.code, "а", 1f),
                ImeKey.text('п'.code, "п", 1f),
                ImeKey.text('р'.code, "р", 1f),
                ImeKey.text('о'.code, "о", 1f),
                ImeKey.text('л'.code, "л", 1f),
                ImeKey.text('д'.code, "д", 1f),
                ImeKey.text('ж'.code, "ж", 1f),
                ImeKey.text('э'.code, "э", 1f)
            )
        )

        rows.add(
            listOf(
                ImeKey.icon(SoftKeyboard.KEYCODE_SHIFT, R.drawable.ic_baseline_forward_24px, 1.2f, false, "Shift"),
                ImeKey.text('я'.code, "я", 1f),
                ImeKey.text('ч'.code, "ч", 1f),
                ImeKey.text('с'.code, "с", 1f),
                ImeKey.text('м'.code, "м", 1f),
                ImeKey.text('и'.code, "и", 1f),
                ImeKey.text('т'.code, "т", 1f),
                ImeKey.text('ь'.code, "ь", 1f),
                ImeKey.text('б'.code, "б", 1f),
                ImeKey.text('ю'.code, "ю", 1f),
                ImeKey.icon(SoftKeyboard.KEYCODE_DELETE, R.drawable.ic_outline_backspace_24px, 1.2f, true, "Backspace")
            )
        )

        rows.add(
            listOf(
                ImeKey.text(SoftKeyboard.KEYCODE_MODE_CHANGE, "?123", 1.5f),
                ImeKey.text(','.code, ",", 1f),
                ImeKey.icon(SoftKeyboard.KEYCODE_LANGUAGE_SWITCH, R.drawable.ic_baseline_language_24px, 1f, false, "Language"),
                ImeKey.text(' '.code, "Русскiй", 4f),
                ImeKey.text('.'.code, ".", 1f),
                ImeKey.icon('\n'.code, R.drawable.ic_outline_keyboard_return_24px, 1.5f, false, "Enter")
            )
        )

        return ImeKeyboardLayout(rows)
    }

    fun english(): ImeKeyboardLayout {
        val rows = mutableListOf<List<ImeKey>>()

        rows.add(
            listOf(
                ImeKey.text('q'.code, "q", 1f),
                ImeKey.text('w'.code, "w", 1f),
                ImeKey.text('e'.code, "e", 1f),
                ImeKey.text('r'.code, "r", 1f),
                ImeKey.text('t'.code, "t", 1f),
                ImeKey.text('y'.code, "y", 1f),
                ImeKey.text('u'.code, "u", 1f),
                ImeKey.text('i'.code, "i", 1f),
                ImeKey.text('o'.code, "o", 1f),
                ImeKey.text('p'.code, "p", 1f)
            )
        )

        rows.add(
            listOf(
                ImeKey.text('a'.code, "a", 1f),
                ImeKey.text('s'.code, "s", 1f),
                ImeKey.text('d'.code, "d", 1f),
                ImeKey.text('f'.code, "f", 1f),
                ImeKey.text('g'.code, "g", 1f),
                ImeKey.text('h'.code, "h", 1f),
                ImeKey.text('j'.code, "j", 1f),
                ImeKey.text('k'.code, "k", 1f),
                ImeKey.text('l'.code, "l", 1f)
            )
        )

        rows.add(
            listOf(
                ImeKey.icon(SoftKeyboard.KEYCODE_SHIFT, R.drawable.ic_baseline_forward_24px, 1.5f, false, "Shift"),
                ImeKey.text('z'.code, "z", 1f),
                ImeKey.text('x'.code, "x", 1f),
                ImeKey.text('c'.code, "c", 1f),
                ImeKey.text('v'.code, "v", 1f),
                ImeKey.text('b'.code, "b", 1f),
                ImeKey.text('n'.code, "n", 1f),
                ImeKey.text('m'.code, "m", 1f),
                ImeKey.icon(SoftKeyboard.KEYCODE_DELETE, R.drawable.ic_outline_backspace_24px, 1.5f, true, "Backspace")
            )
        )

        rows.add(
            listOf(
                ImeKey.text(SoftKeyboard.KEYCODE_MODE_CHANGE, "?123", 1.5f),
                ImeKey.text(','.code, ",", 1f),
                ImeKey.icon(SoftKeyboard.KEYCODE_LANGUAGE_SWITCH, R.drawable.ic_baseline_language_24px, 1f, false, "Language"),
                ImeKey.text(' '.code, "English", 4f),
                ImeKey.text('.'.code, ".", 1f),
                ImeKey.icon('\n'.code, R.drawable.ic_outline_keyboard_return_24px, 1.5f, false, "Enter")
            )
        )

        return ImeKeyboardLayout(rows)
    }

    fun symbols(): ImeKeyboardLayout {
        val rows = mutableListOf<List<ImeKey>>()

        rows.add(
            listOf(
                ImeKey.text('1'.code, "1", 1f),
                ImeKey.text('2'.code, "2", 1f),
                ImeKey.text('3'.code, "3", 1f),
                ImeKey.text('4'.code, "4", 1f),
                ImeKey.text('5'.code, "5", 1f),
                ImeKey.text('6'.code, "6", 1f),
                ImeKey.text('7'.code, "7", 1f),
                ImeKey.text('8'.code, "8", 1f),
                ImeKey.text('9'.code, "9", 1f),
                ImeKey.text('0'.code, "0", 1f)
            )
        )

        rows.add(
            listOf(
                ImeKey.text('@'.code, "@", 1f),
                ImeKey.text('#'.code, "#", 1f),
                ImeKey.text('$'.code, "$", 1f),
                ImeKey.text('_'.code, "_", 1f),
                ImeKey.text('&'.code, "&", 1f),
                ImeKey.text('-'.code, "-", 1f),
                ImeKey.text('+'.code, "+", 1f),
                ImeKey.text('('.code, "(", 1f),
                ImeKey.text(')'.code, ")", 1f),
                ImeKey.text('/'.code, "/", 1f)
            )
        )

        rows.add(
            listOf(
                ImeKey.text(SoftKeyboard.KEYCODE_MODE_2_CHANGE, "=<", 1.5f),
                ImeKey.text('*'.code, "*", 1f),
                ImeKey.text('"'.code, "\"", 1f),
                ImeKey.text('\''.code, "'", 1f),
                ImeKey.text(':'.code, ":", 1f),
                ImeKey.text(';'.code, ";", 1f),
                ImeKey.text('!'.code, "!", 1f),
                ImeKey.text('?'.code, "?", 1f),
                ImeKey.icon(SoftKeyboard.KEYCODE_DELETE, R.drawable.ic_outline_backspace_24px, 1.5f, true, "Backspace")
            )
        )

        rows.add(
            listOf(
                ImeKey.text(SoftKeyboard.KEYCODE_MODE_4_CHANGE, "ABC", 1.5f),
                ImeKey.text(','.code, ",", 1f),
                ImeKey.text(SoftKeyboard.KEYCODE_MODE_3_CHANGE, "123", 1f),
                ImeKey.icon(' '.code, R.drawable.sym_keyboard_space, 4f, false, "Space"),
                ImeKey.text('.'.code, ".", 1f),
                ImeKey.icon('\n'.code, R.drawable.ic_outline_keyboard_return_24px, 1.5f, false, "Enter")
            )
        )

        return ImeKeyboardLayout(rows)
    }

    fun symbolsShift(): ImeKeyboardLayout {
        val rows = mutableListOf<List<ImeKey>>()

        rows.add(
            listOf(
                ImeKey.text('~'.code, "~", 1f),
                ImeKey.text('`'.code, "`", 1f),
                ImeKey.text('|'.code, "|", 1f),
                ImeKey.text('•'.code, "•", 1f),
                ImeKey.text('√'.code, "√", 1f),
                ImeKey.text('π'.code, "π", 1f),
                ImeKey.text('÷'.code, "÷", 1f),
                ImeKey.text('×'.code, "×", 1f),
                ImeKey.text('¶'.code, "¶", 1f),
                ImeKey.text('Δ'.code, "Δ", 1f)
            )
        )

        rows.add(
            listOf(
                ImeKey.text('£'.code, "£", 1f),
                ImeKey.text('¢'.code, "¢", 1f),
                ImeKey.text('€'.code, "€", 1f),
                ImeKey.text('¥'.code, "¥", 1f),
                ImeKey.text('^'.code, "^", 1f),
                ImeKey.text('°'.code, "°", 1f),
                ImeKey.text('='.code, "=", 1f),
                ImeKey.text('{'.code, "{", 1f),
                ImeKey.text('}'.code, "}", 1f),
                ImeKey.text('\\'.code, "\\", 1f)
            )
        )

        rows.add(
            listOf(
                ImeKey.text(SoftKeyboard.KEYCODE_MODE_2_CHANGE, "?123", 1.5f),
                ImeKey.text('%'.code, "%", 1f),
                ImeKey.text('©'.code, "©", 1f),
                ImeKey.text('®'.code, "®", 1f),
                ImeKey.text('™'.code, "™", 1f),
                ImeKey.text('✓'.code, "✓", 1f),
                ImeKey.text('['.code, "[", 1f),
                ImeKey.text(']'.code, "]", 1f),
                ImeKey.icon(SoftKeyboard.KEYCODE_DELETE, R.drawable.ic_outline_backspace_24px, 1.5f, true, "Backspace")
            )
        )

        rows.add(
            listOf(
                ImeKey.text(SoftKeyboard.KEYCODE_MODE_4_CHANGE, "ABC", 1.5f),
                ImeKey.text('<'.code, "<", 1f),
                ImeKey.text(SoftKeyboard.KEYCODE_MODE_3_CHANGE, "123", 1f),
                ImeKey.icon(' '.code, R.drawable.sym_keyboard_space, 4f, false, "Space"),
                ImeKey.text('>'.code, ">", 1f),
                ImeKey.icon('\n'.code, R.drawable.ic_outline_keyboard_return_24px, 1.5f, false, "Enter")
            )
        )

        return ImeKeyboardLayout(rows)
    }

    fun digits(): ImeKeyboardLayout {
        val rows = mutableListOf<List<ImeKey>>()

        rows.add(
            listOf(
                ImeKey.text('+'.code, "+", 1f),
                ImeKey.text('1'.code, "1", 1.6f),
                ImeKey.text('2'.code, "2", 1.6f),
                ImeKey.text('3'.code, "3", 1.6f),
                ImeKey.text('*'.code, "*", 1f)
            )
        )

        rows.add(
            listOf(
                ImeKey.text('-'.code, "-", 1f),
                ImeKey.text('4'.code, "4", 1.6f),
                ImeKey.text('5'.code, "5", 1.6f),
                ImeKey.text('6'.code, "6", 1.6f),
                ImeKey.text('/'.code, "/", 1f)
            )
        )

        rows.add(
            listOf(
                ImeKey.icon(' '.code, R.drawable.sym_keyboard_space, 1f, false, "Space"),
                ImeKey.text('7'.code, "7", 1.6f),
                ImeKey.text('8'.code, "8", 1.6f),
                ImeKey.text('9'.code, "9", 1.6f),
                ImeKey.icon(SoftKeyboard.KEYCODE_DELETE, R.drawable.ic_outline_backspace_24px, 1f, true, "Backspace")
            )
        )

        rows.add(
            listOf(
                ImeKey.text(SoftKeyboard.KEYCODE_MODE_4_CHANGE, "ABC", 1f),
                ImeKey.text(','.code, ",", 1f),
                ImeKey.text(SoftKeyboard.KEYCODE_MODE_3_CHANGE, "!?#", 1f),
                ImeKey.text('0'.code, "0", 1.6f),
                ImeKey.text('='.code, "=", 1f),
                ImeKey.text('.'.code, ".", 1f),
                ImeKey.icon('\n'.code, R.drawable.ic_outline_keyboard_return_24px, 1f, false, "Enter")
            )
        )

        return ImeKeyboardLayout(rows)
    }
}
