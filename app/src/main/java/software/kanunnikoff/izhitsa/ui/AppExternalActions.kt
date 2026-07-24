package software.kanunnikoff.izhitsa.ui

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Открывает страницу приложения в Google Play и переходит к веб-странице при отсутствии магазина.
 *
 * @receiver контекст, из которого открывается страница.
 * @param packageName имя пакета требуемого приложения.
 */
internal fun Context.openStorePage(packageName: String) {
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

/**
 * Открывает внешнюю ссылку из контекста приложения.
 *
 * @receiver контекст, из которого открывается ссылка.
 * @param url адрес внешней страницы.
 */
internal fun Context.openUrl(url: String) {
    startActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

/**
 * Передаёт текст в системное меню отправки.
 *
 * @receiver контекст, из которого открывается системное меню.
 * @param text отправляемый текст.
 */
internal fun Context.shareText(text: String) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = PlainTextMimeType
        putExtra(Intent.EXTRA_TEXT, text)
    }

    startActivity(
        Intent.createChooser(shareIntent, ShareAppTitle)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

/** Имена пакетов связанных приложений автора. */
internal const val YatPackageName = "software.kanunnikoff.yat"
internal const val RussianMeasuresPackageName =
    "software.kanunnikoff.russianmeasures"

/** Адреса Google Play для приложений и страницы автора. */
private const val GooglePlayMarketUrl = "market://details?id="
internal const val GooglePlayWebUrl =
    "https://play.google.com/store/apps/details?id="
internal const val GooglePlayDeveloperUrl =
    "https://play.google.com/store/apps/dev?id=9118553902079488918"

/** Адреса обратной связи и политики конфиденциальности. */
internal const val FeedbackEmailUri =
    "mailto:dmitry.kanunnikoff@gmail.com?subject=%D0%98%D0%B6%D0%B8%D1%86%D0%B0%20%28Android%29"
internal const val PrivacyPolicyUrl =
    "https://docs.google.com/document/d/189iftSQQuRh8VGhFnCUDY5ujwgU5gsnnPIUjOGL5ypE/edit?usp=sharing"

/** Параметры системного меню отправки текста. */
private const val PlainTextMimeType = "text/plain"
private const val ShareAppTitle = "Поделиться приложением"
