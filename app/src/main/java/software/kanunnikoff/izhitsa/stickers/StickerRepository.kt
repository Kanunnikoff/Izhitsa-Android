package software.kanunnikoff.izhitsa.stickers

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Материализует встроенные стикеры в закрытом кэше и выдаёт безопасные URI.
 *
 * @property context контекст, через который доступны ресурсы и [FileProvider].
 */
class StickerRepository(private val context: Context) {
    /**
     * Возвращает URI стикера, доступный внешнему приложению через [FileProvider].
     *
     * Файл создаётся только при первом обращении или после очистки кэша.
     *
     * @param sticker элемент, который нужно поместить в кэш.
     */
    @SuppressLint("ResourceType")
    fun contentUri(sticker: Sticker): Uri {
        val directory = File(context.cacheDir, StickerCacheDirectory)

        if (!directory.exists()) {
            check(directory.mkdirs()) {
                "Не удалось создать временную папку стикеров."
            }
        }

        val destination = File(directory, sticker.fileName)

        /*
         * Идентификатор ресурса берётся только из фиксированного каталога.
         * Пользовательский путь здесь не участвует, поэтому нельзя выйти за
         * пределы выделенной временной папки.
         */
        if (!destination.exists() || destination.length() == 0L) {
            /*
             * PNG одновременно нужен интерфейсу как drawable и здесь как
             * исходный поток байтов. Resources умеет открывать любой упакованный
             * ресурс, поэтому отдельная копия в raw только увеличила бы APK.
             */
            context.resources.openRawResource(sticker.drawableResource).use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.$ProviderAuthoritySuffix",
            destination
        )
    }

    companion object {
        /** Тип содержимого, объявляемый при передаче стикера. */
        const val StickerMimeType = "image/png"

        private const val StickerCacheDirectory = "stickers"
        private const val ProviderAuthoritySuffix = "stickers"
    }
}
