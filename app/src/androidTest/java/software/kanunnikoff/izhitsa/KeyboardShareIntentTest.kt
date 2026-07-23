package software.kanunnikoff.izhitsa

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeyboardShareIntentTest {

    @Suppress("DEPRECATION")
    @Test
    fun shareIntentContainsApplicationPageLink() {
        val chooserIntent = createShareApplicationIntent(
            packageName = ApplicationPackageName
        )
        val shareIntent = chooserIntent.getParcelableExtra<Intent>(
            Intent.EXTRA_INTENT
        )

        assertEquals(Intent.ACTION_CHOOSER, chooserIntent.action)
        assertTrue(
            chooserIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0
        )
        assertNotNull(shareIntent)
        assertEquals(Intent.ACTION_SEND, shareIntent?.action)
        assertEquals(PlainTextMimeType, shareIntent?.type)
        assertEquals(
            ApplicationGooglePlayUrl,
            shareIntent?.getStringExtra(Intent.EXTRA_TEXT)
        )
    }

    private companion object {
        const val ApplicationPackageName = "software.kanunnikoff.izhitsa"
        const val PlainTextMimeType = "text/plain"
        const val ApplicationGooglePlayUrl =
            "https://play.google.com/store/apps/details?id=$ApplicationPackageName"
    }
}
