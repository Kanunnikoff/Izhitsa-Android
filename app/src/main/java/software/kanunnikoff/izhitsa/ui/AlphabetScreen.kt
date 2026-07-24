package software.kanunnikoff.izhitsa.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Показывает дореформенный русский алфавит и ссылку на источник.
 *
 * @param usesPreRevolutionaryOrthography используется ли дореформенное написание.
 */
@Composable
internal fun AlphabetScreen(
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
                        "Дореформенный русскій алфавитъ"
                    } else {
                        "Дореформенный русский алфавит"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (usesPreRevolutionaryOrthography) {
                        "Прописныя и строчныя формы 35 буквъ, входившихъ въ русскій алфавитъ до реформы 1918 года."
                    } else {
                        "Прописные и строчные формы 35 букв, входивших в русский алфавит до реформы 1918 года."
                    },
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
                text = if (usesPreRevolutionaryOrthography) {
                    "Четыре упразднённыя буквы — І, Ѣ, Ѳ и Ѵ. Буквы Ё и Й формально не входили въ алфавитъ, но употреблялись въ письмѣ."
                } else {
                    "Четыре упразднённые буквы — І, Ѣ, Ѳ и Ѵ. Буквы Ё и Й формально не входили в алфавит, но употреблялись в письме."
                },
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
                    text = if (usesPreRevolutionaryOrthography) {
                        "Источникъ: «Русская дореформенная орѳографія»"
                    } else {
                        "Источник: «Русская дореформенная орфография»"
                    },
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}



/** Прописные и строчные формы дореформенного алфавита в историческом порядке. */
private val HistoricalAlphabet = listOf(
    "А" to "а", "Б" to "б", "В" to "в", "Г" to "г", "Д" to "д",
    "Е" to "е", "Ж" to "ж", "З" to "з", "И" to "и", "І" to "і",
    "К" to "к", "Л" to "л", "М" to "м", "Н" to "н", "О" to "о",
    "П" to "п", "Р" to "р", "С" to "с", "Т" to "т", "У" to "у",
    "Ф" to "ф", "Х" to "х", "Ц" to "ц", "Ч" to "ч", "Ш" to "ш",
    "Щ" to "щ", "Ъ" to "ъ", "Ы" to "ы", "Ь" to "ь", "Ѣ" to "ѣ",
    "Э" to "э", "Ю" to "ю", "Я" to "я", "Ѳ" to "ѳ", "Ѵ" to "ѵ"
)

/** Расстояние между карточками букв. */
private val GridSpacing = 12.dp

/** Адрес статьи, поясняющей состав дореформенного алфавита. */
private const val AlphabetSourceUrl =
    "https://ru.wikipedia.org/wiki/%D0%A0%D1%83%D1%81%D1%81%D0%BA%D0%B0%D1%8F_%D0%B4%D0%BE%D1%80%D0%B5%D1%84%D0%BE%D1%80%D0%BC%D0%B5%D0%BD%D0%BD%D0%B0%D1%8F_%D0%BE%D1%80%D1%84%D0%BE%D0%B3%D1%80%D0%B0%D1%84%D0%B8%D1%8F"
