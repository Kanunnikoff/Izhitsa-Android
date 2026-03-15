package software.kanunnikoff.izhitsa

import android.content.Context
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.drawable.Drawable
import android.inputmethodservice.Keyboard
import android.view.inputmethod.EditorInfo
import androidx.core.content.res.ResourcesCompat

@Suppress("DEPRECATION")
class LatinKeyboard : Keyboard {

    private var mEnterKey: Key? = null
    private var mSpaceKey: Key? = null
    private var mModeChangeKey: Key? = null
    private var mLanguageSwitchKey: Key? = null
    private var mSavedModeChangeKey: Key? = null
    private var mSavedLanguageSwitchKey: Key? = null
    private val mContext: Context

    constructor(context: Context, xmlLayoutResId: Int) : super(context, xmlLayoutResId) {
        mContext = context
    }

    constructor(
        context: Context, layoutTemplateResId: Int,
        characters: CharSequence?, columns: Int, horizontalPadding: Int
    ) : super(context, layoutTemplateResId, characters, columns, horizontalPadding) {
        mContext = context
    }

    override fun createKeyFromXml(
        res: Resources,
        parent: Row,
        x: Int,
        y: Int,
        parser: XmlResourceParser
    ): Key {
        val key = LatinKey(res, parent, x, y, parser)

        when (key.codes[0]) {
            10 -> mEnterKey = key
            ' '.toInt() -> mSpaceKey = key
            KEYCODE_MODE_CHANGE -> {
                mModeChangeKey = key
                mSavedModeChangeKey = LatinKey(res, parent, x, y, parser)
            }
            LatinKeyboardView.KEYCODE_LANGUAGE_SWITCH -> {
                mLanguageSwitchKey = key
                mSavedLanguageSwitchKey = LatinKey(res, parent, x, y, parser)
            }
        }

        return key
    }

    fun setLanguageSwitchKeyVisibility(visible: Boolean) {
        if (visible) {
            mModeChangeKey?.let {
                it.width = mSavedModeChangeKey?.width ?: it.width
                it.x = mSavedModeChangeKey?.x ?: it.x
            }

            mLanguageSwitchKey?.let { key ->
                mSavedLanguageSwitchKey?.let { saved ->
                    key.width = saved.width
                    key.icon = saved.icon
                    key.iconPreview = saved.iconPreview
                }
            }
        } else {
            mModeChangeKey?.let { modeKey ->
                mSavedModeChangeKey?.let { savedMode ->
                    mSavedLanguageSwitchKey?.let { savedLang ->
                        modeKey.width = savedMode.width + savedLang.width
                    }
                }
            }
            mLanguageSwitchKey?.let {
                it.width = 0
                it.icon = null
                it.iconPreview = null
            }
        }
    }

    fun setImeOptions(res: Resources, options: Int) {
        val enterKey = mEnterKey ?: return

        when (options and (EditorInfo.IME_MASK_ACTION or EditorInfo.IME_FLAG_NO_ENTER_ACTION)) {
            EditorInfo.IME_ACTION_GO -> {
                enterKey.iconPreview = null
                enterKey.icon = null
                enterKey.label = res.getText(R.string.label_go_key)
            }
            EditorInfo.IME_ACTION_NEXT -> {
                enterKey.iconPreview = null
                enterKey.icon = null
                enterKey.label = res.getText(R.string.label_next_key)
            }
            EditorInfo.IME_ACTION_SEARCH -> {
                enterKey.icon = ResourcesCompat.getDrawable(res, R.drawable.sym_keyboard_search, null)
                enterKey.label = null
            }
            EditorInfo.IME_ACTION_SEND -> {
                enterKey.iconPreview = null
                enterKey.icon = null
                enterKey.label = res.getText(R.string.label_send_key)
            }
            else -> {
                enterKey.icon = ResourcesCompat.getDrawable(res, R.drawable.sym_keyboard_return, null)
                enterKey.label = null
            }
        }
    }

    fun setSpaceIcon(icon: Drawable?) {
        mSpaceKey?.icon = icon
    }

    internal class LatinKey(res: Resources, parent: Row, x: Int, y: Int, parser: XmlResourceParser) :
        Key(res, parent, x, y, parser) {

        override fun isInside(x: Int, y: Int): Boolean {
            return super.isInside(x, if (codes[0] == KEYCODE_CANCEL) y - 10 else y)
        }
    }
}
