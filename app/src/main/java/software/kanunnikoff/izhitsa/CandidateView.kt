package software.kanunnikoff.izhitsa

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.max

class CandidateView(context: Context) : View(context) {
    private var service: SoftKeyboard? = null
    private var suggestions: List<String> = emptyList()
    private var selectedIndex = -1
    private var touchX = OUT_OF_BOUNDS
    private val selectionHighlight: Drawable?
    private var typedWordValid = false
    private var bgPadding: Rect? = null

    private val wordWidth = IntArray(MAX_SUGGESTIONS)
    private val wordX = IntArray(MAX_SUGGESTIONS)

    private val colorNormal: Int
    private val colorRecommended: Int
    private val colorOther: Int
    private val verticalPadding: Int
    private val paint: Paint
    private var scrolled = false
    private var targetScrollX = 0
    private var totalWidth = 0

    private val gestureDetector: GestureDetector

    init {
        selectionHighlight = ContextCompat.getDrawable(context, android.R.drawable.list_selector_background)
        selectionHighlight?.state = intArrayOf(
            android.R.attr.state_enabled,
            android.R.attr.state_focused,
            android.R.attr.state_window_focused,
            android.R.attr.state_pressed
        )

        val r: Resources = context.resources

        setBackgroundColor(ContextCompat.getColor(context, R.color.candidate_background))

        colorNormal = ContextCompat.getColor(context, R.color.candidate_normal)
        colorRecommended = ContextCompat.getColor(context, R.color.candidate_recommended)
        colorOther = ContextCompat.getColor(context, R.color.candidate_other)
        verticalPadding = r.getDimensionPixelSize(R.dimen.candidate_vertical_padding)

        paint = Paint().apply {
            color = colorNormal
            isAntiAlias = true
            textSize = r.getDimensionPixelSize(R.dimen.candidate_font_height).toFloat()
            strokeWidth = 0f
        }

        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                scrolled = true
                var sx = scrollX + distanceX
                if (sx < 0) {
                    sx = 0f
                }
                if (sx + width > totalWidth) {
                    sx -= distanceX
                }
                targetScrollX = sx.toInt()
                scrollTo(sx.toInt(), scrollY)
                invalidate()
                return true
            }
        })
        isHorizontalFadingEdgeEnabled = true
        setWillNotDraw(false)
        isHorizontalScrollBarEnabled = false
        isVerticalScrollBarEnabled = false
    }

    /**
     * A connection back to the service to communicate with the text field
     * @param listener
     */
    fun setService(listener: SoftKeyboard) {
        service = listener
    }

    override fun computeHorizontalScrollRange(): Int {
        return totalWidth
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val measuredWidth = resolveSize(50, widthMeasureSpec)
        val padding = Rect()
        selectionHighlight?.getPadding(padding)
        val desiredHeight = paint.textSize.toInt() + verticalPadding + padding.top + padding.bottom
        setMeasuredDimension(measuredWidth, resolveSize(desiredHeight, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawCandidates(canvas)
    }

    private fun drawCandidates(canvas: Canvas?) {
        totalWidth = 0
        if (suggestions.isEmpty()) return

        if (bgPadding == null) {
            bgPadding = Rect(0, 0, 0, 0)
            background?.getPadding(bgPadding!!)
        }
        var x = 0
        val count = suggestions.size
        val height = height
        val bgPadding = bgPadding ?: Rect()
        val paint = paint
        val touchX = touchX
        val scrollX = scrollX
        val scrolled = scrolled
        val typedWordValid = typedWordValid
        val y = ((height - paint.textSize) / 2f - paint.ascent()).toInt()

        for (i in 0 until count) {
            val suggestion = suggestions[i]
            val textWidth = paint.measureText(suggestion)
            val wordWidth = textWidth.toInt() + X_GAP * 2

            this.wordX[i] = x
            this.wordWidth[i] = wordWidth
            paint.color = colorNormal
            if (touchX + scrollX >= x && touchX + scrollX < x + wordWidth && !scrolled) {
                if (canvas != null) {
                    canvas.translate(x.toFloat(), 0f)
                    selectionHighlight?.setBounds(0, bgPadding.top, wordWidth, height)
                    selectionHighlight?.draw(canvas)
                    canvas.translate(-x.toFloat(), 0f)
                }
                selectedIndex = i
            }

            if (canvas != null) {
                if ((i == 1 && !typedWordValid) || (i == 0 && typedWordValid)) {
                    paint.isFakeBoldText = true
                    paint.color = colorRecommended
                } else if (i != 0) {
                    paint.color = colorOther
                }
                canvas.drawText(suggestion, (x + X_GAP).toFloat(), y.toFloat(), paint)
                paint.color = colorOther
                canvas.drawLine(
                    x + wordWidth + 0.5f,
                    bgPadding.top.toFloat(),
                    x + wordWidth + 0.5f,
                    height + 1f,
                    paint
                )
                paint.isFakeBoldText = false
            }
            x += wordWidth
        }
        totalWidth = x
        if (targetScrollX != scrollX) {
            scrollToTarget()
        }
    }

    private fun scrollToTarget() {
        var sx = scrollX
        if (targetScrollX > sx) {
            sx += SCROLL_PIXELS
            if (sx >= targetScrollX) {
                sx = targetScrollX
                requestLayout()
            }
        } else {
            sx -= SCROLL_PIXELS
            if (sx <= targetScrollX) {
                sx = targetScrollX
                requestLayout()
            }
        }
        scrollTo(sx, scrollY)
        invalidate()
    }

    fun setSuggestions(suggestions: List<String>?, completions: Boolean, typedWordValid: Boolean) {
        clear()
        if (suggestions != null) {
            this.suggestions = ArrayList(suggestions)
        }
        this.typedWordValid = typedWordValid
        scrollTo(0, 0)
        targetScrollX = 0
        drawCandidates(null)
        invalidate()
        requestLayout()
    }

    fun clear() {
        suggestions = emptyList()
        touchX = OUT_OF_BOUNDS
        selectedIndex = -1
        invalidate()
    }

    override fun onTouchEvent(me: MotionEvent): Boolean {
        if (gestureDetector.onTouchEvent(me)) {
            return true
        }

        val action = me.action
        val x = me.x.toInt()
        val y = me.y.toInt()
        touchX = x

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                scrolled = false
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                if (y <= 0) {
                    if (selectedIndex >= 0) {
                        service?.pickSuggestionManually(selectedIndex)
                        selectedIndex = -1
                    }
                }
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                if (!scrolled) {
                    if (selectedIndex >= 0) {
                        service?.pickSuggestionManually(selectedIndex)
                    }
                }
                selectedIndex = -1
                removeHighlight()
                requestLayout()
            }
        }
        return true
    }

    /**
     * For flick through from keyboard, call this method with the x coordinate of the flick
     * gesture.
     * @param x
     */
    fun takeSuggestionAt(x: Float) {
        touchX = x.toInt()
        drawCandidates(null)
        if (selectedIndex >= 0) {
            service?.pickSuggestionManually(selectedIndex)
        }
        invalidate()
    }

    private fun removeHighlight() {
        touchX = OUT_OF_BOUNDS
        invalidate()
    }

    companion object {
        private const val OUT_OF_BOUNDS = -1
        private const val MAX_SUGGESTIONS = 32
        private const val SCROLL_PIXELS = 20
        private const val X_GAP = 10
    }
}
