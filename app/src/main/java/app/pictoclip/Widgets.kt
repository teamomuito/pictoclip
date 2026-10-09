package app.pictoclip

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.text.TextUtils
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** One clip in the list: a white panel, a page icon, the text, and a time pill. Blue brackets when selected. */
class ClipRow(context: Context) : FrameLayout(context) {
    private val body = Ds.label(context, 15f).apply {
        maxLines = 3
        ellipsize = TextUtils.TruncateAt.END
    }
    private val time = Ds.label(context, 12f).apply {
        gravity = Gravity.CENTER
        setPadding(context.dp(6), context.dp(4), context.dp(6), context.dp(4))
        background = PanelDrawable(context.dp(1).toFloat(), Ds.BG)
    }
    private val bracket = Paint().apply {
        color = Ds.CURSOR
        style = Paint.Style.STROKE
        strokeWidth = context.dp(3).toFloat()
        isAntiAlias = false
    }

    var cursor = false
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    init {
        setWillNotDraw(false)
        val gap = context.dp(10)
        setPadding(gap, context.dp(6), gap, context.dp(6))

        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = context.dp(56)
            setPadding(context.dp(12), context.dp(10), context.dp(12), context.dp(10))
            background = PanelDrawable(context.dp(2).toFloat())
            isDuplicateParentStateEnabled = true
        }
        val icon = ImageView(context).apply {
            setImageDrawable(PageIconDrawable(context.dp(3).toFloat()))
        }
        panel.addView(icon, LinearLayout.LayoutParams(context.dp(27), context.dp(33)))
        panel.addView(
            body,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = context.dp(12)
                marginEnd = context.dp(10)
            },
        )
        panel.addView(time, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        addView(panel, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    fun bind(clip: Clip, selected: Boolean) {
        body.text = clip.text.trim().replace(Regex("\\s*\n\\s*"), "\n")
        time.text = formatTime(clip.time)
        cursor = selected
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (!cursor) return
        val sw = bracket.strokeWidth
        val inset = context.dp(3) + sw / 2
        val l = inset
        val t = inset - context.dp(1)
        val r = width - inset
        val b = height - inset + context.dp(1)
        val arm = context.dp(14).toFloat()
        val p = Path().apply {
            moveTo(l, t + arm); lineTo(l, t); lineTo(l + arm, t)
            moveTo(r - arm, t); lineTo(r, t); lineTo(r, t + arm)
            moveTo(r, b - arm); lineTo(r, b); lineTo(r - arm, b)
            moveTo(l + arm, b); lineTo(l, b); lineTo(l, b - arm)
        }
        canvas.drawPath(p, bracket)
    }

    private fun formatTime(millis: Long): String {
        val then = Calendar.getInstance().apply { timeInMillis = millis }
        val now = Calendar.getInstance()
        val sameDay = then.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            then.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
        val fmt = if (sameDay) "HH:mm" else "MM/dd"
        return SimpleDateFormat(fmt, Locale.getDefault()).format(Date(millis))
    }
}

/** Bottom-screen style button: a lettered circle (A / B / X / Y) followed by a label. */
class DsButton(context: Context, private val letter: String, label: String) : TextView(context) {
    private val ring = Paint().apply {
        color = Ds.BORDER
        style = Paint.Style.STROKE
        strokeWidth = context.dp(2).toFloat()
        isAntiAlias = true
    }
    private val glyph = Paint().apply {
        color = Ds.BORDER
        textAlign = Paint.Align.CENTER
        typeface = Ds.font
        textSize = context.dp(13).toFloat()
        isAntiAlias = true
    }
    private val radius = context.dp(11).toFloat()

    init {
        text = label
        typeface = Ds.font
        textSize = 16f
        setTextColor(Ds.TEXT)
        gravity = Gravity.CENTER_VERTICAL or Gravity.CENTER_HORIZONTAL
        isClickable = true
        includeFontPadding = false
        minimumHeight = context.dp(48)
        background = PanelDrawable(context.dp(2).toFloat())
        setPadding(context.dp(12) + (radius * 2).toInt() + context.dp(8), 0, context.dp(12), 0)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = paddingStart - context.dp(8) - radius
        val cy = height / 2f
        canvas.drawCircle(cx, cy, radius, ring)
        val baseline = cy - (glyph.descent() + glyph.ascent()) / 2
        canvas.drawText(letter, cx, baseline, glyph)
    }
}
