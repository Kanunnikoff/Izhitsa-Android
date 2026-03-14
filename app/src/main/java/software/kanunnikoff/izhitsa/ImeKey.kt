package software.kanunnikoff.izhitsa

import androidx.annotation.DrawableRes

data class ImeKey(
    val code: Int,
    val label: String? = null,
    val shiftLabel: String? = null,
    @param:DrawableRes val iconRes: Int = 0,
    val weight: Float = 1f,
    val repeatable: Boolean = false,
    val contentDescription: String? = null
) {
    companion object {
        fun text(code: Int, label: String, weight: Float): ImeKey {
            return ImeKey(code = code, label = label, weight = weight, contentDescription = label)
        }

        fun text(code: Int, label: String, shiftLabel: String, weight: Float): ImeKey {
            return ImeKey(code = code, label = label, shiftLabel = shiftLabel, weight = weight, contentDescription = label)
        }

        fun icon(
            code: Int,
            @DrawableRes iconRes: Int,
            weight: Float,
            repeatable: Boolean,
            contentDescription: String
        ): ImeKey {
            return ImeKey(
                code = code,
                iconRes = iconRes,
                weight = weight,
                repeatable = repeatable,
                contentDescription = contentDescription
            )
        }
    }
}
