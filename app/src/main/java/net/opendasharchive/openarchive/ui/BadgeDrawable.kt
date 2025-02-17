package net.opendasharchive.openarchive.ui

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.util.Constants
import kotlin.math.max

class BadgeDrawable : Drawable {

    private var mBadgePaint: Paint? = null
    private var mBadgePaint1: Paint? = null
    private var mTextPaint: Paint? = null
    private val mTxtRect = Rect()

    private var mCount = Constants.EMPTY_STRING

    constructor(context: Context) : super() {

        val mTextSize = context.resources.getDimension(R.dimen.badge_text_size)

        val color = ContextCompat.getColor(context.applicationContext, R.color.oablue)

        mBadgePaint = Paint()
        mBadgePaint?.apply {
            this.color = color
            this.isAntiAlias = true
            this.style = Paint.Style.STROKE
            this.strokeWidth = 5f
        }


        mTextPaint = Paint()
        mTextPaint?.apply {
            this.color = color
            this.typeface = Typeface.DEFAULT
            this.textSize = mTextSize
            this.isAntiAlias = true
            this.textAlign = Paint.Align.CENTER
        }
    }


    override fun draw(canvas: Canvas) {
        // Position the badge in the top-right quadrant of the icon.
        mTextPaint?.getTextBounds(mCount, 0, mCount.length, mTxtRect)
        val radius = (max(mTxtRect.width(), mTxtRect.height()) / 2).toFloat()
        canvas.drawText(mCount, 0f, radius / 2 + radius / 4, mTextPaint ?: Paint())
        canvas.drawCircle(0f, 0f, (radius * 2), mBadgePaint ?: Paint())
    }

    override fun setAlpha(alpha: Int) {
        //NO-OP
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        //NO-OP
    }

    override fun getOpacity(): Int {
        return PixelFormat.UNKNOWN
    }

    /*
    Sets the count (i.e notifications) to display.
     */
    fun setCount(count: String) {
        mCount = count
        invalidateSelf()
    }


}