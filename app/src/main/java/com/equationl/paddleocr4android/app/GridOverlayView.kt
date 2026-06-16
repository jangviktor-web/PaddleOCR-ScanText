package com.equationl.paddleocr4android.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * 构图网格线覆盖层
 *
 * 绘制三等分网格线（Rule of Thirds），帮助用户构图
 */
class GridOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#60FFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }

    var showGrid = false
        set(value) {
            field = value
            visibility = if (value) VISIBLE else GONE
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!showGrid) return

        val w = width.toFloat()
        val h = height.toFloat()

        // 两条竖线（三等分）
        canvas.drawLine(w / 3f, 0f, w / 3f, h, gridPaint)
        canvas.drawLine(w * 2f / 3f, 0f, w * 2f / 3f, h, gridPaint)

        // 两条横线（三等分）
        canvas.drawLine(0f, h / 3f, w, h / 3f, gridPaint)
        canvas.drawLine(0f, h * 2f / 3f, w, h * 2f / 3f, gridPaint)
    }
}
