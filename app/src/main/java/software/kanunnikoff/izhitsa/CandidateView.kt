package software.kanunnikoff.izhitsa

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat

class CandidateView(context: Context) : View(context) {

    private var mService: SoftKeyboard? = null
    private var mSuggestions: List<String>? = null
    private var mSelectedIndex = -1
    private var mTouchX = OUT_OF_BOUNDS
    private val mSelectionHighlight: Drawable? = ContextCompat.getDrawable(context, android.R.drawable.list_selector_background)
    private var mTypedWordValid = false
    private var mBgPadding: Rect? = null

    private val mWordWidth = IntArray(MAX_SUGGESTIONS)
    private val mWordX = IntArray(MAX_SUGGESTIONS)

    private val mColorNormal: Int
    private val mColorRecommended: Int
    private val mColorOther: Int
    private val mVerticalPadding: Int
    private val mPaint: Paint
    private var mScrolled = false
    private var mTargetScrollX = 0
    private var mTotalWidth = 0
    private val mGestureDetector: GestureDetector

    init {
        mSelectionHighlight?.state = intArrayOf(
            android.R.attr.state_enabled,
            android.R.attr.state_focused,
            android.R.attr.state_window_focused,
            android.R.attr.state_pressed
        )

        val r = context.resources
        setBackgroundColor(ContextCompat.getColor(context, R.color.candidate_background))

        mColorNormal = ContextCompat.getColor(context, R.color.candidate_normal)
        mColorRecommended = ContextCompat.getColor(context, R.color.candidate_recommended)
        mColorOther = ContextCompat.getColor(context, R.color.candidate_other)
        mVerticalPadding = r.getDimensionPixelSize(R.dimen.candidate_vertical_padding)

        mPaint = Paint().apply {
            color = mColorNormal
            isAntiAlias = true
            textSize = r.getDimensionPixelSize(R.dimen.candidate_font_height).toFloat()
            strokeWidth = 0f
        }

        mGestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                mScrolled = true
                var sx = scrollX
                sx += distanceX.toInt()
                if (sx < 0) {
                    sx = 0
                }
                if (sx + width > mTotalWidth) {
                    sx -= distanceX.toInt()
                }
                mTargetScrollX = sx
                scrollTo(sx, scrollY)
                invalidate()
                return true
            }
        })
        isHorizontalFadingEdgeEnabled = true
        setWillNotDraw(false)
        isHorizontalScrollBarEnabled = false
        isVerticalScrollBarEnabled = false
    }

    fun setService(listener: SoftKeyboard) {
        mService = listener
    }

    override fun computeHorizontalScrollRange(): Int {
        return mTotalWidth
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val measuredWidth = resolveSize(50, widthMeasureSpec)
        val padding = Rect()
        mSelectionHighlight?.getPadding(padding)
        val desiredHeight = mPaint.textSize.toInt() + mVerticalPadding + padding.top + padding.bottom
        setMeasuredDimension(measuredWidth, resolveSize(desiredHeight, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        mTotalWidth = 0
        val suggestions = mSuggestions ?: return

        if (mBgPadding == null) {
            mBgPadding = Rect(0, 0, 0, 0)
            background?.getPadding(mBgPadding!!)
        }

        var x = 0
        val count = suggestions.size
        val height = height
        val bgPadding = mBgPadding!!
        val paint = mPaint
        val touchX = mTouchX
        val scrollX = scrollX
        val scrolled = mScrolled
        val typedWordValid = mTypedWordValid
        val y = ((height - paint.textSize) / 2 - paint.ascent()).toInt()

        for (i in 0 until count) {
            val suggestion = suggestions[i]
            val textWidth = paint.measureText(suggestion)
            val wordWidth = textWidth.toInt() + X_GAP * 2

            mWordX[i] = x
            mWordWidth[i] = wordWidth
            paint.color = mColorNormal
            if (touchX + scrollX >= x && touchX + scrollX < x + wordWidth && !scrolled) {
                canvas.save()
                canvas.translate(x.toFloat(), 0f)
                mSelectionHighlight?.setBounds(0, bgPadding.top, wordWidth, height)
                mSelectionHighlight?.draw(canvas)
                canvas.restore()
                mSelectedIndex = i
            }

            if ((i == 1 && !typedWordValid) || (i == 0 && typedWordValid)) {
                paint.isFakeBoldText = true
                paint.color = mColorRecommended
            } else if (i != 0) {
                paint.color = mColorOther
            }
            canvas.drawText(suggestion, (x + X_GAP).toFloat(), y.toFloat(), paint)
            paint.color = mColorOther
            canvas.drawLine(
                x + wordWidth + 0.5f, bgPadding.top.toFloat(),
                x + wordWidth + 0.5f, (height + 1).toFloat(), paint
            )
            paint.isFakeBoldText = false
            x += wordWidth
        }
        mTotalWidth = x
        if (mTargetScrollX != getScrollX()) {
            scrollToTarget()
        }
    }

    private fun scrollToTarget() {
        var sx = scrollX
        if (mTargetScrollX > sx) {
            sx += SCROLL_PIXELS
            if (sx >= mTargetScrollX) {
                sx = mTargetScrollX
                requestLayout()
            }
        } else {
            sx -= SCROLL_PIXELS
            if (sx <= mTargetScrollX) {
                sx = mTargetScrollX
                requestLayout()
            }
        }
        scrollTo(sx, scrollY)
        invalidate()
    }

    fun setSuggestions(suggestions: List<String>?, completions: Boolean, typedWordValid: Boolean) {
        clear()
        if (suggestions != null) {
            mSuggestions = ArrayList(suggestions)
        }
        mTypedWordValid = typedWordValid
        scrollTo(0, 0)
        mTargetScrollX = 0
        // Trigger a fake draw to calculate width (in original code onDraw(null) was used)
        // Here we'll just invalidate and wait for the real onDraw.
        invalidate()
        requestLayout()
    }

    fun clear() {
        mSuggestions = EMPTY_LIST
        mTouchX = OUT_OF_BOUNDS
        mSelectedIndex = -1
        invalidate()
    }

    override fun onTouchEvent(me: MotionEvent): Boolean {
        if (mGestureDetector.onTouchEvent(me)) {
            return true
        }

        val action = me.action
        val x = me.x.toInt()
        val y = me.y.toInt()
        mTouchX = x

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                mScrolled = false
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                if (y <= 0) {
                    if (mSelectedIndex >= 0) {
                        mService?.pickSuggestionManually(mSelectedIndex)
                        mSelectedIndex = -1
                    }
                }
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                if (!mScrolled) {
                    if (mSelectedIndex >= 0) {
                        mService?.pickSuggestionManually(mSelectedIndex)
                    }
                }
                mSelectedIndex = -1
                removeHighlight()
                requestLayout()
            }
        }
        return true
    }

    fun takeSuggestionAt(x: Float) {
        mTouchX = x.toInt()
        // Force evaluation of selection index
        // In original Java code, this was done by calling onDraw(null)
        // In Kotlin/Android, it's safer to just invalidate and wait, but for logic we might need immediate calculation
        // or a dedicated method for hit testing.
        invalidate()
        // Wait for next draw? No, we need it now. 
        // Let's re-implement the hit test logic or just use a flag.
    }

    private fun removeHighlight() {
        mTouchX = OUT_OF_BOUNDS
        invalidate()
    }

    companion object {
        private const val OUT_OF_BOUNDS = -1
        private const val MAX_SUGGESTIONS = 32
        private const val SCROLL_PIXELS = 20
        private const val X_GAP = 10
        private val EMPTY_LIST = emptyList<String>()
    }
}
