package software.kanunnikoff.izhitsa.compose

import software.kanunnikoff.izhitsa.R

object KeyboardLayouts {
    val Russian = listOf(
        listOf(
            KeyInfo(1081, "й"), KeyInfo(1094, "ц"), KeyInfo(1091, "у"), KeyInfo(1082, "к"),
            KeyInfo(1077, "е"), KeyInfo(1085, "н"), KeyInfo(1075, "г"), KeyInfo(1096, "ш"),
            KeyInfo(1097, "щ"), KeyInfo(1079, "з"), KeyInfo(1093, "х")
        ),
        listOf(
            KeyInfo(1092, "ф"), KeyInfo(1099, "ы"), KeyInfo(1074, "в"), KeyInfo(1072, "а"),
            KeyInfo(1087, "п"), KeyInfo(1088, "р"), KeyInfo(1086, "о"), KeyInfo(1083, "л"),
            KeyInfo(1076, "д"), KeyInfo(1078, "ж"), KeyInfo(1101, "э")
        ),
        listOf(
            KeyInfo(-1, iconResId = R.drawable.ic_baseline_forward_24px, weight = 1.5f, isModifier = true),
            KeyInfo(1103, "я"), KeyInfo(1095, "ч"), KeyInfo(1089, "с"), KeyInfo(1084, "м"),
            KeyInfo(1080, "и"), KeyInfo(1090, "т"), KeyInfo(1100, "ь"), KeyInfo(1073, "б"),
            KeyInfo(1102, "ю"),
            KeyInfo(-5, iconResId = R.drawable.ic_outline_backspace_24px, weight = 1.5f, isModifier = true)
        ),
        listOf(
            KeyInfo(-2, "?123", weight = 1.5f, isModifier = true),
            KeyInfo(44, ","),
            KeyInfo(-101, iconResId = R.drawable.ic_baseline_language_24px),
            KeyInfo(32, "Русскiй", weight = 4f),
            KeyInfo(46, "."),
            KeyInfo(10, iconResId = R.drawable.ic_outline_keyboard_return_24px, weight = 1.5f, isModifier = true)
        )
    )

    val English = listOf(
        listOf(
            KeyInfo(113, "q"), KeyInfo(119, "w"), KeyInfo(101, "e"), KeyInfo(114, "r"),
            KeyInfo(116, "t"), KeyInfo(121, "y"), KeyInfo(117, "u"), KeyInfo(105, "i"),
            KeyInfo(111, "o"), KeyInfo(112, "p")
        ),
        listOf(
            KeyInfo(97, "a"), KeyInfo(115, "s"), KeyInfo(100, "d"), KeyInfo(102, "f"),
            KeyInfo(103, "g"), KeyInfo(104, "h"), KeyInfo(106, "j"), KeyInfo(107, "k"),
            KeyInfo(108, "l")
        ),
        listOf(
            KeyInfo(-1, iconResId = R.drawable.ic_baseline_forward_24px, weight = 1.5f, isModifier = true),
            KeyInfo(122, "z"), KeyInfo(120, "x"), KeyInfo(99, "c"), KeyInfo(118, "v"),
            KeyInfo(98, "b"), KeyInfo(110, "n"), KeyInfo(109, "m"),
            KeyInfo(-5, iconResId = R.drawable.ic_outline_backspace_24px, weight = 1.5f, isModifier = true)
        ),
        listOf(
            KeyInfo(-2, "?123", weight = 1.5f, isModifier = true),
            KeyInfo(44, ","),
            KeyInfo(-101, iconResId = R.drawable.ic_baseline_language_24px),
            KeyInfo(32, "English", weight = 4f),
            KeyInfo(46, "."),
            KeyInfo(10, iconResId = R.drawable.ic_outline_keyboard_return_24px, weight = 1.5f, isModifier = true)
        )
    )
}
