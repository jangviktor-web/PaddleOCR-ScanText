package com.equationl.paddleocr4android.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 对焦视图：显示对焦框 + 曝光调节条
 *
 * 功能：
 * - 点击显示对焦框（带呼吸动画）
 * - 对焦框旁显示曝光补偿滑块
 * - 支持拖动对焦框位置
 * - 长按时对焦框变为锁定状态（方形）
 */
class FocusView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    // 对焦框画笔
    private val focusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val focusFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.TRANSPARENT
        style = Paint.Style.FILL
    }

    // 锁定状态画笔
    private val lockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD700")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    // 曝光条画笔
    private val exposureBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#40000000")
        style = Paint.Style.FILL
    }
    private val exposureFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val exposureTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    // 对焦框状态
    private var focusX = 0f
    private var focusY = 0f
    private var focusRadius = 80f
    private var isFocused = false
    private var isLocked = false
    private var showExposureBar = false

    // 曝光值
    private var exposureValue = 0
    private val maxExposure = 3
    private val minExposure = -3

    // 曝光条区域
    private val exposureBarRect = RectF()
    private val exposureThumbRadius = 20f

    // 动画
    private var focusAnimator: ValueAnimator? = null
    private var currentFocusScale = 1f

    // 回调
    var onFocusChanged: ((x: Float, y: Float) -> Unit)? = null
    var onExposureChanged: ((补偿值: Int) -> Unit)? = null
    var onFocusLocked: ((x: Float, y: Float, locked: Boolean) -> Unit)? = null
    var onZoomChanged: ((scaleFactor: Float) -> Unit)? = null

    // 缩放手势
    private var scaleGestureDetector: ScaleGestureDetector? = null

    // 拖动状态
    private var isDragging = false
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    // 长按检测
    private var longPressRunnable: Runnable? = null
    private val longPressTimeout = 500L

    init {
        scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                onZoomChanged?.invoke(detector.scaleFactor)
                return true
            }
        })
    }

    fun showFocus(x: Float, y: Float, locked: Boolean = false) {
        focusX = x
        focusY = y
        isFocused = true
        isLocked = locked
        showExposureBar = locked
        currentFocusScale = 1.5f
        invalidate()
        startFocusAnimation()
    }

    fun hideFocus() {
        isFocused = false
        isLocked = false
        showExposureBar = false
        invalidate()
    }

    fun setExposure(value: Int) {
        exposureValue = value.coerceIn(minExposure, maxExposure)
        invalidate()
    }

    private fun startFocusAnimation() {
        focusAnimator?.cancel()
        focusAnimator = ValueAnimator.ofFloat(1.5f, 1f).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                currentFocusScale = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isFocused) return

        val scaledRadius = focusRadius * currentFocusScale

        // 绘制对焦框
        if (isLocked) {
            // 锁定状态：方形
            val half = scaledRadius
            val rect = RectF(focusX - half, focusY - half, focusX + half, focusY + half)
            canvas.drawRect(rect, lockPaint)
        } else {
            // 普通状态：圆形
            canvas.drawCircle(focusX, focusY, scaledRadius, focusFillPaint)
            canvas.drawCircle(focusX, focusY, scaledRadius, focusPaint)
            // 四个角的短线
            drawCornerTicks(canvas, focusX, focusY, scaledRadius)
        }

        // 绘制曝光调节条
        if (showExposureBar) {
            drawExposureBar(canvas)
        }
    }

    private fun drawCornerTicks(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val tickLen = 20f
        val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            strokeWidth = 4f
            strokeCap = Paint.Cap.ROUND
        }

        // 左上
        canvas.drawLine(cx - r, cy - r, cx - r + tickLen, cy - r, tickPaint)
        canvas.drawLine(cx - r, cy - r, cx - r, cy - r + tickLen, tickPaint)
        // 右上
        canvas.drawLine(cx + r, cy - r, cx + r - tickLen, cy - r, tickPaint)
        canvas.drawLine(cx + r, cy - r, cx + r, cy - r + tickLen, tickPaint)
        // 左下
        canvas.drawLine(cx - r, cy + r, cx - r + tickLen, cy + r, tickPaint)
        canvas.drawLine(cx - r, cy + r, cx - r, cy + r - tickLen, tickPaint)
        // 右下
        canvas.drawLine(cx + r, cy + r, cx + r - tickLen, cy + r, tickPaint)
        canvas.drawLine(cx + r, cy + r, cx + r, cy + r - tickLen, tickPaint)
    }

    private fun drawExposureBar(canvas: Canvas) {
        val barWidth = 12f
        val barHeight = 300f
        val barX = focusX + focusRadius + 30f
        val barTop = focusY - barHeight / 2f
        val barBottom = focusY + barHeight / 2f

        exposureBarRect.set(barX, barTop, barX + barWidth, barBottom)

        // 背景
        canvas.drawRoundRect(exposureBarRect, 6f, 6f, exposureBgPaint)

        // 中间线
        val midY = (barTop + barBottom) / 2f
        canvas.drawLine(barX - 4f, midY, barX + barWidth + 4f, midY, exposureTextPaint)

        // 当前值的滑块位置
        val range = (maxExposure - minExposure).toFloat()
        val fraction = (exposureValue - minExposure) / range
        val thumbY = barBottom - fraction * barHeight

        // 滑块
        canvas.drawCircle(barX + barWidth / 2f, thumbY, exposureThumbRadius, exposureFillPaint)

        // 显示当前值
        val sign = if (exposureValue > 0) "+" else ""
        canvas.drawText("${sign}$exposureValue", barX + barWidth / 2f, thumbY - exposureThumbRadius - 8f, exposureTextPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 双指缩放手势
        scaleGestureDetector?.onTouchEvent(event)

        // 单指操作（对焦/曝光/拖动）
        if (event.pointerCount == 1) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = event.x
                    lastTouchY = event.y

                    // 检查是否触摸到曝光条
                    if (showExposureBar && exposureBarRect.contains(event.x, event.y)) {
                        isDragging = true
                        updateExposureFromTouch(event.y)
                        return true
                    }

                    // 检查是否在对焦框附近
                    val dist = sqrt((event.x - focusX).toDouble().pow(2.0) + (event.y - focusY).toDouble().pow(2.0))
                    if (isFocused && dist < focusRadius + 40) {
                        isDragging = true
                        return true
                    }

                    // 启动长按检测
                    longPressRunnable = Runnable {
                        isLocked = !isLocked
                        showExposureBar = isLocked
                        invalidate()
                        onFocusLocked?.invoke(focusX, focusY, isLocked)
                    }
                    postDelayed(longPressRunnable, longPressTimeout)
                    return true
                }

                MotionEvent.ACTION_MOVE -> {
                    val isScaling = scaleGestureDetector?.isInProgress == true
                    if (!isScaling) {
                        removeCallbacks(longPressRunnable)
                        if (isDragging) {
                            focusX = event.x
                            focusY = event.y
                            invalidate()
                            return true
                        }
                        val dist = sqrt((event.x - lastTouchX).toDouble().pow(2.0) + (event.y - lastTouchY).toDouble().pow(2.0))
                        if (dist > 20) {
                            removeCallbacks(longPressRunnable)
                        }
                    }
                    return true
                }

                MotionEvent.ACTION_UP -> {
                    removeCallbacks(longPressRunnable)
                    if (isDragging) {
                        isDragging = false
                        if (showExposureBar) {
                            updateExposureFromTouch(event.y)
                        } else {
                            focusX = event.x
                            focusY = event.y
                            onFocusChanged?.invoke(focusX, focusY)
                            showFocus(focusX, focusY)
                        }
                        return true
                    }
                    // 短按：对焦
                    focusX = event.x
                    focusY = event.y
                    onFocusChanged?.invoke(focusX, focusY)
                    showFocus(focusX, focusY)
                    return true
                }

                MotionEvent.ACTION_CANCEL -> {
                    removeCallbacks(longPressRunnable)
                    isDragging = false
                }
            }
        }
        return true
    }

    private fun updateExposureFromTouch(touchY: Float) {
        val barTop = exposureBarRect.top
        val barBottom = exposureBarRect.bottom
        val barHeight = barBottom - barTop
        val fraction = 1f - ((touchY - barTop) / barHeight).coerceIn(0f, 1f)
        val range = (maxExposure - minExposure).toFloat()
        exposureValue = (fraction * range + minExposure).toInt().coerceIn(minExposure, maxExposure)
        onExposureChanged?.invoke(exposureValue)
        invalidate()
    }
}
