package software.kanunnikoff.izhitsa.stickers

import android.content.ClipDescription
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputContentInfo

/**
 * Передаёт стикеры в поле ввода через протокол содержимого метода ввода.
 *
 * @property repository хранилище, предоставляющее доступный получателю URI файла.
 */
class StickerContentSender(
    private val repository: StickerRepository
) {
    /**
     * Проверяет, объявило ли текущее поле ввода поддержку изображений PNG.
     *
     * @param editorInfo сведения о поле, полученные службой метода ввода.
     */
    fun isSupported(editorInfo: EditorInfo): Boolean {
        return editorInfo.contentMimeTypes.orEmpty().any { mimeType ->
            ClipDescription.compareMimeTypes(
                StickerRepository.StickerMimeType,
                mimeType
            )
        }
    }

    /**
     * Вставляет [sticker] в приложение, владеющее [inputConnection].
     *
     * @param inputConnection соединение с принимающим полем.
     * @param sticker выбранный стикер.
     * @return `true`, если получатель принял содержимое.
     */
    fun commit(
        inputConnection: InputConnection,
        sticker: Sticker
    ): Boolean {
        val contentUri = repository.contentUri(sticker = sticker)
        val description = ClipDescription(
            sticker.description,
            arrayOf(StickerRepository.StickerMimeType)
        )
        val content = InputContentInfo(
            contentUri,
            description,
            null
        )

        /*
         * Получателю выдаётся только временное право чтения одного URI. Метод
         * ввода не предоставляет приложению доступ к другим файлам или записи.
         */
        return inputConnection.commitContent(
            content,
            InputConnection.INPUT_CONTENT_GRANT_READ_URI_PERMISSION,
            null
        )
    }
}
