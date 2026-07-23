package software.kanunnikoff.izhitsa.stickers

import androidx.annotation.DrawableRes
import software.kanunnikoff.izhitsa.R

data class Sticker(
    val identifier: Int,
    val description: String,
    @param:DrawableRes val drawableResource: Int
) {
    val fileName: String
        get() = "sticker_${identifier.toString().padStart(length = 2, padChar = '0')}.png"
}

object StickerCatalog {
    val items = listOf(
        Sticker(
            identifier = 1,
            description = "Привѣтствую!",
            drawableResource = R.drawable.sticker_01
        ),
        Sticker(
            identifier = 2,
            description = "Какъ поживаете?",
            drawableResource = R.drawable.sticker_02
        ),
        Sticker(
            identifier = 3,
            description = "Премного благодарю!",
            drawableResource = R.drawable.sticker_03
        ),
        Sticker(
            identifier = 4,
            description = "Съ удовольствиемъ!",
            drawableResource = R.drawable.sticker_04
        ),
        Sticker(
            identifier = 5,
            description = "Разумѣется!",
            drawableResource = R.drawable.sticker_05
        ),
        Sticker(
            identifier = 6,
            description = "Никакъ нѣтъ!",
            drawableResource = R.drawable.sticker_06
        ),
        Sticker(
            identifier = 7,
            description = "Прошу прощенія!",
            drawableResource = R.drawable.sticker_07
        ),
        Sticker(
            identifier = 8,
            description = "Не извольте безпокоиться!",
            drawableResource = R.drawable.sticker_08
        ),
        Sticker(
            identifier = 9,
            description = "Уже въ пути!",
            drawableResource = R.drawable.sticker_09
        ),
        Sticker(
            identifier = 10,
            description = "До скорой встрѣчи!",
            drawableResource = R.drawable.sticker_10
        )
    )
}
