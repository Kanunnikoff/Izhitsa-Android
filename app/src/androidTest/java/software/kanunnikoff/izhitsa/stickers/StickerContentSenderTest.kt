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

@RunWith(AndroidJUnit4::class)
class StickerContentSenderTest {
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

    private class RecordingInputConnection(
        targetView: View
    ) : BaseInputConnection(targetView, false) {
        var committedUri: Uri? = null
            private set
        var committedFlags: Int = 0
            private set

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
