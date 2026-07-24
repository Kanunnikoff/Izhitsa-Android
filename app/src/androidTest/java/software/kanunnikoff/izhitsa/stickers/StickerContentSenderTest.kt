package software.kanunnikoff.izhitsa.stickers

import android.net.Uri
import android.os.Bundle
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputContentInfo
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Проверяет передачу стикера через настоящее хранилище и подменённое поле ввода. */
@RunWith(AndroidJUnit4::class)
class StickerContentSenderTest {
    /** Получатель видит URI содержимого и получает только временное право чтения. */
    @Test
    fun contentIsCommittedWithTemporaryReadAccess() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val sender = StickerContentSender(
            repository = StickerRepository(context = context)
        )
        val editorInfo = EditorInfo().apply {
            contentMimeTypes = arrayOf("image/*")
        }
        val recordingConnection = RecordingInputConnection(
            targetView = View(context)
        )

        assertTrue(sender.isSupported(editorInfo = editorInfo))
        assertTrue(
            sender.commit(
                inputConnection = recordingConnection,
                sticker = StickerCatalog.items.first()
            )
        )
        assertEquals(
            "content",
            recordingConnection.committedUri?.scheme
        )
        assertEquals(
            InputConnection.INPUT_CONTENT_GRANT_READ_URI_PERMISSION,
            recordingConnection.committedFlags
        )
    }

    /**
     * Соединение, сохраняющее параметры вызова вместо передачи их редактору.
     *
     * @param targetView представление, требуемое базовым соединением Android.
     */
    private class RecordingInputConnection(
        targetView: View
    ) : BaseInputConnection(targetView, false) {
        var committedUri: Uri? = null
            private set
        var committedFlags: Int = 0
            private set

        /**
         * Запоминает URI и флаги, чтобы проверка могла сравнить их с протоколом Android.
         *
         * @param inputContentInfo описание передаваемого содержимого.
         * @param flags права доступа.
         * @param opts дополнительные параметры вызова.
         */
        override fun commitContent(
            inputContentInfo: InputContentInfo,
            flags: Int,
            opts: Bundle?
        ): Boolean {
            committedUri = inputContentInfo.contentUri
            committedFlags = flags

            return true
        }
    }
}
